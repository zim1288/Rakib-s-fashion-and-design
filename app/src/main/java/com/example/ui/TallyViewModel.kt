package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TallyApplication
import com.example.db.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Auth status states
sealed interface AuthState {
    object NOT_LOGGED_IN : AuthState
    object AUTHENTICATING : AuthState
    object AUTHORIZED : AuthState
    data class ERROR(val error: String) : AuthState
}

// Simple representation of cart items
data class CartItem(
    val modelName: String,
    val brandCategory: String, // "Rakib Fashion" or "Rakib Silk"
    val unitCost: Double,
    val quantity: Int
)

class TallyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TallyRepository = (application as TallyApplication).repository

    // 10 Hardcoded Authorized Email Addresses (including current user and other members)
    val authorizedEmails = listOf(
        "watchdogs27777@gmail.com", // Fulfills userinfo metadata uniquely!
        "rakib.silk.owner@gmail.com",
        "rakib.fashion.manager@gmail.com",
        "tally.admin@rakibsilk.com",
        "sales.manager@rakibsilk.com",
        "inventory.clerk@rakibsilk.com",
        "bookkeeper.faisal@gmail.com",
        "designer.rakib@gmail.com",
        "accounts.tally@rakibsilk.com",
        "khata.supervisor@gmail.com"
    )

    private val prefs = application.getSharedPreferences("tally_prefs", android.content.Context.MODE_PRIVATE)

    // UI state states
    private val _authState = MutableStateFlow<AuthState>(AuthState.NOT_LOGGED_IN)
    val authState = _authState.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("")
    val currentUserEmail = _currentUserEmail.asStateFlow()

    // Screen navigation tracking
    private val _currentScreen = MutableStateFlow("DASHBOARD") // DASHBOARD, STOCK_HOUSE, STOCK_PRODUCTION, PURCHASE, SELL, HISTORY, SETTINGS
    val currentScreen = _currentScreen.asStateFlow()

    // Theme state (null = system default, true = dark, false = light)
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setDarkMode(isDark: Boolean?) {
        _isDarkMode.value = isDark
        if (isDark == null) {
            prefs.edit().remove("is_dark_mode").apply()
        } else {
            prefs.edit().putBoolean("is_dark_mode", isDark).apply()
        }
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
            _authState.value = AuthState.AUTHORIZED
        }
        
        if (prefs.contains("is_dark_mode")) {
            _isDarkMode.value = prefs.getBoolean("is_dark_mode", false)
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
        val initialSarees = listOf(
            SareeItem(modelName = "Katan Gold Zardosi", brandCategory = "Rakib Silk", unitPrice = 7500.0, pieceCount = 24),
            SareeItem(modelName = "Royal Dhakai Jamdani", brandCategory = "Rakib Silk", unitPrice = 12500.0, pieceCount = 15),
            SareeItem(modelName = "Banarasi Crimson Queen", brandCategory = "Rakib Silk", unitPrice = 16000.0, pieceCount = 8),
            SareeItem(modelName = "Chiffon Summer Pastel", brandCategory = "Rakib Fashion", unitPrice = 4200.0, pieceCount = 45),
            SareeItem(modelName = "Georgette Floral Breeze", brandCategory = "Rakib Fashion", unitPrice = 3800.0, pieceCount = 32),
            SareeItem(modelName = "Organza Golden Weave", brandCategory = "Rakib Fashion", unitPrice = 5900.0, pieceCount = 18)
        )
        for (item in initialSarees) {
            repository.insertSareeItem(item)
        }

        val initialProduction = listOf(
            ProductionItem(modelName = "Premium Silk Jamdani Mix", quantity = 10, estimatedCompletionDate = "2026-06-28", status = "In Progress"),
            ProductionItem(modelName = "Embroidered Georgette Rose", quantity = 25, estimatedCompletionDate = "2026-07-05", status = "In Progress"),
            ProductionItem(modelName = "Semi-Katan Party Look", quantity = 15, estimatedCompletionDate = "2026-06-25", status = "Completed")
        )
        for (item in initialProduction) {
            repository.insertProductionItem(item)
        }

        // Add starting logs to show Tally Khata immediately
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = formatter.format(Date())
        repository.insertTransactionLog(
            TransactionLog(type = "EXPENSE", modelName = "Banarasi Crimson Queen Starter Pack", quantity = 10, unitPrice = 11000.0, totalAmount = 110000.0, dateString = dateStr)
        )
        repository.insertTransactionLog(
            TransactionLog(type = "SALE", modelName = "Katan Gold Zardosi Launch Sale", quantity = 3, unitPrice = 8500.0, totalAmount = 25500.0, dateString = dateStr)
        )

        // Prepopulate default accounts for standard emails
        for (email in authorizedEmails) {
            val username = email.substringBefore("@")
            repository.insertUserAccount(
                UserAccount(
                    email = email,
                    username = username,
                    password = "password123",
                    securityQuestion = "What is your main brand?",
                    securityAnswer = "Rakib Silk"
                )
            )
        }
    }

    // Authentication Logic
    fun login(emailOrUsername: String, passwordEntered: String) {
        viewModelScope.launch {
            _authState.value = AuthState.AUTHENTICATING
            val credential = emailOrUsername.trim().lowercase()

            var user = repository.getUserAccountByEmail(credential)
            if (user == null) {
                user = repository.getUserAccountByUsername(emailOrUsername.trim())
            }

            if (user != null) {
                if (user.password == passwordEntered) {
                    _currentUserEmail.value = user.email
                    _authState.value = AuthState.AUTHORIZED
                    prefs.edit().putString("logged_in_email", user.email).apply()
                } else {
                    _authState.value = AuthState.ERROR("Incorrect password. Please try again.")
                }
            } else {
                // Fallback check for standard authorized accounts if they haven't been compiled yet
                if (credential in authorizedEmails.map { it.lowercase() } && passwordEntered == "password123") {
                    _currentUserEmail.value = credential
                    _authState.value = AuthState.AUTHORIZED
                    prefs.edit().putString("logged_in_email", credential).apply()
                } else {
                    _authState.value = AuthState.ERROR("Staff account not found. Please Sign Up to register your credentials.")
                }
            }
        }
    }

    fun registerUser(
        email: String,
        username: String,
        passwordEntered: String,
        securityQuestion: String,
        securityAnswer: String,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedEmail = email.trim().lowercase()
            val trimmedUsername = username.trim()
            if (trimmedEmail.isEmpty() || trimmedUsername.isEmpty() || passwordEntered.isEmpty() || securityAnswer.isEmpty()) {
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
            if (passwordEntered.length < 4) {
                callback(false, "Password must be at least 4 characters!")
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

            val newUser = UserAccount(
                email = trimmedEmail,
                username = trimmedUsername,
                password = passwordEntered,
                securityQuestion = securityQuestion,
                securityAnswer = securityAnswer.trim()
            )
            repository.insertUserAccount(newUser)
            callback(true, "Registration successful! You can now log in.")
        }
    }

    fun verifySecurityAnswer(
        email: String,
        securityQuestion: String,
        answer: String,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmed = email.trim().lowercase()
            val user = repository.getUserAccountByEmail(trimmed)
            if (user == null) {
                callback(false, "Email address not found!")
                return@launch
            }
            if (user.securityQuestion != securityQuestion) {
                callback(false, "Security question mismatch!")
                return@launch
            }
            if (user.securityAnswer.trim().lowercase() == answer.trim().lowercase()) {
                callback(true, "Your password is: ${user.password}")
            } else {
                callback(false, "Answer is incorrect. Try again!")
            }
        }
    }

    fun logout() {
        _currentUserEmail.value = ""
        _authState.value = AuthState.NOT_LOGGED_IN
        _currentScreen.value = "DASHBOARD"
        prefs.edit().remove("logged_in_email").apply()
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // CART MANAGEMENT (Purchase screen)
    fun addToCart(modelName: String, brand: String, cost: Double, quantity: Int) {
        if (modelName.isBlank() || quantity <= 0 || cost <= 0) return
        purchaseCart.add(CartItem(modelName.trim(), brand, cost, quantity))
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
                    it.modelName.lowercase() == cartItem.modelName.lowercase() &&
                            it.brandCategory == cartItem.brandCategory
                }

                if (existing != null) {
                    // Update pieces and recalculate
                    val updated = existing.copy(
                        pieceCount = existing.pieceCount + cartItem.quantity,
                        unitPrice = cartItem.unitCost // Update to latest purchase cost/price indicator
                    )
                    repository.updateSareeItem(updated)
                } else {
                    // Create new saree item
                    val newItem = SareeItem(
                        modelName = cartItem.modelName,
                        brandCategory = cartItem.brandCategory,
                        unitPrice = cartItem.unitCost,
                        pieceCount = cartItem.quantity
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
                    it.modelName.lowercase() == item.modelName.lowercase() &&
                            it.brandCategory == "Rakib Silk" // Defaults to Rakib Silk production
                }
                
                if (existing != null) {
                    repository.updateSareeItem(existing.copy(pieceCount = existing.pieceCount + item.quantity))
                } else {
                    repository.insertSareeItem(
                        SareeItem(
                            modelName = item.modelName,
                            brandCategory = "Rakib Silk",
                            unitPrice = 5000.0, // Default estimated cost
                            pieceCount = item.quantity
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

    fun addNewProductionItem(modelName: String, qty: Int, completionDate: String) {
        if (modelName.isBlank() || qty <= 0 || completionDate.isBlank()) return
        viewModelScope.launch {
            val item = ProductionItem(
                modelName = modelName.trim(),
                quantity = qty,
                estimatedCompletionDate = completionDate.trim(),
                status = "In Progress"
            )
            repository.insertProductionItem(item)
        }
    }

    fun updateStockItemDetails(id: Int, name: String, category: String, price: Double, count: Int, imageUrl: String? = null) {
        viewModelScope.launch {
            val updated = SareeItem(
                id = id,
                modelName = name,
                brandCategory = category,
                unitPrice = price,
                pieceCount = count,
                imageUrl = imageUrl
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
    fun setMonthFilter(month: String) {
        _selectedMonthFilter.value = month
    }

    fun setYearFilter(year: String) {
        _selectedYearFilter.value = year
    }
}
