package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransportRequest
import com.example.services.AiService
import com.example.ui.components.RequestCard
import com.example.ui.components.RuralLinkButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsListScreen(
    requests: List<TransportRequest>,
    onRequestClick: (String) -> Unit,
    onNewBookingClick: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    var selectedFilterTab by remember { mutableStateOf("All") }

    val filterTabs = listOf("All", "Searching", "Responses Received", "Transporter Selected")

    val filteredRequests = remember(requests, selectedFilterTab) {
        if (selectedFilterTab == "All") requests
        else requests.filter { req ->
            when (selectedFilterTab) {
                "Searching" -> req.status == "SEARCHING" || req.status == "Pending"
                "Responses Received" -> req.status == "RESPONSES_RECEIVED" || req.status == "Transporters Responded"
                "Transporter Selected" -> req.status == "TRANSPORTER_SELECTED" || req.status == "Matched"
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Transport Requests",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("requests_list_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Requests",
                            tint = EmeraldGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground,
                    titleContentColor = EmeraldGreen,
                    actionIconContentColor = EmeraldGreen
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewBookingClick,
                containerColor = EmeraldGreen,
                contentColor = BrightLeaf,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("requests_list_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Booking")
            }
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
        ) {
            // Filter Tabs with White background and Bright Leaf indicator line
            ScrollableTabRow(
                selectedTabIndex = filterTabs.indexOf(selectedFilterTab).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = CardBackground,
                contentColor = EmeraldGreen,
                indicator = { tabPositions ->
                    val index = filterTabs.indexOf(selectedFilterTab).coerceAtLeast(0)
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = EmeraldGreen,
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = CardBorder)
                }
            ) {
                filterTabs.forEach { tabTitle ->
                    val isSelected = selectedFilterTab == tabTitle
                    Tab(
                        selected = isSelected,
                        onClick = { selectedFilterTab = tabTitle },
                        text = {
                            Text(
                                text = tabTitle,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = if (isSelected) EmeraldGreen else SecondaryText
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (requests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "You haven't created a transport request yet.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create your first request to get instant quotes from verified transporters.",
                            fontSize = 12.5.sp,
                            color = SecondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        RuralLinkButton(
                            text = "Book Transport",
                            onClick = onNewBookingClick,
                            icon = Icons.Default.Add,
                            testTag = "requests_empty_create_button"
                        )
                    }
                }
            } else if (filteredRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No requests found for '$selectedFilterTab'",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        RuralLinkButton(
                            text = "Book Transport",
                            onClick = onNewBookingClick,
                            icon = Icons.Default.Add,
                            testTag = "requests_filtered_empty_button"
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRequests) { req ->
                        RequestCard(
                            request = req,
                            onClick = { onRequestClick(req.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        com.example.ui.components.SupabaseSyncFooter(
                            statusText = "Synchronized with Supabase public.transport_requests"
                        )
                    }
                }
            }
        }
    }
}
