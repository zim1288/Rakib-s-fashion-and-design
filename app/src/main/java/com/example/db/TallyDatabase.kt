package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saree_inventory")
data class SareeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "sku") val sku: String = "",
    @ColumnInfo(name = "color") val color: String = "",
    @ColumnInfo(name = "fabric_type") val fabricType: String = "",
    @ColumnInfo(name = "brand_category") val brandCategory: String, // "Rakib Fashion" or "Rakib Silk"
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "piece_count") val pieceCount: Int,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "local_image_url") val localImageUrl: String? = null,
    @ColumnInfo(name = "last_modified") val lastModified: Long = System.currentTimeMillis()
) {
    val totalValue: Double get() = unitPrice * pieceCount
}

@Entity(tableName = "stock_production")
data class ProductionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "sku") val sku: String = "",
    @ColumnInfo(name = "color") val color: String = "",
    @ColumnInfo(name = "fabric_type") val fabricType: String = "",
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "estimated_completion_date") val estimatedCompletionDate: String,
    @ColumnInfo(name = "status") val status: String, // "In Progress" or "Completed"
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "local_image_url") val localImageUrl: String? = null,
    @ColumnInfo(name = "last_modified") val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "transaction_logs")
data class TransactionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "type") val type: String, // "EXPENSE" (Purchase) or "SALE"
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "sku") val sku: String = "",
    @ColumnInfo(name = "color") val color: String = "",
    @ColumnInfo(name = "fabric_type") val fabricType: String = "",
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    @ColumnInfo(name = "customer_number") val customerNumber: String = "",
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "date_string") val dateString: String, // e.g. "2026-06-19"
    @ColumnInfo(name = "time_string") val timeString: String = "", // e.g. "14:30:00"
    @ColumnInfo(name = "last_modified") val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val username: String,
    val password: String,
    @ColumnInfo(name = "security_question") val securityQuestion: String,
    @ColumnInfo(name = "security_answer") val securityAnswer: String,
    @ColumnInfo(name = "is_verified", defaultValue = "0") val isVerified: Boolean = false,
    @ColumnInfo(name = "verification_code") val verificationCode: String? = null,
    @ColumnInfo(name = "code_generated_at") val codeGeneratedAt: Long = 0L
)

@Dao
interface TallyDao {
    @Query("SELECT * FROM saree_inventory ORDER BY model_name ASC")
    fun getAllSareeItems(): Flow<List<SareeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSareeItem(item: SareeItem): Long

    @Query("DELETE FROM saree_inventory")
    suspend fun deleteAllSareeItems()

    @Update
    suspend fun updateSareeItem(item: SareeItem)

    @Delete
    suspend fun deleteSareeItem(item: SareeItem)

    @Query("DELETE FROM saree_inventory WHERE id = :id")
    suspend fun deleteSareeItemById(id: Int)

    @Query("SELECT * FROM saree_inventory WHERE id = :id LIMIT 1")
    suspend fun getSareeItemById(id: Int): SareeItem?

    @Query("SELECT * FROM saree_inventory WHERE model_name = :name AND brand_category = :brand LIMIT 1")
    suspend fun getSareeItemByNameAndBrand(name: String, brand: String): SareeItem?

    @Query("SELECT * FROM stock_production ORDER BY id DESC")
    fun getAllProductionItems(): Flow<List<ProductionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductionItem(item: ProductionItem): Long

    @Query("DELETE FROM stock_production")
    suspend fun deleteAllProductionItems()

    @Update
    suspend fun updateProductionItem(item: ProductionItem)

    @Delete
    suspend fun deleteProductionItem(item: ProductionItem)

    @Query("SELECT * FROM saree_inventory")
    suspend fun getAllSareeItemsSync(): List<SareeItem>

    @Query("SELECT * FROM stock_production ORDER BY estimated_completion_date ASC")
    suspend fun getAllProductionItemsSync(): List<ProductionItem>

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    suspend fun getAllTransactionLogsSync(): List<TransactionLog>

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    fun getAllTransactionLogs(): Flow<List<TransactionLog>>

    @Query("UPDATE transaction_logs SET customer_name = :newName, customer_number = :newNumber, last_modified = :now WHERE type = 'SALE' AND ((customer_number = :oldNumber AND customer_number IS NOT NULL AND TRIM(customer_number) != '') OR ((customer_number IS NULL OR TRIM(customer_number) = '') AND LOWER(TRIM(customer_name)) = LOWER(TRIM(:oldName))))")
    suspend fun updateCustomerDetails(oldName: String, oldNumber: String, newName: String, newNumber: String, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionLog(log: TransactionLog): Long

    @Query("DELETE FROM transaction_logs")
    suspend fun deleteAllTransactionLogs()

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccountByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getUserAccountByUsername(username: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccount): Long
}

@Database(entities = [SareeItem::class, ProductionItem::class, TransactionLog::class, UserAccount::class], version = 11, exportSchema = false)
abstract class TallyDatabase : RoomDatabase() {
    abstract fun tallyDao(): TallyDao
}
