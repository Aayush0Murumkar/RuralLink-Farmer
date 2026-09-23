package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiRecommendation
import com.example.services.AgriMatchAiService
import com.example.services.AuthService
import com.example.ui.components.AgriMatchRecommendationCard
import com.example.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgriMatchScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var aiRecommendation by remember { mutableStateOf<AiRecommendation?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBookingSuccessDialog by remember { mutableStateOf(false) }
    
    val aiService = remember { AgriMatchAiService() }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        val authService = AuthService(context)
        val farmer = authService.currentFarmer.firstOrNull()
        
        val farmerName = farmer?.name?.ifBlank { "Farmer" } ?: "Ramesh Patil"
        val farmerLocation = farmer?.address?.ifBlank { "Nashik Rural, Maharashtra" } ?: "Nashik Rural, Maharashtra"

        // Dynamically build input payload
        val promptJson = """
        {
          "farmer_profile": {
            "farmer_id": "${farmer?.id ?: "FARM_102"}",
            "name": "$farmerName",
            "location": "$farmerLocation",
            "current_crop": "Tomatoes",
            "ready_quantity_kg": 1200,
            "min_acceptable_rate_per_kg": 22
          },
          "orders_pool": [
            {
              "order_id": "MOCK_ORD_01",
              "source": "mock",
              "crop": "Tomatoes",
              "quantity_kg": 500,
              "offered_rate_per_kg": 24,
              "buyer_location": "Vashi APMC, Navi Mumbai (160 km)",
              "delivery_window": "Within 48 hours"
            },
            {
              "order_id": "MOCK_ORD_02",
              "source": "mock",
              "crop": "Tomatoes",
              "quantity_kg": 3000,
              "offered_rate_per_kg": 28,
              "buyer_location": "Surat APMC (240 km)",
              "delivery_window": "Within 24 hours"
            },
            {
              "order_id": "MOCK_ORD_03",
              "source": "mock",
              "crop": "Onions",
              "quantity_kg": 1000,
              "offered_rate_per_kg": 18,
              "buyer_location": "Pune Market (210 km)",
              "delivery_window": "Within 72 hours"
            },
            {
              "order_id": "MOCK_ORD_04",
              "source": "mock",
              "crop": "Tomatoes",
              "quantity_kg": 800,
              "offered_rate_per_kg": 23,
              "buyer_location": "Thane Wholesale (140 km)",
              "delivery_window": "Within 36 hours"
            },
            {
              "order_id": "SUPABASE_ORD_05",
              "source": "supabase_live",
              "crop": "Tomatoes",
              "quantity_kg": 1150,
              "offered_rate_per_kg": 29,
              "buyer_location": "Kalyan Direct Hub (120 km)",
              "delivery_window": "Within 24 hours"
            }
          ],
          "available_transporters": [
            {
              "transporter_id": "TR_01",
              "name": "Kisan Express Logistics",
              "vehicle_type": "Tata Ace (1.5 Ton)",
              "capacity": "1500 kg",
              "estimated_cost": "₹2,400",
              "rating": 4.9
            },
            {
              "transporter_id": "TR_02",
              "name": "Sahyadri Freight Movers",
              "vehicle_type": "Mahindra Bolero Maxi Truck",
              "capacity": "1200 kg",
              "estimated_cost": "₹2,100",
              "rating": 4.7
            },
            {
              "transporter_id": "TR_03",
              "name": "GreenLine Cargo",
              "vehicle_type": "Eicher Pro 2049",
              "capacity": "2500 kg",
              "estimated_cost": "₹3,800",
              "rating": 4.4
            }
          ]
        }
        """.trimIndent()

        try {
            val result = aiService.getRecommendation(promptJson)
            aiRecommendation = result
        } catch (e: Exception) {
            errorMessage = e.message ?: "An unexpected error occurred."
        } finally {
            isLoading = false
        }
    }

    if (showBookingSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showBookingSuccessDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = CardBackground,
            icon = { 
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PrimaryGreenContainer, shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle, 
                        contentDescription = null, 
                        tint = PrimaryGreen, 
                        modifier = Modifier.size(32.dp)
                    ) 
                }
            },
            title = { 
                Text(
                    "AgriMatch Order Dispatched", 
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryDarkText
                ) 
            },
            text = { 
                Text(
                    "Your transport booking request for ${aiRecommendation?.cropName ?: "Produce"} has been accepted and dispatched to local verified logistics partners.",
                    fontSize = 14.sp,
                    color = SecondaryText,
                    lineHeight = 20.sp
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBookingSuccessDialog = false
                        onBack()
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Return to Dashboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "AgriMatch AI", 
                            fontWeight = FontWeight.Bold, 
                            color = PrimaryDarkText,
                            fontSize = 18.sp
                        ) 
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = PrimaryGreenContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Curator",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryDarkText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppBackground)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = PrimaryGreen,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "AgriMatch AI is analyzing market orders...", 
                        color = PrimaryDarkText, 
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Evaluating APMC buyer rates, distance, & logistics feasibility",
                        color = SecondaryText,
                        fontSize = 12.5.sp
                    )
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Recommendation Unavailable", fontWeight = FontWeight.Bold, color = ErrorRed, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "", color = SecondaryText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBack, 
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Go Back")
                    }
                }
            } else if (aiRecommendation != null) {
                val rec = aiRecommendation
                if (rec != null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        item {
                            AgriMatchRecommendationCard(
                                recommendation = rec,
                                onTransporterSelected = { /* Transporter choice updated in card */ },
                                onAcceptOrder = { showBookingSuccessDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

