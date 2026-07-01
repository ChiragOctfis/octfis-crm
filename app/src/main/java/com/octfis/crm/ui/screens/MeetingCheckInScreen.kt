// app/src/main/java/com/octfis/crm/ui/screens/MeetingCheckInScreen.kt
package com.octfis.crm.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.octfis.crm.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MeetingCheckInCard(
    meetingZohoId   : String,
    meetingTitle    : String,
    meetingLocation : String = "",
    vm              : MeetingCheckInViewModel = viewModel(),
) {
    val context      = LocalContext.current
    val checkInState by vm.checkInState.collectAsState()
    val isCheckedIn  by vm.isCheckedIn.collectAsState()
    val checkedInAt  by vm.checkedInAt.collectAsState()
    val checkedInAddress by vm.checkedInAddress.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }

    // Load existing check-in status
    LaunchedEffect(meetingZohoId) {
        vm.loadCheckInStatus(meetingZohoId)
    }

    // Reset error state after showing
    LaunchedEffect(checkInState) {
        if (checkInState is CheckInState.Error) {
            kotlinx.coroutines.delay(3000)
            vm.resetState()
        }
    }

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            vm.checkIn(context, meetingZohoId)
        } else {
            showPermissionDialog = true
        }
    }

    fun initiateCheckIn() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            vm.checkIn(context, meetingZohoId)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon    = { Icon(Icons.Default.LocationOff, null, tint = CrmError) },
            title   = { Text("Location Permission Required") },
            text    = { Text("Location permission is needed to check in to this meeting. Please enable it in app settings.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            },
        )
    }

    // ── Check-in Card ─────────────────────────────────────────────────────────
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isCheckedIn)
                Color(0xFFE8F5E9)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCheckedIn) Color(0xFF43A047)
                            else CrmPrimary
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = if (isCheckedIn)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.LocationOn,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = if (isCheckedIn) "Checked In" else "Check In",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        color      = if (isCheckedIn) Color(0xFF2E7D32)
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text     = meetingTitle,
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Already checked in — show details ─────────────────────────
            if (isCheckedIn) {
                CheckInDetailsCard(
                    checkedInAt      = checkedInAt,
                    checkedInAddress = checkedInAddress,
                )
            }

            // ── Success state ─────────────────────────────────────────────
            if (checkInState is CheckInState.Success) {
                val s = checkInState as CheckInState.Success
                CheckInDetailsCard(
                    checkedInAt      = s.timestamp,
                    checkedInAddress = s.location.address,
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Error state ───────────────────────────────────────────────
            if (checkInState is CheckInState.Error) {
                val e = checkInState as CheckInState.Error
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CrmError.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        null,
                        tint     = CrmError,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(e.message, fontSize = 12.sp, color = CrmError)
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Check In Button ───────────────────────────────────────────
            if (!isCheckedIn) {
                val isLoading = checkInState is CheckInState.GettingGPS ||
                        checkInState is CheckInState.Saving

                Button(
                    onClick  = { initiateCheckIn() },
                    enabled  = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrmPrimary),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color       = Color.White,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text  = if (checkInState is CheckInState.GettingGPS)
                                "Getting location..."
                            else
                                "Saving check-in...",
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint     = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Check In Now",
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text      = "Your GPS location will be recorded",
                    fontSize  = 11.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CheckInDetailsCard(
    checkedInAt      : String,
    checkedInAddress : String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE8F5E9))
            .padding(14.dp),
    ) {
        // Time
        if (checkedInAt.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    null,
                    tint     = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Check-in Time",
                        fontSize = 10.sp,
                        color    = Color(0xFF558B2F),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        formatDisplayTime(checkedInAt),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color(0xFF1B5E20),
                    )
                }
            }
        }

        // Address
        if (checkedInAddress.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Place,
                    null,
                    tint     = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Location",
                        fontSize   = 10.sp,
                        color      = Color(0xFF558B2F),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        checkedInAddress,
                        fontSize = 12.sp,
                        color    = Color(0xFF1B5E20),
                    )
                }
            }
        }
    }
}

private fun formatDisplayTime(raw: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val out = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        out.format(sdf.parse(raw)!!)
    } catch (e: Exception) {
        raw.replace("T", " ").take(16)
    }
}