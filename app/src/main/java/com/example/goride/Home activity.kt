package com.example.goride

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goride.ui.theme.GoRideTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                    uid = user.uid,
                    db = db,
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

data class RideItem(
    val id: String = "",
    val pickup: String = "",
    val drop: String = "",
    val createdAt: Timestamp? = null
)

@Composable
fun HomeScreen(
    userEmail: String,
    uid: String,
    db: FirebaseFirestore,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFB3E5FC), Color(0xFFFFF3E0))
    )

    //  Firestore rides state
    val rides = remember { mutableStateListOf<RideItem>() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    //  Profile (Firestore users/{uid})
    var displayName by remember { mutableStateOf("GoRide User") }
    var isProfileLoading by remember { mutableStateOf(true) }

    // Dialog state
    var showBookDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showDeleteRideDialog by remember { mutableStateOf(false) }

    var pickup by remember { mutableStateOf("") }
    var drop by remember { mutableStateOf("") }

    // For delete confirmation
    var selectedRide by remember { mutableStateOf<RideItem?>(null) }

    //  Load Profile once (users/{uid})
    LaunchedEffect(uid) {
        isProfileLoading = true
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name")
                if (!name.isNullOrBlank()) displayName = name
                isProfileLoading = false
            }
            .addOnFailureListener {
                isProfileLoading = false
            }
    }

    //  Live listener for rides
    DisposableEffect(uid) {
        var reg: ListenerRegistration? = null
        reg = db.collection("users")
            .document(uid)
            .collection("rides")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                isLoading = false
                if (e != null) {
                    error = e.message ?: "Failed to load rides"
                    return@addSnapshotListener
                }
                error = null
                rides.clear()
                snap?.documents?.forEach { doc ->
                    rides.add(
                        RideItem(
                            id = doc.id,
                            pickup = doc.getString("pickup") ?: "",
                            drop = doc.getString("drop") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    )
                }
            }

        onDispose { reg?.remove() }
    }

    fun addRideToFirestore(pick: String, dr: String) {
        val rideData = hashMapOf(
            "pickup" to pick.trim(),
            "drop" to dr.trim(),
            "createdAt" to Timestamp.now()
        )

        db.collection("users")
            .document(uid)
            .collection("rides")
            .add(rideData)
            .addOnSuccessListener {
                Toast.makeText(context, "Ride booked!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { ex ->
                Toast.makeText(context, ex.message ?: "Booking failed", Toast.LENGTH_SHORT).show()
            }
    }

    fun deleteRide(ride: RideItem) {
        db.collection("users")
            .document(uid)
            .collection("rides")
            .document(ride.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Ride deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { ex ->
                Toast.makeText(context, ex.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
            }
    }

    fun saveNameToFirestore(newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "name" to clean,
                    "email" to userEmail,
                    "updatedAt" to Timestamp.now()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                displayName = clean
                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { ex ->
                Toast.makeText(context, ex.message ?: "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    // Mark the index where "Recent Rides" title appears so My Rides can scroll there
    val ridesSectionIndex = 3

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                            text = if (isProfileLoading) "Loading profile..." else "Hi, $displayName",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Red)
                    }
                }
            }

            item {
                // Actions (functional)
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
                        onClick = {
                            scope.launch { listState.animateScrollToItem(ridesSectionIndex) }
                        }
                    )
                    HomeActionCard(
                        title = "Profile",
                        icon = Icons.Default.Person,
                        onClick = { showProfileDialog = true }
                    )
                }
            }

            item {
                Text(
                    text = "Recent Rides",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF01579B)
                )
            }

            item {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Loading rides...")
                    }
                }
                error?.let { Text(text = it, color = Color.Red, fontSize = 14.sp) }
            }

            if (!isLoading && rides.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No rides yet.")
                            Text(
                                "Tap “Book Ride” to create your first ride (saved in Firestore).",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                items(rides) { ride ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedRide = ride
                                showDeleteRideDialog = true
                            },
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${ride.pickup} → ${ride.drop}", fontSize = 16.sp)
                                Text(
                                    text = "Tap to delete (demo)",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }
        }

        // ✅ Book Ride Dialog
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
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next
                            )
                        )
                        OutlinedTextField(
                            value = drop,
                            onValueChange = { drop = it },
                            label = { Text("Drop location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Saved to Firestore: users/$uid/rides",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pickup.trim().isNotEmpty() && drop.trim().isNotEmpty()) {
                            addRideToFirestore(pickup, drop)
                            pickup = ""
                            drop = ""
                            showBookDialog = false
                        } else {
                            Toast.makeText(context, "Please enter pickup and drop", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showBookDialog = false }) { Text("Cancel") }
                }
            )
        }

        // ✅ Profile Dialog (with Edit Name)
        if (showProfileDialog) {
            AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                title = { Text("Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Name: $displayName")
                        Text("Email: $userEmail")
                        Text("Total rides: ${rides.size}")
                        Text("Auth + Firestore connected ✅", color = Color(0xFF01579B))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showProfileDialog = false
                        showEditNameDialog = true
                    }) { Text("Edit Name") }
                },
                dismissButton = {
                    TextButton(onClick = { showProfileDialog = false }) { Text("Close") }
                }
            )
        }

        // ✅ Edit Name Dialog (writes to users/{uid})
        if (showEditNameDialog) {
            var tempName by remember { mutableStateOf(displayName) }

            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = { Text("Edit Name") },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Your name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        saveNameToFirestore(tempName)
                        showEditNameDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
                }
            )
        }

        // ✅ Delete Ride confirmation
        if (showDeleteRideDialog && selectedRide != null) {
            val ride = selectedRide!!
            AlertDialog(
                onDismissRequest = {
                    showDeleteRideDialog = false
                    selectedRide = null
                },
                title = { Text("Delete Ride?") },
                text = { Text("${ride.pickup} → ${ride.drop}") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteRide(ride)
                        showDeleteRideDialog = false
                        selectedRide = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteRideDialog = false
                        selectedRide = null
                    }) { Text("Cancel") }
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
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
