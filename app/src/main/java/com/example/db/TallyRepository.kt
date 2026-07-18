package com.example.db

import android.util.Log
import com.example.api.NetworkProductionItem
import com.example.api.NetworkSareeItem
import com.example.api.NetworkTransactionLog
import com.example.api.NetworkCustomerUpdateRequest
import com.example.api.SareeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SyncState {
    data object IDLE : SyncState
    data object SYNCING : SyncState
    data object SUCCESS : SyncState
    data class ERROR(val message: String) : SyncState
}

class TallyRepository(private val tallyDao: TallyDao) {
    companion object {
        private const val TAG = "TallyRepository"
    }
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
        triggerProductionDeleteSync(item.id)
    }

    // 4. Transaction log triggers with Optimistic Background Sync
    suspend fun updateCustomerDetails(oldName: String, oldNumber: String, newName: String, newNumber: String) {
        tallyDao.updateCustomerDetails(oldName, oldNumber, newName, newNumber)
        repositoryScope.launch {
            try {
                SareeApi.service.updateCustomerDetailsOnServer(
                    NetworkCustomerUpdateRequest(oldName, oldNumber, newName, newNumber)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update customer details on server", e)
            }
        }
    }

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
                        sku = item.sku,
                        color = item.color,
                        fabricType = item.fabricType,
                        brandCategory = item.brandCategory,
                        unitPrice = item.unitPrice,
                        pieceCount = item.pieceCount,
                        imageUrl = item.imageUrl,
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

    private fun triggerProductionDeleteSync(id: Int) {
        if (id <= 0) return
        repositoryScope.launch {
            _syncState.value = SyncState.SYNCING
            try {
                val response = SareeApi.service.deleteProductionItem(id)
                if (response.isSuccessful) {
                    _syncState.value = SyncState.SUCCESS
                } else {
                    _syncState.value = SyncState.ERROR("MongoDB API code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MongoDB production delete failed, operating in offline fallback.", e)
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
                            sku = item.sku,
                            color = item.color,
                            fabricType = item.fabricType,
                            quantity = item.quantity,
                            estimatedCompletionDate = item.estimatedCompletionDate,
                            status = item.status,
                            imageUrl = item.imageUrl,
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
                        sku = log.sku,
                        color = log.color,
                        fabricType = log.fabricType,
                        quantity = log.quantity,
                        unitPrice = log.unitPrice,
                        totalAmount = log.totalAmount,
                        customerName = log.customerName,
                        customerNumber = log.customerNumber,
                        timestamp = log.timestamp,
                        dateString = log.dateString,
                        timeString = log.timeString
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
            val sarees = tallyDao.getAllSareeItemsSync()
            val prodItems = tallyDao.getAllProductionItemsSync()
            val localTransactions = tallyDao.getAllTransactionLogsSync()

            val mappedSarees = sarees.map {
                NetworkSareeItem(
                    id = it.id,
                    modelName = it.modelName,
                    sku = it.sku,
                    color = it.color,
                    fabricType = it.fabricType,
                    brandCategory = it.brandCategory,
                    unitPrice = it.unitPrice,
                    pieceCount = it.pieceCount,
                    imageUrl = it.imageUrl
                )
            }
            mappedSarees.chunked(100).forEach { batch ->
                try {
                    SareeApi.service.syncInventory(batch)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync inventory batch", e)
                }
            }

            val mappedProduction = prodItems.map {
                NetworkProductionItem(
                    id = it.id,
                    modelName = it.modelName,
                    sku = it.sku,
                    color = it.color,
                    fabricType = it.fabricType,
                    quantity = it.quantity,
                    estimatedCompletionDate = it.estimatedCompletionDate,
                    status = it.status,
                    imageUrl = it.imageUrl
                )
            }
            mappedProduction.chunked(100).forEach { batch ->
                try {
                    SareeApi.service.syncProduction(batch)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync production batch", e)
                }
            }

            val mappedTransactions = localTransactions.map { log ->
                NetworkTransactionLog(
                    id = log.id,
                    type = log.type,
                    modelName = log.modelName,
                    sku = log.sku,
                    color = log.color,
                    fabricType = log.fabricType,
                    quantity = log.quantity,
                    unitPrice = log.unitPrice,
                    totalAmount = log.totalAmount,
                    customerName = log.customerName,
                    customerNumber = log.customerNumber,
                    timestamp = log.timestamp,
                    dateString = log.dateString,
                    timeString = log.timeString
                )
            }
            mappedTransactions.chunked(100).forEach { batch ->
                try {
                    SareeApi.service.syncTransactions(batch)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync transactions batch", e)
                }
            }

            // FETCH data from server to keep this device in sync
            val serverInventory = SareeApi.service.getInventory()
            val serverProduction = SareeApi.service.getProduction()
            val serverTransactions = SareeApi.service.getTransactions()

            // Update local database with server truth
            if (serverInventory.isNotEmpty()) {
                tallyDao.deleteAllSareeItems()
                serverInventory.forEach { netItem ->
                    tallyDao.insertSareeItem(
                        SareeItem(
                            id = netItem.id ?: 0,
                            modelName = netItem.modelName,
                            sku = netItem.sku,
                            color = netItem.color,
                            fabricType = netItem.fabricType,
                            brandCategory = netItem.brandCategory,
                            unitPrice = netItem.unitPrice,
                            pieceCount = netItem.pieceCount,
                            imageUrl = netItem.imageUrl
                        )
                    )
                }
            }

            if (serverProduction.isNotEmpty()) {
                tallyDao.deleteAllProductionItems()
                serverProduction.forEach { netItem ->
                    tallyDao.insertProductionItem(
                        ProductionItem(
                            id = netItem.id ?: 0,
                            modelName = netItem.modelName,
                            sku = netItem.sku,
                            color = netItem.color,
                            fabricType = netItem.fabricType,
                            quantity = netItem.quantity,
                            estimatedCompletionDate = netItem.estimatedCompletionDate,
                            status = netItem.status,
                            imageUrl = netItem.imageUrl
                        )
                    )
                }
            }

            if (serverTransactions.isNotEmpty()) {
                tallyDao.deleteAllTransactionLogs()
                serverTransactions.forEach { netItem ->
                    tallyDao.insertTransactionLog(
                        TransactionLog(
                            id = netItem.id ?: 0,
                            type = netItem.type ?: "SALE",
                            modelName = netItem.modelName ?: "Unknown",
                            sku = netItem.sku ?: "",
                            color = netItem.color ?: "",
                            fabricType = netItem.fabricType ?: "",
                            quantity = netItem.quantity ?: 1,
                            unitPrice = netItem.unitPrice ?: 0.0,
                            totalAmount = netItem.totalAmount ?: 0.0,
                            customerName = netItem.customerName ?: "",
                            customerNumber = netItem.customerNumber ?: "",
                            timestamp = netItem.timestamp ?: System.currentTimeMillis(),
                            dateString = netItem.dateString ?: "",
                            timeString = netItem.timeString ?: ""
                        )
                    )
                }
            }

            _syncState.value = SyncState.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Offline sync failed", e)
            _syncState.value = SyncState.ERROR("Offline sync failed: ${e.message}")
        }
    }

    suspend fun sendVerificationEmail(email: String, code: String): Result<Unit> {
        return try {
            val response = SareeApi.service.sendVerificationEmail(com.example.api.NetworkEmailRequest(email, code))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                var errorMsg = "Failed to send email (Code: ${response.code()})"
                try {
                    val jsonObj = org.json.JSONObject(errorBody)
                    if (jsonObj.has("error")) {
                        errorMsg = jsonObj.getString("error")
                    }
                } catch (e: Exception) {
                    errorMsg = errorBody
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send verification email via API", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}
