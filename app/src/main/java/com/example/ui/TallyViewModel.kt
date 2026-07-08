package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TallyApplication
import com.example.db.*
import com.example.api.SareeApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

fun hashPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}

// Auth status states
sealed interface AuthState {
    object NotLoggedIn : AuthState
    object Authenticating : AuthState
    object Authorized : AuthState
    data class VerificationRequired(val email: String) : AuthState
    data class Error(val error: String) : AuthState
}

// Simple representation of cart items
data class CartItem(
    val modelName: String,
    val brandCategory: String, // "Rakib Fashion" or "Rakib Silk"
    val unitCost: Double,
    val quantity: Int,
    val imageUrl: String? = null,
    val sku: String = "",
    val color: String = "",
    val fabricType: String = ""
)

class TallyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TallyRepository = (application as TallyApplication).repository

    private val prefs = application.getSharedPreferences("tally_prefs", android.content.Context.MODE_PRIVATE)

    // UI state states
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotLoggedIn)
    val authState = _authState.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("")
    val currentUserEmail = _currentUserEmail.asStateFlow()

    // Screen navigation tracking
    private val _currentScreen = MutableStateFlow("DASHBOARD") // DASHBOARD, STOCK_HOUSE, STOCK_PRODUCTION, PURCHASE, SELL, HISTORY, SETTINGS
    val currentScreen = _currentScreen.asStateFlow()

    private val _previousScreen = MutableStateFlow<String?>(null)

    // Theme state (null = system default, true = dark, false = light)
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setDarkMode(isDark: Boolean?) {
        _isDarkMode.value = isDark
        if (isDark == null) {
            prefs.edit { remove("is_dark_mode") }
        } else {
            prefs.edit { putBoolean("is_dark_mode", isDark) }
        }
    }

    // Low stock threshold
    private val _lowStockThreshold = MutableStateFlow(10)
    val lowStockThreshold = _lowStockThreshold.asStateFlow()

    fun setLowStockThreshold(threshold: Int) {
        _lowStockThreshold.value = threshold
        prefs.edit { putInt("low_stock_threshold", threshold) }
    }

    // Sync state exposed from Repository
    val syncState = repository.syncState

    // Live Room lists
    val sareeItems = repository.allSareeItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val productionItems = repository.allProductionItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactionLogs = repository.allTransactionLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart list for Purchase Product Sreen
    val purchaseCart = mutableStateListOf<CartItem>()

    // Transaction history search/filters state
    private val _selectedMonthFilter = MutableStateFlow("All") // "All", "Jan", "Feb", "Mar", ...
    val selectedMonthFilter = _selectedMonthFilter.asStateFlow()

    private val _selectedYearFilter = MutableStateFlow("All") // "All", "2026", "2027", "2028"...
    val selectedYearFilter = _selectedYearFilter.asStateFlow()

    // Prepopulate starting database content on startup if totally empty
    init {
        // Restore login and theme state
        val savedEmail = prefs.getString("logged_in_email", null)
        if (savedEmail != null) {
            _currentUserEmail.value = savedEmail
            SareeApi.userEmailHeader = savedEmail
            _authState.value = AuthState.Authorized
            viewModelScope.launch { repository.syncOfflineData() }
        }

        if (prefs.contains("is_dark_mode")) {
            _isDarkMode.value = prefs.getBoolean("is_dark_mode", false)
        }

        if (prefs.contains("low_stock_threshold")) {
            _lowStockThreshold.value = prefs.getInt("low_stock_threshold", 10)
        }

        viewModelScope.launch {
            // Wait for Room flow first emission or inspect database
            sareeItems.firstOrNull()?.let { list ->
                if (list.isEmpty()) {
                    prepopulateDatabase()
                }
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        // No dummy data for real testing
    }

    // Authentication Logic
    fun login(emailOrUsername: String, passwordEntered: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticating
            val credential = emailOrUsername.trim().lowercase()

            var user = repository.getUserAccountByEmail(credential)
            if (user == null) {
                user = repository.getUserAccountByUsername(emailOrUsername.trim())
            }

            if (user != null) {
                val enteredHash = hashPassword(passwordEntered)
                if ((user.password == enteredHash) || (user.password == passwordEntered)) {
                    if (!user.isVerified) {
                        _authState.value = AuthState.VerificationRequired(user.email)
                    } else {
                        _currentUserEmail.value = user.email
                        SareeApi.userEmailHeader = user.email
                        _authState.value = AuthState.Authorized
                        prefs.edit { putString("logged_in_email", user.email) }
                        viewModelScope.launch { repository.syncOfflineData() }
                    }
                } else {
                    _authState.value = AuthState.Error("Incorrect password. Please try again.")
                }
            } else {
                // If not found locally, check the remote server
                val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(credential).matches()
                if (isEmail) {
                    try {
                        val response = SareeApi.service.loginUserOnServer(
                            com.example.api.NetworkUserAuthRequest(credential, hashPassword(passwordEntered))
                        )
                        if (response.isSuccessful) {
                            // User found on server and password matched
                            // Create local record to restore state
                            val restoredUser = UserAccount(
                                email = credential,
                                username = credential.substringBefore("@"),
                                password = hashPassword(passwordEntered),
                                securityQuestion = "",
                                securityAnswer = "",
                                isVerified = true, // We assume they are verified if they could log in on server
                                verificationCode = null,
                                codeGeneratedAt = 0L
                            )
                            repository.insertUserAccount(restoredUser)

                            _currentUserEmail.value = restoredUser.email
                            SareeApi.userEmailHeader = restoredUser.email
                            _authState.value = AuthState.Authorized
                            prefs.edit { putString("logged_in_email", restoredUser.email) }
                            viewModelScope.launch { repository.syncOfflineData() }
                        } else {
                            if (response.code() == 401) {
                                _authState.value = AuthState.Error("Incorrect password. Please try again.")
                            } else {
                                _authState.value = AuthState.Error("Account not found. Please Sign Up to register.")
                            }
                        }
                    } catch (e: Exception) {
                        _authState.value = AuthState.Error("Account not found locally, and failed to reach server. Error: ${e.message}")
                    }
                } else {
                    _authState.value = AuthState.Error("Account not found. Please Sign Up to register.")
                }
            }
        }
    }

    fun registerUser(
        email: String,
        username: String,
        passwordEntered: String,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedEmail = email.trim().lowercase()
            val trimmedUsername = username.trim()
            if (trimmedEmail.isEmpty() || trimmedUsername.isEmpty() || passwordEntered.isEmpty()) {
                callback(false, "All fields are required!")
                return@launch
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                callback(false, "Please enter a valid email address!")
                return@launch
            }
            if (trimmedUsername.length < 3) {
                callback(false, "Username must be at least 3 characters!")
                return@launch
            }

            // Strong password policy validation
            val hasMinLength = passwordEntered.length >= 8
            val hasDigit = passwordEntered.any { it.isDigit() }
            val hasSymbol = passwordEntered.any { !it.isLetterOrDigit() }
            if (!hasMinLength || !hasDigit || !hasSymbol) {
                callback(false, "Password must be at least 8 characters long and contain both numbers and symbols!")
                return@launch
            }

            // Check duplicate email
            val existingEmail = repository.getUserAccountByEmail(trimmedEmail)
            if (existingEmail != null) {
                callback(false, "This email is already registered!")
                return@launch
            }

            // Check duplicate username
            val existingUsername = repository.getUserAccountByUsername(trimmedUsername)
            if (existingUsername != null) {
                callback(false, "This username is already taken!")
                return@launch
            }

            val generatedCode = (100000..999999).random().toString()
            val newUser = UserAccount(
                email = trimmedEmail,
                username = trimmedUsername,
                password = hashPassword(passwordEntered),
                securityQuestion = "",
                securityAnswer = "",
                isVerified = false,
                verificationCode = generatedCode,
                codeGeneratedAt = System.currentTimeMillis()
            )
            repository.insertUserAccount(newUser)

            // Trigger Verification Email and wait for result
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.sendVerificationEmail(trimmedEmail, generatedCode)
            }

            if (result.isSuccess) {
                _authState.value = AuthState.VerificationRequired(trimmedEmail)
                callback(true, "A 6-digit confirmation code is being sent to your email. It may take up to 1 minute.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to send email."
                callback(false, errorMsg)
            }
        }
    }

    fun verifyCode(email: String, enteredCode: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserAccountByEmail(email)
            if (user == null) {
                callback(false, "User account not found.")
                return@launch
            }
            if (user.verificationCode != enteredCode) {
                callback(false, "Incorrect verification code. Please try again.")
                return@launch
            }
            val isExpired = System.currentTimeMillis() - user.codeGeneratedAt > 30 * 60 * 1000L // 30 mins
            if (isExpired) {
                callback(false, "Verification code expired. Please click Resend Code.")
                return@launch
            }

            // Successfully verified!
            val verifiedUser = user.copy(isVerified = true, verificationCode = null, codeGeneratedAt = 0L)
            repository.insertUserAccount(verifiedUser)

            // Push the user to the MongoDB user collection
            try {
                SareeApi.service.registerUserOnServer(
                    com.example.api.NetworkUserAuthRequest(verifiedUser.email, verifiedUser.password)
                )
            } catch (e: Exception) {
                // If it fails, we still allow local login, but log it
                android.util.Log.e("TallyViewModel", "Failed to sync user to production DB", e)
            }

            _currentUserEmail.value = verifiedUser.email
            SareeApi.userEmailHeader = verifiedUser.email
            _authState.value = AuthState.Authorized
            prefs.edit { putString("logged_in_email", verifiedUser.email) }

            callback(true, "Email verified successfully!")
        }
    }

    fun resendVerificationCode(email: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserAccountByEmail(email)
            if (user == null) {
                callback(false, "User account not found.")
                return@launch
            }
            val timeSinceLast = System.currentTimeMillis() - user.codeGeneratedAt
            if (timeSinceLast < (2 * 60 * 1000L)) { // 2 minutes cooldown
                val remainingSec = 120 - (timeSinceLast / 1000)
                callback(false, "Please wait $remainingSec seconds before resending.")
                return@launch
            }

            val newCode = (100000..999999).random().toString()
            val updatedUser = user.copy(verificationCode = newCode, codeGeneratedAt = System.currentTimeMillis())
            repository.insertUserAccount(updatedUser)

            // Trigger Verification Email and wait for result
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.sendVerificationEmail(email, newCode)
            }

            if (result.isSuccess) {
                callback(true, "A new 6-digit confirmation code is being sent to $email. It may take up to 1 minute.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to send email."
                callback(false, errorMsg)
            }
        }
    }

    fun sendRecoveryOtp(email: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val trimmed = email.trim().lowercase()
            var user = repository.getUserAccountByEmail(trimmed)

            if (user == null) {
                // Check if the user exists on the remote backend
                try {
                    val response = SareeApi.service.checkUserOnServer(
                        com.example.api.NetworkEmailRequest(trimmed, "")
                    )
                    if (response.isSuccessful) {
                        // User exists on server, create a dummy local user so recovery can proceed
                        val dummyUser = UserAccount(
                            email = trimmed,
                            username = trimmed.substringBefore("@"),
                            password = "RECOVERING_PASSWORD", // Will be overwritten on reset
                            securityQuestion = "",
                            securityAnswer = "",
                            isVerified = true,
                            verificationCode = null,
                            codeGeneratedAt = 0L
                        )
                        repository.insertUserAccount(dummyUser)
                        user = dummyUser
                    } else {
                        callback(false, "Email address not found!")
                        return@launch
                    }
                } catch (e: Exception) {
                    callback(false, "Email address not found locally, and failed to reach server. Error: ${e.message}")
                    return@launch
                }
            }

            val generatedCode = (100000..999999).random().toString()
            val updatedUser = user.copy(verificationCode = generatedCode, codeGeneratedAt = System.currentTimeMillis())
            repository.insertUserAccount(updatedUser)

            // Trigger Verification Email and wait for result
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.sendVerificationEmail(trimmed, generatedCode)
            }

            if (result.isSuccess) {
                callback(true, "A 6-digit OTP is being sent to your email. It may take up to 1 minute.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to send email."
                callback(false, errorMsg)
            }
        }
    }

    fun verifyRecoveryOtp(email: String, otp: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val trimmed = email.trim().lowercase()
            val user = repository.getUserAccountByEmail(trimmed)
            if (user == null) {
                callback(false, "Email address not found!")
                return@launch
            }
            if (user.verificationCode == otp) {
                val timeDiff = System.currentTimeMillis() - user.codeGeneratedAt
                if (timeDiff > 10 * 60 * 1000) { // 10 mins expiry
                    callback(false, "OTP has expired. Please request a new one.")
                    return@launch
                }
                callback(true, "OTP verified successfully!")
            } else {
                callback(false, "Incorrect OTP!")
            }
        }
    }

    fun resetPassword(email: String, newPassword: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val trimmed = email.trim().lowercase()
            val user = repository.getUserAccountByEmail(trimmed)
            if (user == null) {
                callback(false, "Email address not found!")
                return@launch
            }
            val updatedUser = user.copy(password = newPassword, verificationCode = null, codeGeneratedAt = 0L)
            repository.insertUserAccount(updatedUser)

            // Sync to backend
            try {
                SareeApi.service.registerUserOnServer(
                    com.example.api.NetworkUserAuthRequest(updatedUser.email, updatedUser.password)
                )
            } catch (_: Exception) {}

            callback(true, "Password reset successfully! You can now login.")
        }
    }

    suspend fun getVerificationCodeForDebug(email: String): String? {
        val trimmed = email.trim().lowercase()
        return repository.getUserAccountByEmail(trimmed)?.verificationCode
    }

    fun logout() {
        _currentUserEmail.value = ""
        SareeApi.userEmailHeader = ""
        _authState.value = AuthState.NotLoggedIn
        _currentScreen.value = "DASHBOARD"
        prefs.edit { remove("logged_in_email") }
    }

    fun navigateTo(screen: String) {
        if (screen != "SETTINGS") {
            _previousScreen.value = null
        }
        _currentScreen.value = screen
    }

    fun toggleSettings() {
        if (_currentScreen.value == "SETTINGS") {
            _currentScreen.value = _previousScreen.value ?: "DASHBOARD"
            _previousScreen.value = null
        } else {
            _previousScreen.value = _currentScreen.value
            _currentScreen.value = "SETTINGS"
        }
    }

    // CART MANAGEMENT (Purchase screen)
    fun addToCart(modelName: String, brand: String, cost: Double, quantity: Int, imageUrl: String? = null, sku: String = "", color: String = "", fabricType: String = "") {
        if (modelName.isBlank() || quantity <= 0 || cost <= 0) return
        purchaseCart.add(CartItem(modelName.trim(), brand, cost, quantity, imageUrl, sku, color, fabricType))
    }

    fun removeFromCart(index: Int) {
        if (index in purchaseCart.indices) {
            purchaseCart.removeAt(index)
        }
    }

    fun clearCart() {
        purchaseCart.clear()
    }

    // COMMIT TO STOCK (Optimistic update implementation)
    fun commitCartToStock() {
        val itemsToCommit = purchaseCart.toList()
        if (itemsToCommit.isEmpty()) return

        // 1. Instant optimistic local UI clear
        purchaseCart.clear()

        viewModelScope.launch {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = formatter.format(Date())

            for (cartItem in itemsToCommit) {
                // Check if item exists in present house inventory
                val existing = sareeItems.value.firstOrNull {
                    it.modelName.equals(cartItem.modelName, ignoreCase = true) &&
                            it.brandCategory == cartItem.brandCategory
                }

                if (existing != null) {
                    // Update pieces and recalculate
                    val updated = existing.copy(
                        pieceCount = existing.pieceCount + cartItem.quantity,
                        unitPrice = cartItem.unitCost, // Update to latest purchase cost/price indicator
                        imageUrl = cartItem.imageUrl ?: existing.imageUrl,
                        sku = if (cartItem.sku.isNotBlank()) cartItem.sku else existing.sku,
                        color = if (cartItem.color.isNotBlank()) cartItem.color else existing.color,
                        fabricType = if (cartItem.fabricType.isNotBlank()) cartItem.fabricType else existing.fabricType
                    )
                    repository.updateSareeItem(updated)
                } else {
                    // Create new saree item
                    val newItem = SareeItem(
                        modelName = cartItem.modelName,
                        brandCategory = cartItem.brandCategory,
                        unitPrice = cartItem.unitCost,
                        pieceCount = cartItem.quantity,
                        imageUrl = cartItem.imageUrl,
                        sku = cartItem.sku,
                        color = cartItem.color,
                        fabricType = cartItem.fabricType
                    )
                    repository.insertSareeItem(newItem)
                }

                // Add permanent Expense logs
                val totalCost = cartItem.unitCost * cartItem.quantity
                val expenseLog = TransactionLog(
                    type = "EXPENSE",
                    modelName = cartItem.modelName,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.unitCost,
                    totalAmount = totalCost,
                    dateString = dateStr
                )
                repository.insertTransactionLog(expenseLog)
            }
        }
    }

    // SALES LOGGING (Optimistic UI Update)
    fun logClientSale(sareeItem: SareeItem, quantityToSell: Int, salePricePerPiece: Double, onError: (String) -> Unit, onSuccess: () -> Unit) {
        if (quantityToSell <= 0 || salePricePerPiece <= 0) {
            onError("Please enter valid positive numbers")
            return
        }
        if (quantityToSell > sareeItem.pieceCount) {
            onError("Insufficient Stock In House! Only ${sareeItem.pieceCount} pieces available.")
            return
        }

        viewModelScope.launch {
            // Deduct from inventory instantly
            val updatedItem = sareeItem.copy(pieceCount = sareeItem.pieceCount - quantityToSell)
            repository.updateSareeItem(updatedItem)

            // Log Sale transaction
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = formatter.format(Date())
            val totalSalesValue = salePricePerPiece * quantityToSell
            val saleLog = TransactionLog(
                type = "SALE",
                modelName = sareeItem.modelName,
                quantity = quantityToSell,
                unitPrice = salePricePerPiece,
                totalAmount = totalSalesValue,
                dateString = dateStr
            )
            repository.insertTransactionLog(saleLog)
            onSuccess()
        }
    }

    // Toggle production item status: "In Progress" <-> "Completed"
    fun toggleProductionStatus(item: ProductionItem) {
        val newStatus = if (item.status == "In Progress") "Completed" else "In Progress"
        val updated = item.copy(status = newStatus)

        viewModelScope.launch {
            repository.updateProductionItem(updated)

            // Dynamic quality of life: If completed, let's automatically add this finished lot to Stock in House (Rakib Silk)
            if (newStatus == "Completed") {
                // Find existing or insert new automatically
                val existing = sareeItems.value.firstOrNull {
                    it.modelName.equals(item.modelName, ignoreCase = true) &&
                            it.brandCategory == "Rakib Silk" // Defaults to Rakib Silk production
                }

                if (existing != null) {
                    repository.updateSareeItem(existing.copy(
                        pieceCount = existing.pieceCount + item.quantity,
                        imageUrl = item.imageUrl ?: existing.imageUrl,
                        sku = if (item.sku.isNotBlank()) item.sku else existing.sku,
                        color = if (item.color.isNotBlank()) item.color else existing.color,
                        fabricType = if (item.fabricType.isNotBlank()) item.fabricType else existing.fabricType
                    ))
                } else {
                    repository.insertSareeItem(
                        SareeItem(
                            modelName = item.modelName,
                            brandCategory = "Rakib Silk",
                            unitPrice = 5000.0, // Default estimated cost
                            pieceCount = item.quantity,
                            imageUrl = item.imageUrl,
                            sku = item.sku,
                            color = item.color,
                            fabricType = item.fabricType
                        )
                    )
                }

                // Add log for production completion stock upgrade
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateStr = formatter.format(Date())
                repository.insertTransactionLog(
                    TransactionLog(
                        type = "PRODUCTION_FINISHED",
                        modelName = "${item.modelName} (Production Finished)",
                        quantity = item.quantity,
                        unitPrice = 0.0,
                        totalAmount = 0.0,
                        dateString = dateStr
                    )
                )
            }
        }
    }

    fun addNewProductionItem(modelName: String, qty: Int, completionDate: String, imageUrl: String? = null, sku: String = "", color: String = "", fabricType: String = "") {
        if (modelName.isBlank() || qty <= 0 || completionDate.isBlank()) return
        viewModelScope.launch {
            val item = ProductionItem(
                modelName = modelName.trim(),
                quantity = qty,
                estimatedCompletionDate = completionDate.trim(),
                status = "In Progress",
                imageUrl = imageUrl,
                sku = sku,
                color = color,
                fabricType = fabricType
            )
            repository.insertProductionItem(item)
        }
    }

    fun deleteProductionItem(item: ProductionItem) {
        viewModelScope.launch {
            repository.deleteProductionItem(item)
        }
    }

    fun updateStockItemDetails(id: Int, name: String, sku: String, color: String, fabricType: String, category: String, price: Double, count: Int, imageUrl: String? = null) {
        viewModelScope.launch {
            val existing = sareeItems.value.firstOrNull { it.id == id }
            val finalImageUrl = if (imageUrl.isNullOrBlank()) existing?.imageUrl else imageUrl
            val updated = SareeItem( sku = sku, color = color, fabricType = fabricType,
                id = id,
                modelName = name,
                brandCategory = category,
                unitPrice = price,
                pieceCount = count,
                imageUrl = finalImageUrl
            )
            repository.updateSareeItem(updated)
        }
    }

    fun deleteStockItem(item: SareeItem) {
        viewModelScope.launch {
            repository.deleteSareeItem(item)
        }
    }

    // Setters for transaction filters
    fun addStockItemDirectly(name: String, sku: String, color: String, fabricType: String, category: String, price: Double, count: Int, imageUrl: String? = null) {
        viewModelScope.launch {
            val item = SareeItem(
                modelName = name,
                sku = sku,
                color = color,
                fabricType = fabricType,
                brandCategory = category,
                unitPrice = price,
                pieceCount = count,
                imageUrl = imageUrl
            )
            repository.insertSareeItem(item)
        }
    }
    fun setMonthFilter(month: String) {
        _selectedMonthFilter.value = month
    }

    fun setYearFilter(year: String) {
        _selectedYearFilter.value = year
    }

    fun forceSync() {
        viewModelScope.launch {
            repository.syncOfflineData()
        }
    }
}
