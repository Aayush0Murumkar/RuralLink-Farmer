package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Farmer
import com.example.services.AuthResult
import com.example.ui.components.RuralLinkButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    farmer: Farmer?,
    onUpdateProfile: (suspend (name: String, address: String, mobileNumber: String, email: String) -> AuthResult)? = null,
    onEditProfileClick: (() -> Unit)? = null,
    onNavigateToDiagnostics: (() -> Unit)? = null,
    onLogoutClick: () -> Unit,
    onResetDemoClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<com.example.services.SupabaseService.ConnectionTestResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    if (showEditDialog) {
        EditProfileDialog(
            farmer = farmer,
            onDismiss = {
                showEditDialog = false
                errorMessage = null
            },
            onSave = { name, address, mobile, email ->
                if (onUpdateProfile != null) {
                    isSaving = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = onUpdateProfile(name, address, mobile, email)
                        isSaving = false
                        if (result is AuthResult.Success) {
                            showEditDialog = false
                            successMessage = "Profile updated successfully!"
                        } else if (result is AuthResult.Error) {
                            errorMessage = result.message
                        }
                    }
                } else if (onEditProfileClick != null) {
                    showEditDialog = false
                    onEditProfileClick()
                }
            },
            errorMessage = errorMessage,
            isSaving = isSaving
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Profile",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkText,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = PrimaryDarkText
                ),
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EmeraldGreen)
                    }
                }
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success Message Banner
            if (!successMessage.isNullOrBlank()) {
                Surface(
                    color = BrightLeafLight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successMessage ?: "",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { successMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Profile Header Box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Farmer Avatar",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = farmer?.name ?: "Farmer Name",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkText
                )
                
                if (farmer?.mobileVerified == true) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified Farmer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = EmeraldGreen
                        )
                    }
                }
            }

            // Details Card (Soft layout)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CardBackground,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ProfileItemRow(
                        icon = Icons.Default.Phone,
                        label = "Mobile Number",
                        value = "+91 ${farmer?.mobileNumber ?: "Not Provided"}"
                    )
                    ProfileItemRow(
                        icon = Icons.Default.HomeWork,
                        label = "Farm Address & Location",
                        value = farmer?.address ?: "Not Provided"
                    )
                    ProfileItemRow(
                        icon = Icons.Default.Lock,
                        label = "Masked Aadhaar",
                        value = farmer?.aadhaarMasked ?: "XXXX XXXX 1234"
                    )
                    ProfileItemRow(
                        icon = Icons.Default.Email,
                        label = "Email Address",
                        value = farmer?.email.takeIf { !it.isNullByBlank() } ?: "Not provided"
                    )
                }
            }

            // Logout & Reset Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryDarkText
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
                
                TextButton(
                    onClick = onResetDemoClick,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Demo Data", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            com.example.ui.components.SupabaseSyncFooter(
                statusText = "Secured & Linked to Supabase Auth"
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    farmer: Farmer?,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, mobile: String, email: String) -> Unit,
    errorMessage: String?,
    isSaving: Boolean
) {
    var name by remember(farmer) { mutableStateOf(farmer?.name ?: "") }
    var address by remember(farmer) { mutableStateOf(farmer?.address ?: "") }
    var mobile by remember(farmer) { mutableStateOf(farmer?.mobileNumber ?: "") }
    var email by remember(farmer) { mutableStateOf(farmer?.email ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryDarkText)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Full Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Farm Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Farm Address") },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_address_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Mobile Number
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) mobile = it },
                    label = { Text("Mobile Number") },
                    prefix = { Text("+91 ") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_mobile_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Email Address
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, address, mobile, email) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        },
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ProfileItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(EmeraldGreen.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = SecondaryText,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryDarkText,
                lineHeight = 20.sp
            )
        }
    }
}

private fun String?.isNullByBlank(): Boolean = this == null || this.isBlank()
