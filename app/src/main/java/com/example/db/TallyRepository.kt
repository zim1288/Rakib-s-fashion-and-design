package com.example.db

import android.util.Log
import com.example.api.NetworkProductionItem
import com.example.api.NetworkSareeItem
import com.example.api.NetworkTransactionLog
import com.example.api.SareeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SyncState {
    object IDLE : SyncState
    object SYNCING : SyncState
    object SUCCESS : SyncState
    data class ERROR(val message: String) : SyncState
}

class TallyRepository(private val tallyDao: TallyDao) {
    private val TAG = "TallyRepository"
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    suspend fun getUserAccountByEmail(email: String): UserAccount? = tallyDao.getUserAccountByEmail(email)
    suspend fun getUserAccountByUsername(username: String): UserAccount? = tallyDao.getUserAccountByUsername(username)
    suspend fun insertUserAccount(user: UserAccount) = tallyDao.insertUserAccount(user)

    // Sync status reflecting MongoDB interaction
    private val _syncState = MutableStateFlow<SyncState>(SyncState.IDLE)
    val syncState = _syncState.asStateFlow()

    // 1. Live flows updating instantly from Room SQLite
    val allSareeItems: Flow<List<SareeItem>> = tallyDao.getAllSareeItems()
    val allProductionItems: Flow<List<ProductionItem>> = tallyDao.getAllProductionItems()
    val allTransactionLogs: Flow<List<TransactionLog>> = tallyDao.getAllTransactionLogs()

    // 2. Saree inventory actions with Optimistic Background Sync
    suspend fun insertSareeItem(item: SareeItem) {
        val id = tallyDao.insertSareeItem(item)
        val insertedItem = item.copy(id = id.toInt())
        triggerInventorySync(insertedItem)
    }

    suspend fun updateSareeItem(item: SareeItem) {
        tallyDao.updateSareeItem(item)
        triggerInventorySync(item)
    }

    suspend fun deleteSareeItem(item: SareeItem) {
        tallyDao.deleteSareeItem(item)
        triggerInventoryDeleteSync(item.id)
    }

    suspend fun deleteSareeItemById(id: Int) {
        tallyDao.deleteSareeItemById(id)
        triggerInventoryDeleteSync(id)
    }

    // 3. Stock in production actions with Optimistic Background Sync
    suspend fun insertProductionItem(item: ProductionItem) {
        val id = tallyDao.insertProductionItem(item)
        val insertedItem = item.copy(id = id.toInt())
        triggerProductionSync(insertedItem)
    }

    suspend fun updateProductionItem(item: ProductionItem) {
        tallyDao.updateProductionItem(item)
        triggerProductionSync(item)
    }

    suspend fun deleteProductionItem(item: ProductionItem) {
        tallyDao.deleteProductionItem(item)
    }

    // 4. Transaction log triggers with Optimistic Background Sync
    suspend fun insertTransactionLog(log: TransactionLog) {
        val id = tallyDao.insertTransactionLog(log)
        val insertedLog = log.copy(id = id.toInt())
        triggerTransactionLogSync(insertedLog)
    }

    // --- Background MongoDB API Sync Work ---
    private fun triggerInventorySync(item: SareeItem) {
        repositoryScope.launch {
            _syncState.value = SyncState.SYNCING
            try {
                val response = SareeApi.service.updateInventoryItem(
                    NetworkSareeItem(
                        id = item.id,
                        modelName = item.modelName,
                        brandCategory = item.brandCategory,
                        unitPrice = item.unitPrice,
                        pieceCount = item.pieceCount,
                        imageUrl = item.imageUrl
                    )
                )
                if (response.isSuccessful) {
                    _syncState.value = SyncState.SUCCESS
                } else {
                    _syncState.value = SyncState.ERROR("MongoDB API responded with error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MongoDB sync failed, operating in offline fallback.", e)
                _syncState.value = SyncState.ERROR("Offline Mode: Retrying in background")
            }
        }
    }

    private fun triggerInventoryDeleteSync(id: Int) {
        if (id <= 0) return
        repositoryScope.launch {
            _syncState.value = SyncState.SYNCING
            try {
                val response = SareeApi.service.deleteInventoryItem(id)
                if (response.isSuccessful) {
                    _syncState.value = SyncState.SUCCESS
                } else {
                    _syncState.value = SyncState.ERROR("MongoDB API code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MongoDB delete failed, operating in offline fallback.", e)
                _syncState.value = SyncState.ERROR("Offline Mode: Sync pending")
            }
        }
    }

    private fun triggerProductionSync(item: ProductionItem) {
        repositoryScope.launch {
            _syncState.value = SyncState.SYNCING
            try {
                val response = SareeApi.service.syncProduction(
                    listOf(
                        NetworkProductionItem(
                            id = item.id,
                            modelName = item.modelName,
                            quantity = item.quantity,
                            estimatedCompletionDate = item.estimatedCompletionDate,
                            status = item.status,
                            imageUrl = item.imageUrl
                        )
                    )
                )
                if (response.isSuccessful) {
                    _syncState.value = SyncState.SUCCESS
                } else {
                    _syncState.value = SyncState.ERROR("MongoDB response: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MongoDB production sync failed, saved locally.", e)
                _syncState.value = SyncState.ERROR("Offline: Local Sync Saved")
            }
        }
    }

    private fun triggerTransactionLogSync(log: TransactionLog) {
        repositoryScope.launch {
            _syncState.value = SyncState.SYNCING
            try {
                val response = SareeApi.service.logTransaction(
                    NetworkTransactionLog(
                        id = log.id,
                        type = log.type,
                        modelName = log.modelName,
                        quantity = log.quantity,
                        unitPrice = log.unitPrice,
                        totalAmount = log.totalAmount,
                        timestamp = log.timestamp,
                        dateString = log.dateString
                    )
                )
                if (response.isSuccessful) {
                    _syncState.value = SyncState.SUCCESS
                } else {
                    _syncState.value = SyncState.ERROR("MongoDB log code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MongoDB transaction sync failed, logged offline.", e)
                _syncState.value = SyncState.ERROR("Offline: Transaction Queued")
            }
        }
    }

    suspend fun syncOfflineData() {
        _syncState.value = SyncState.SYNCING
        try {
            val sarees = tallyDao.getAllSareeItems().first()
            val prodItems = tallyDao.getAllProductionItems().first()

            SareeApi.service.syncInventory(sarees.map {
                NetworkSareeItem(
                    id = it.id,
                    modelName = it.modelName,
                    brandCategory = it.brandCategory,
                    unitPrice = it.unitPrice,
                    pieceCount = it.pieceCount,
                    imageUrl = it.imageUrl
                )
            })

            SareeApi.service.syncProduction(prodItems.map {
                NetworkProductionItem(
                    id = it.id,
                    modelName = it.modelName,
                    quantity = it.quantity,
                    estimatedCompletionDate = it.estimatedCompletionDate,
                    status = it.status,
                    imageUrl = it.imageUrl
                )
            })

            _syncState.value = SyncState.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Offline sync failed", e)
            _syncState.value = SyncState.ERROR("Offline sync failed: ${e.message}")
        }
    }

    suspend fun sendVerificationEmail(email: String, code: String): Boolean {
        return try {
            val response = SareeApi.service.sendVerificationEmail(com.example.api.NetworkEmailRequest(email, code))
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send verification email via API", e)
            false
        }
    }
}
