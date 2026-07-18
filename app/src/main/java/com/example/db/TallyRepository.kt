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
        val updated = item.copy(lastModified = System.currentTimeMillis())
        val id = tallyDao.insertSareeItem(updated)
        val insertedItem = updated.copy(id = id.toInt())
        triggerInventorySync(insertedItem)
    }

    suspend fun updateSareeItem(item: SareeItem) {
        val updated = item.copy(lastModified = System.currentTimeMillis())
        tallyDao.updateSareeItem(updated)
        triggerInventorySync(updated)
    }

    suspend fun deleteSareeItem(item: SareeItem) {
        tallyDao.deleteSareeItem(item)
        triggerInventoryDeleteSync(item.id)
    }

    // 3. Stock in production actions with Optimistic Background Sync
    suspend fun insertProductionItem(item: ProductionItem) {
        val updated = item.copy(lastModified = System.currentTimeMillis())
        val id = tallyDao.insertProductionItem(updated)
        val insertedItem = updated.copy(id = id.toInt())
        triggerProductionSync(insertedItem)
    }

    suspend fun updateProductionItem(item: ProductionItem) {
        val updated = item.copy(lastModified = System.currentTimeMillis())
        tallyDao.updateProductionItem(updated)
        triggerProductionSync(updated)
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
        val updated = log.copy(lastModified = System.currentTimeMillis())
        val id = tallyDao.insertTransactionLog(updated)
        val insertedLog = updated.copy(id = id.toInt())
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
                        timeString = log.timeString,
                        lastModified = log.lastModified
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

            // FETCH data from server to keep this device in sync
            val serverInventory = SareeApi.service.getInventory()
            val serverProduction = SareeApi.service.getProduction()
            val serverTransactions = SareeApi.service.getTransactions()

            // Execute remote pushes (Inventory)
            val serverInvMap = serverInventory.associateBy { it.id ?: 0 }
            val inventoryToPush = mutableListOf<NetworkSareeItem>()

            sarees.forEach { localInv ->
                val serverInv = serverInvMap[localInv.id]
                if (serverInv == null) {
                    inventoryToPush.add(NetworkSareeItem(
                        id = localInv.id, modelName = localInv.modelName, sku = localInv.sku,
                        color = localInv.color, fabricType = localInv.fabricType,
                        brandCategory = localInv.brandCategory, unitPrice = localInv.unitPrice,
                        pieceCount = localInv.pieceCount, imageUrl = localInv.imageUrl,
                        lastModified = localInv.lastModified
                    ))
                } else {
                    if (localInv.lastModified > (serverInv.lastModified ?: 0L)) {
                        inventoryToPush.add(NetworkSareeItem(
                            id = localInv.id, modelName = localInv.modelName, sku = localInv.sku,
                            color = localInv.color, fabricType = localInv.fabricType,
                            brandCategory = localInv.brandCategory, unitPrice = localInv.unitPrice,
                            pieceCount = localInv.pieceCount, imageUrl = localInv.imageUrl,
                            lastModified = localInv.lastModified
                        ))
                    }
                }
            }

            if (inventoryToPush.isNotEmpty()) {
                inventoryToPush.chunked(100).forEach { batch ->
                    try {
                        SareeApi.service.syncInventory(batch)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync inventory batch", e)
                    }
                }
            }

            // Execute remote pushes (Production)
            val serverProdMap = serverProduction.associateBy { it.id ?: 0 }
            val productionToPush = mutableListOf<NetworkProductionItem>()

            prodItems.forEach { localProd ->
                val serverProd = serverProdMap[localProd.id]
                if (serverProd == null) {
                    productionToPush.add(NetworkProductionItem(
                        id = localProd.id, modelName = localProd.modelName, sku = localProd.sku,
                        color = localProd.color, fabricType = localProd.fabricType,
                        quantity = localProd.quantity, estimatedCompletionDate = localProd.estimatedCompletionDate,
                        status = localProd.status, imageUrl = localProd.imageUrl,
                        lastModified = localProd.lastModified
                    ))
                } else {
                    if (localProd.lastModified > (serverProd.lastModified ?: 0L)) {
                        productionToPush.add(NetworkProductionItem(
                            id = localProd.id, modelName = localProd.modelName, sku = localProd.sku,
                            color = localProd.color, fabricType = localProd.fabricType,
                            quantity = localProd.quantity, estimatedCompletionDate = localProd.estimatedCompletionDate,
                            status = localProd.status, imageUrl = localProd.imageUrl,
                            lastModified = localProd.lastModified
                        ))
                    }
                }
            }

            if (productionToPush.isNotEmpty()) {
                productionToPush.chunked(100).forEach { batch ->
                    try {
                        SareeApi.service.syncProduction(batch)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync production batch", e)
                    }
                }
            }


            // --- SMART SYNC FOR TRANSACTIONS ---
            val serverTxMap = serverTransactions.associateBy { it.id ?: 0 }
            val transactionsToPush = mutableListOf<NetworkTransactionLog>()

            localTransactions.forEach { localTx ->
                val serverTx = serverTxMap[localTx.id]
                if (serverTx == null) {
                    transactionsToPush.add(NetworkTransactionLog(
                        id = localTx.id, type = localTx.type, modelName = localTx.modelName,
                        sku = localTx.sku, color = localTx.color, fabricType = localTx.fabricType,
                        quantity = localTx.quantity, unitPrice = localTx.unitPrice,
                        totalAmount = localTx.totalAmount, customerName = localTx.customerName,
                        customerNumber = localTx.customerNumber, timestamp = localTx.timestamp,
                        dateString = localTx.dateString, timeString = localTx.timeString,
                        lastModified = localTx.lastModified
                    ))
                } else {
                    val localMod = localTx.lastModified
                    val serverMod = serverTx.lastModified ?: 0L
                    if (localMod > serverMod) {
                        transactionsToPush.add(NetworkTransactionLog(
                            id = localTx.id, type = localTx.type, modelName = localTx.modelName,
                            sku = localTx.sku, color = localTx.color, fabricType = localTx.fabricType,
                            quantity = localTx.quantity, unitPrice = localTx.unitPrice,
                            totalAmount = localTx.totalAmount, customerName = localTx.customerName,
                            customerNumber = localTx.customerNumber, timestamp = localTx.timestamp,
                            dateString = localTx.dateString, timeString = localTx.timeString,
                            lastModified = localTx.lastModified
                        ))
                    }
                }
            }

            if (transactionsToPush.isNotEmpty()) {
                transactionsToPush.chunked(100).forEach { batch ->
                    try {
                        SareeApi.service.syncTransactions(batch)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync transactions batch", e)
                    }
                }
            }

            val localTxMap = localTransactions.associateBy { it.id }
            serverTransactions.forEach { serverTx ->
                val localTx = localTxMap[serverTx.id ?: 0]
                val localMod = localTx?.lastModified ?: 0L
                val serverMod = serverTx.lastModified ?: 0L

                if (localTx == null || serverMod > localMod) {
                    tallyDao.insertTransactionLog(
                        TransactionLog(
                            id = serverTx.id ?: 0,
                            type = serverTx.type ?: "SALE",
                            modelName = serverTx.modelName ?: "Unknown",
                            sku = serverTx.sku ?: "",
                            color = serverTx.color ?: "",
                            fabricType = serverTx.fabricType ?: "",
                            quantity = serverTx.quantity ?: 1,
                            unitPrice = serverTx.unitPrice ?: 0.0,
                            totalAmount = serverTx.totalAmount ?: 0.0,
                            customerName = serverTx.customerName ?: "",
                            customerNumber = serverTx.customerNumber ?: "",
                            timestamp = serverTx.timestamp ?: System.currentTimeMillis(),
                            dateString = serverTx.dateString ?: "",
                            timeString = serverTx.timeString ?: "",
                            lastModified = serverTx.lastModified ?: 0L
                        )
                    )
                }
            }

            // Update local database with server truth (Smart Pull for Inventory)
            val localInvMap = sarees.associateBy { it.id }
            serverInventory.forEach { serverInv ->
                val localInv = localInvMap[serverInv.id ?: 0]
                val localMod = localInv?.lastModified ?: 0L
                val serverMod = serverInv.lastModified ?: 0L

                if (localInv == null || serverMod > localMod) {
                    tallyDao.insertSareeItem(
                        SareeItem(
                            id = serverInv.id ?: 0,
                            modelName = serverInv.modelName,
                            sku = serverInv.sku,
                            color = serverInv.color,
                            fabricType = serverInv.fabricType,
                            brandCategory = serverInv.brandCategory,
                            unitPrice = serverInv.unitPrice,
                            pieceCount = serverInv.pieceCount,
                            imageUrl = serverInv.imageUrl,
                            lastModified = serverInv.lastModified ?: 0L
                        )
                    )
                }
            }

            // Update local database with server truth (Smart Pull for Production)
            val localProdMap = prodItems.associateBy { it.id }
            serverProduction.forEach { serverProd ->
                val localProd = localProdMap[serverProd.id ?: 0]
                val localMod = localProd?.lastModified ?: 0L
                val serverMod = serverProd.lastModified ?: 0L

                if (localProd == null || serverMod > localMod) {
                    tallyDao.insertProductionItem(
                        ProductionItem(
                            id = serverProd.id ?: 0,
                            modelName = serverProd.modelName,
                            sku = serverProd.sku,
                            color = serverProd.color,
                            fabricType = serverProd.fabricType,
                            quantity = serverProd.quantity,
                            estimatedCompletionDate = serverProd.estimatedCompletionDate,
                            status = serverProd.status,
                            imageUrl = serverProd.imageUrl,
                            lastModified = serverProd.lastModified ?: 0L
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
