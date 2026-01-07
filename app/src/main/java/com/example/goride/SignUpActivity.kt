package com.example.goride

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goride.ui.theme.GoRideTheme
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GoRideTheme {
                SignUpScreen(
                    onSignUp = { email, password, onResult ->
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onResult(true, null)
                                } else {
                                    onResult(false, task.exception?.message ?: "Signup failed")
                                }
                            }
                    },
                    onGoToLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onGoToHome = {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SignUpScreen(
    onSignUp: (email: String, password: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onGoToLogin: () -> Unit,
    onGoToHome: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val gradientColors = listOf(
        Color(0xFFB3E5FC),
        Color(0xFFFFD580)
    )

    fun attemptSignUp() {
        errorMessage = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            errorMessage = "Please enter a valid email."
            return
        }
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }

        isLoading = true
        onSignUp(email, password) { success, msg ->
            isLoading = false
            if (success) onGoToHome()
            else errorMessage = msg ?: "Signup failed"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create your GoRide account",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF01579B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { attemptSignUp() })
                )

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = it, color = Color.Red, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { attemptSignUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Creating...", color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(
                            text = "Sign Up",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Already have an account? Login",
                    color = Color(0xFF01579B),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(enabled = !isLoading) { onGoToLogin() }
                )
            }
        }
    }
}
