package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.SupabaseClientConfig
import com.example.services.SupabaseService
import com.example.services.SupabaseService.DiagnosticInsertResult
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseDiagnosticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabaseService = remember { SupabaseService.getInstance(context) }

    val isConfigured = SupabaseClientConfig.isConfigured
    val hasSession = supabaseService.hasValidSession()
    val currentUserId = supabaseService.getCurrentUserId()
    val currentUserEmail = supabaseService.getCurrentUserEmail()

    var isTestingInsert by remember { mutableStateOf(false) }
    var diagnosticResult by remember { mutableStateOf<DiagnosticInsertResult?>(null) }
    var selectedInspectorTab by remember { mutableIntStateOf(0) } // 0: Response, 1: Payload, 2: Headers, 3: SQL Fix

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Supabase Diagnostics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = PrimaryDarkText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = PrimaryGreenContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "PostgREST Inspector",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryDarkText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connection Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Supabase Connection Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PrimaryDarkText
                            )

                            // Initialized Status Badge
                            Surface(
                                color = if (isConfigured) EmeraldGreenContainer else ErrorRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isConfigured) EmeraldGreen else ErrorRed)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isConfigured) "INITIALIZED" else "NOT INITIALIZED",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = if (isConfigured) EmeraldGreen else ErrorRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Session Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppBackground)
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (hasSession) Icons.Default.VerifiedUser else Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (hasSession) PrimaryGreen else SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (hasSession) "Session: Authenticated User" else "Session: Anonymous API Key",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryDarkText
                                    )
                                    Text(
                                        text = if (hasSession) "ID: $currentUserId (${currentUserEmail ?: "Farmer User"})" else "Using default publishable key credentials",
                                        fontSize = 11.5.sp,
                                        color = SecondaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Configuration Details Grid
                        DiagnosticInfoRow("Project Base URL", SupabaseClientConfig.supabaseUrl)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorderSubtle)
                        DiagnosticInfoRow("API Key (Masked)", SupabaseClientConfig.maskedApiKey)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorderSubtle)
                        DiagnosticInfoRow("PostgREST Endpoint", SupabaseClientConfig.restUrl)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorderSubtle)
                        DiagnosticInfoRow("Target Table", "public.transport_requests")
                    }
                }
            }

            // 2. Trigger Test Insert Button Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Live Database Insertion Test",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Triggers an HTTP POST request to ${SupabaseClientConfig.restUrl}/transport_requests with Prefer: return=representation header and captures raw PostgREST outputs.",
                            fontSize = 12.5.sp,
                            color = SecondaryText,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isTestingInsert = true
                                coroutineScope.launch {
                                    diagnosticResult = supabaseService.testTransportRequestInsertDiagnostic()
                                    isTestingInsert = false
                                }
                            },
                            enabled = !isTestingInsert,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("trigger_test_insert_button")
                        ) {
                            if (isTestingInsert) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Executing POST Request...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Trigger Test Insert into transport_requests",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 3. PostgREST Error & Response Inspector Card
            item {
                val res = diagnosticResult
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = when {
                            res == null -> CardBorder
                            res.success -> PrimaryGreen
                            res.isRlsPolicyBlocked -> AccentEarthy
                            else -> ErrorRed
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PostgREST Response Inspector",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PrimaryDarkText
                            )

                            if (res != null) {
                                Surface(
                                    color = when {
                                        res.success -> EmeraldGreenContainer
                                        res.isRlsPolicyBlocked -> AccentEarthyContainer
                                        else -> ErrorRed.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = when {
                                            res.success -> "HTTP 201 SUCCESS"
                                            res.isRlsPolicyBlocked -> "RLS RESTRICTED (HTTP 201)"
                                            res.httpStatusCode != null -> "HTTP ${res.httpStatusCode}"
                                            else -> "CLIENT ERROR"
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = when {
                                            res.success -> EmeraldGreen
                                            res.isRlsPolicyBlocked -> OnAccentEarthy
                                            else -> ErrorRed
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (res == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppBackground)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = SecondaryText,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No request logged yet",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SecondaryText
                                    )
                                    Text(
                                        text = "Click 'Trigger Test Insert' above to log PostgREST status and errors",
                                        fontSize = 11.5.sp,
                                        color = SecondaryText
                                    )
                                }
                            }
                        } else {
                            // Result Message Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            res.success -> EmeraldGreenContainer.copy(alpha = 0.5f)
                                            res.isRlsPolicyBlocked -> AccentEarthyContainer.copy(alpha = 0.5f)
                                            else -> ErrorRed.copy(alpha = 0.08f)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            res.success -> EmeraldGreen
                                            res.isRlsPolicyBlocked -> AccentEarthy
                                            else -> ErrorRed
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when {
                                                res.success -> Icons.Default.CheckCircle
                                                res.isRlsPolicyBlocked -> Icons.Default.Security
                                                else -> Icons.Default.Error
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                res.success -> EmeraldGreen
                                                res.isRlsPolicyBlocked -> AccentEarthyDark
                                                else -> ErrorRed
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = res.serverMessage,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDarkText
                                        )
                                    }

                                    if (!res.errorCode.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "PostgREST Error Code: ${res.errorCode} | Latency: ${res.latencyMs}ms",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SecondaryText
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Tab Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InspectorTabButton("Response Body", 0, selectedInspectorTab) { selectedInspectorTab = 0 }
                                InspectorTabButton("Payload JSON", 1, selectedInspectorTab) { selectedInspectorTab = 1 }
                                InspectorTabButton("HTTP Headers", 2, selectedInspectorTab) { selectedInspectorTab = 2 }
                                InspectorTabButton("SQL Fix", 3, selectedInspectorTab) { selectedInspectorTab = 3 }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Tab Content Block
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (selectedInspectorTab) {
                                                0 -> "RAW POSTGREST RESPONSE"
                                                1 -> "CLIENT PAYLOAD POSTED"
                                                2 -> "DISPATCH HEADERS"
                                                else -> "RECOMMENDED RLS SQL FIX"
                                            },
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFAAAAAA),
                                            fontFamily = FontFamily.Monospace
                                        )

                                        IconButton(
                                            onClick = {
                                                val contentToCopy = when (selectedInspectorTab) {
                                                    0 -> res.rawResponseBody
                                                    1 -> res.requestPayloadJson
                                                    2 -> res.headersUsed.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                                                    else -> res.rlsFixSql
                                                }
                                                copyToClipboard(contentToCopy, "Diagnostic Details")
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val displayText = when (selectedInspectorTab) {
                                        0 -> res.rawResponseBody.ifBlank { "[] (Empty Body returned)" }
                                        1 -> res.requestPayloadJson
                                        2 -> res.headersUsed.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                                        else -> res.rlsFixSql
                                    }

                                    Text(
                                        text = displayText,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = when {
                                            selectedInspectorTab == 3 -> Color(0xFF81C784)
                                            res.success -> Color(0xFFE0E0E0)
                                            else -> Color(0xFFFF8A80)
                                        },
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Row Level Security (RLS) Debugging Checklist Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PostgREST & Supabase RLS Troubleshooting",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = PrimaryDarkText
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TroubleshootingStep(
                            number = "1",
                            title = "HTTP 201 with empty body []",
                            desc = "PostgreSQL accepted the INSERT, but table Row Level Security (RLS) has no SELECT policy allowed for this session."
                        )

                        TroubleshootingStep(
                            number = "2",
                            title = "HTTP 403 Forbidden / PGRST301",
                            desc = "RLS policy restricts INSERT on public.transport_requests for the anon/authenticated role. Execute the SQL script above."
                        )

                        TroubleshootingStep(
                            number = "3",
                            title = "HTTP 409 / Foreign Key Conflict",
                            desc = "The referenced farmer_id UUID does not exist in public.farmers table. Ensure profile syncing is triggered prior to booking."
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val sql = """
ALTER TABLE public.transport_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow authenticated inserts" ON public.transport_requests FOR INSERT TO authenticated, anon WITH CHECK (true);
CREATE POLICY "Allow authenticated reads" ON public.transport_requests FOR SELECT TO authenticated, anon USING (true);
                                """.trimIndent()
                                copyToClipboard(sql, "Supabase RLS Fix SQL Script")
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreenContainer, contentColor = PrimaryGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Full Supabase RLS Fix SQL Script", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = SecondaryText,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            color = PrimaryDarkText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RowScope.InspectorTabButton(
    label: String,
    index: Int,
    selectedIndex: Int,
    onClick: () -> Unit
) {
    val isSelected = index == selectedIndex
    Surface(
        color = if (isSelected) PrimaryGreen else AppBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else SecondaryText
            )
        }
    }
}

@Composable
private fun TroubleshootingStep(
    number: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(PrimaryGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDarkText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.5.sp,
                color = SecondaryText,
                lineHeight = 16.sp
            )
        }
    }
}
