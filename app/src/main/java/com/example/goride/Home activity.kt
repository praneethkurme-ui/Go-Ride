package com.example.goride

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goride.ui.theme.GoRideTheme
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //  Session protection
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            GoRideTheme {
                HomeScreen(
                    userEmail = user.email ?: "User",
                    onLogout = {
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    userEmail: String,
    onLogout: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFB3E5FC), Color(0xFFFFF3E0))
    )

    // Sample data (simple)
    val recentRides = remember {
        mutableStateListOf(
            "City Centre → University",
            "Railway Station → Home",
            "Mall → Office"
        )
    }

    // Dialog state (simple functional actions)
    var showBookDialog by remember { mutableStateOf(false) }
    var showMyRidesDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Simple inputs for "Book Ride"
    var pickup by remember { mutableStateOf("") }
    var drop by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        //  Whole page scrolls + top content visible
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()     //  fixes top hidden issue
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GoRide",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF01579B)
                        )
                        Text(
                            text = "Logged in as: $userEmail",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color.Red
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Action Cards (now functional)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HomeActionCard(
                        title = "Book Ride",
                        icon = Icons.Default.DirectionsCar,
                        onClick = { showBookDialog = true }
                    )
                    HomeActionCard(
                        title = "My Rides",
                        icon = Icons.Default.History,
                        onClick = { showMyRidesDialog = true }
                    )
                    HomeActionCard(
                        title = "Profile",
                        icon = Icons.Default.Person,
                        onClick = { showProfileDialog = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            item {
                Text(
                    text = "Recent Rides",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF01579B)
                )
            }

            items(recentRides) { ride ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF0288D1)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = ride, fontSize = 16.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ---------------------------
        //  Dialogs (simple, functional)
        // ---------------------------

        if (showBookDialog) {
            AlertDialog(
                onDismissRequest = { showBookDialog = false },
                title = { Text("Book a Ride") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = pickup,
                            onValueChange = { pickup = it },
                            label = { Text("Pickup location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drop,
                            onValueChange = { drop = it },
                            label = { Text("Drop location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "This is a simple demo booking (no maps yet).",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pickup.isNotBlank() && drop.isNotBlank()) {
                            recentRides.add(0, "$pickup → $drop")
                            pickup = ""
                            drop = ""
                            showBookDialog = false
                        }
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showBookDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showMyRidesDialog) {
            AlertDialog(
                onDismissRequest = { showMyRidesDialog = false },
                title = { Text("My Rides") },
                text = { Text("You have ${recentRides.size} rides in your history (sample/demo).") },
                confirmButton = {
                    TextButton(onClick = { showMyRidesDialog = false }) { Text("OK") }
                }
            )
        }

        if (showProfileDialog) {
            AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                title = { Text("Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Email: $userEmail")
                        Text("Account: Firebase Authentication")
                        Text(
                            text = "Profile screen is a prototype for now.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProfileDialog = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun HomeActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF0288D1),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
