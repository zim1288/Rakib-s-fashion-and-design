package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.db.*
import com.example.ui.AuthState
import com.example.ui.TallyViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
enum class AuthMode { SIGN_IN, SIGN_UP, FORGOT_PASSWORD }

@Composable
fun LoginScreen(viewModel: TallyViewModel, authState: AuthState) {
    if (authState is AuthState.VerificationRequired) {
        EmailVerificationLayout(viewModel = viewModel, email = authState.email)
        return
    }

    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    val focusManager = LocalFocusManager.current

    // Sign In Fields
    var loginUserOrEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Sign Up Fields
    var signUpEmail by remember { mutableStateOf("") }
    var signUpUsername by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var signUpConfirmPasswordVisible by remember { mutableStateOf(false) }
    var signUpStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isSignUpLoading by remember { mutableStateOf(false) }

    // Forgot Password Fields
    var forgotEmail by remember { mutableStateOf("") }
    var forgotStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isSendOtpLoading by remember { mutableStateOf(false) }
    var isRecoveryOtpSent by remember { mutableStateOf(false) }
    var isRecoveryOtpVerified by remember { mutableStateOf(false) }
    var recoveryOtpText by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var recoveryNewPasswordVisible by remember { mutableStateOf(false) }

    var debugRecoveryOtp by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(forgotEmail, isRecoveryOtpSent) {
        if (isRecoveryOtpSent) {
            debugRecoveryOtp = viewModel.getVerificationCodeForDebug(forgotEmail)
        } else {
            debugRecoveryOtp = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Spacer(modifier = Modifier.height(36.dp))

            // Decorative South Asian Paisley/Mandala styled icon
            Card(
                modifier = Modifier
                    .size(80.dp)
                    .testTag("login_logo_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Royal Logo",
                        tint = GoldAccent,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Rakib Silk & Fashion",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mode switching and primary container card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    when (mode) {
                        AuthMode.SIGN_IN -> {
                            Text(
                                text = "Sign In To Account",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = loginUserOrEmail,
                                onValueChange = { loginUserOrEmail = it },
                                label = { Text("Email or Username") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Email/Username", tint = RoyalCrimson) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_username_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalCrimson,
                                    focusedLabelColor = RoyalCrimson
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = RoyalCrimson) },
                                trailingIcon = {
                                    IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (loginPasswordVisible) Icons.Default.Info else Icons.Default.Lock,
                                            contentDescription = "Toggle password visibility",
                                            tint = RoyalCrimson.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalCrimson,
                                    focusedLabelColor = RoyalCrimson
                                )
                            )

                            // Forget Password Switcher Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        forgotStatusMessage = null
                                        mode = AuthMode.FORGOT_PASSWORD
                                    },
                                    modifier = Modifier.testTag("forgot_password_btn")
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        color = RoyalCrimson,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }

                            if (authState is AuthState.Error) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = authState.error,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.login(loginUserOrEmail, loginPassword)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_btn_submit"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalCrimson,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (authState is AuthState.Authenticating) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("AUTHENTICATE & ENTER", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Go to SignUp Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                TextButton(
                                    onClick = {
                                        signUpStatusMessage = null
                                        mode = AuthMode.SIGN_UP
                                    },
                                    modifier = Modifier.testTag("sign_up_mode_btn")
                                ) {
                                    Text("Sign Up", color = RoyalCrimson, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        AuthMode.SIGN_UP -> {
                            Text(
                                text = "Create Staff Account",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = signUpEmail,
                                onValueChange = { signUpEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = RoyalCrimson) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalCrimson,
                                    focusedLabelColor = RoyalCrimson
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = signUpUsername,
                                onValueChange = { signUpUsername = it },
                                label = { Text("Staff Username") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username", tint = RoyalCrimson) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_username_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalCrimson,
                                    focusedLabelColor = RoyalCrimson
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = signUpPassword,
                                onValueChange = { signUpPassword = it },
                                label = { Text("Password (Min 8 chars, number & symbol)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = RoyalCrimson) },
                                trailingIcon = {
                                    IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                                        Icon(
                                            imageVector = if (signUpPasswordVisible) Icons.Default.Info else Icons.Default.Lock,
                                            contentDescription = "Toggle password visibility",
                                            tint = RoyalCrimson.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalCrimson,
                                    focusedLabelColor = RoyalCrimson
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = signUpConfirmPassword,
                                onValueChange = { signUpConfirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = RoyalCrimson) },
                                trailingIcon = {
                                    IconButton(onClick = { signUpConfirmPasswordVisible = !signUpConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (signUpConfirmPasswordVisible) Icons.Default.Info else Icons.Default.Lock,
                                            contentDescription = "Toggle confirm password visibility",
                                            tint = RoyalCrimson.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                visualTransformation = if (signUpConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_confirm_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalCrimson,
                                    focusedLabelColor = RoyalCrimson
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            signUpStatusMessage?.let { (isSuccess, message) ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSuccess) SageGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = message,
                                        color = if (isSuccess) Color(0xFF2D332C) else MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (signUpPassword != signUpConfirmPassword) {
                                        signUpStatusMessage = Pair(false, "Passwords do not match!")
                                    } else {
                                        isSignUpLoading = true
                                        viewModel.registerUser(
                                            email = signUpEmail,
                                            username = signUpUsername,
                                            passwordEntered = signUpPassword
                                        ) { success, msg ->
                                            isSignUpLoading = false
                                            signUpStatusMessage = Pair(success, msg)
                                            if (success) {
                                                // Reset fields
                                                signUpEmail = ""
                                                signUpUsername = ""
                                                signUpPassword = ""
                                                signUpConfirmPassword = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("signup_btn_submit"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalCrimson,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isSignUpLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("SIGN UP & CREATE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                TextButton(
                                    onClick = { mode = AuthMode.SIGN_IN },
                                    modifier = Modifier.testTag("back_to_signin_btn")
                                ) {
                                    Text("Sign In", color = RoyalCrimson, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        AuthMode.FORGOT_PASSWORD -> {
                            Text(
                                text = "Recover Your Password",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = forgotEmail,
                                onValueChange = { forgotEmail = it },
                                label = { Text("Registered Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = RoyalCrimson) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalCrimson,
                                    focusedLabelColor = RoyalCrimson
                                )
                            )

                            if (!isRecoveryOtpVerified) {
                                Spacer(modifier = Modifier.height(8.dp))

                                if (!isRecoveryOtpSent) {
                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            if (forgotEmail.isBlank()) {
                                                forgotStatusMessage = Pair(false, "Please enter your registered email.")
                                                return@Button
                                            }
                                            isSendOtpLoading = true
                                            viewModel.sendRecoveryOtp(forgotEmail) { isSuccess, resultText ->
                                                isSendOtpLoading = false
                                                forgotStatusMessage = Pair(isSuccess, resultText)
                                                if (isSuccess) isRecoveryOtpSent = true
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("forgot_send_otp_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RoyalCrimson,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isSendOtpLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text("SEND OTP", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OtpInputField(
                                        otpText = recoveryOtpText,
                                        onOtpChange = { recoveryOtpText = it },
                                        modifier = Modifier.testTag("forgot_otp_input")
                                    )
                                    if (debugRecoveryOtp != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Testing Helper (Local DB): OTP is $debugRecoveryOtp",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(10.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            if (recoveryOtpText.length != 6) {
                                                forgotStatusMessage = Pair(false, "Please enter 6-digit OTP")
                                                return@Button
                                            }
                                            viewModel.verifyRecoveryOtp(forgotEmail, recoveryOtpText) { isSuccess, resultText ->
                                                forgotStatusMessage = Pair(isSuccess, resultText)
                                                if (isSuccess) isRecoveryOtpVerified = true
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("forgot_confirm_otp_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RoyalCrimson,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("CONFIRM OTP", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = recoveryNewPassword,
                                    onValueChange = { recoveryNewPassword = it },
                                    label = { Text("New Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "New Password", tint = RoyalCrimson) },
                                    singleLine = true,
                                    visualTransformation = if (recoveryNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        val image = if (recoveryNewPasswordVisible) Icons.Default.Lock else Icons.Default.Lock // Just simple toggle
                                        IconButton(onClick = { recoveryNewPasswordVisible = !recoveryNewPasswordVisible }) {
                                            Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = RoyalCrimson)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("forgot_new_password_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RoyalCrimson,
                                        focusedLabelColor = RoyalCrimson
                                    )
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (recoveryNewPassword.length < 6) {
                                            forgotStatusMessage = Pair(false, "Password must be at least 6 characters")
                                            return@Button
                                        }
                                        viewModel.resetPassword(forgotEmail, recoveryNewPassword) { isSuccess, resultText ->
                                            forgotStatusMessage = Pair(isSuccess, resultText)
                                            if (isSuccess) {
                                                // Reset state and switch back to sign in mode after 2 seconds
                                                isRecoveryOtpSent = false
                                                isRecoveryOtpVerified = false
                                                forgotEmail = ""
                                                recoveryOtpText = ""
                                                recoveryNewPassword = ""
                                                mode = AuthMode.SIGN_IN
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("forgot_reset_password_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalCrimson,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("RESET PASSWORD", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            forgotStatusMessage?.let { (isSuccess, message) ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSuccess) SageGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = message,
                                        color = if (isSuccess) Color(0xFF2D332C) else MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSuccess) FontWeight.Bold else FontWeight.Normal),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(
                                onClick = { mode = AuthMode.SIGN_IN },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_back_to_signin_btn")
                            ) {
                                Text("Back to Sign In", color = RoyalCrimson, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun EmailVerificationLayout(viewModel: TallyViewModel, email: String) {
    var verificationCodeInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(120) }
    var isResendLoading by remember { mutableStateOf(false) }

    var debugOtp by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(email, secondsRemaining) {
        debugOtp = viewModel.getVerificationCodeForDebug(email)
    }

    LaunchedEffect(key1 = secondsRemaining) {
        if (secondsRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            secondsRemaining -= 1
        }
    }

    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Spacer(modifier = Modifier.height(36.dp))

            // Decorative Email Card
            Card(
                modifier = Modifier
                    .size(80.dp)
                    .testTag("verification_logo_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Code",
                        tint = GoldAccent,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "We have sent a 6-digit confirmation code to your email:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Container card for PIN input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verification_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Confirmation Code",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OtpInputField(
                        otpText = verificationCodeInput,
                        onOtpChange = { verificationCodeInput = it },
                        modifier = Modifier.testTag("verification_code_input")
                    )

                    if (debugOtp != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Testing Helper (Local DB): OTP is $debugOtp",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    statusMessage?.let { (isSuccess, message) ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuccess) SageGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = message,
                                color = if (isSuccess) Color(0xFF2D332C) else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (verificationCodeInput.length != 6) {
                                statusMessage = Pair(false, "Please enter the full 6-digit code.")
                                return@Button
                            }
                            viewModel.verifyCode(email, verificationCodeInput) { success, msg ->
                                statusMessage = Pair(success, msg)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("verify_code_submit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("VERIFY EMAIL", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (secondsRemaining > 0) {
                        Text(
                            text = "Resend code in ${secondsRemaining / 60}:${String.format(Locale.getDefault(), "%02d", secondsRemaining % 60)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                isResendLoading = true
                                viewModel.resendVerificationCode(email) { success, msg ->
                                    isResendLoading = false
                                    statusMessage = Pair(success, msg)
                                    if (success) {
                                        secondsRemaining = 120
                                    }
                                }
                            },
                            modifier = Modifier.testTag("resend_code_btn")
                        ) {
                            if (isResendLoading) {
                                CircularProgressIndicator(color = RoyalCrimson, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    "Resend Code",
                                    color = RoyalCrimson,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cancel / Back to login link
            TextButton(
                onClick = {
                    viewModel.logout()
                },
                modifier = Modifier.testTag("back_to_signin_after_signup_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = RoyalCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Back to Sign In",
                        color = RoyalCrimson,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun OtpInputField(
    otpText: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    otpLength: Int = 6
) {
    val focusRequesters = remember { List(otpLength) { FocusRequester() } }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0 until otpLength) {
            val char = otpText.getOrNull(i)?.toString() ?: ""
            OutlinedTextField(
                value = char,
                onValueChange = { value ->
                    val newVal = value.filter { it.isDigit() }
                    if (newVal.isNotEmpty()) {
                        val newOtp = otpText.padEnd(otpLength, ' ').replaceRange(i, i + 1, newVal.takeLast(1)).replace(" ", "")
                        onOtpChange(newOtp.take(otpLength))
                        if (i < otpLength - 1) {
                            focusRequesters[i + 1].requestFocus()
                        }
                    } else {
                        val newOtp = otpText.padEnd(otpLength, ' ').replaceRange(i, i + 1, " ").trimEnd()
                        onOtpChange(newOtp)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .aspectRatio(1f)
                    .focusRequester(focusRequesters[i])
                    .onKeyEvent { event ->
                        if (event.key == Key.Backspace && char.isEmpty() && i > 0) {
                            focusRequesters[i - 1].requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalCrimson,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            )
        }
    }
}
