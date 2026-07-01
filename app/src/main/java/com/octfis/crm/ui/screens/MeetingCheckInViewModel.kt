// app/src/main/java/com/octfis/crm/ui/screens/MeetingCheckInViewModel.kt
package com.octfis.crm.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octfis.crm.data.remote.ZohoServiceLocator
import com.octfis.crm.service.CheckInLocation
import com.octfis.crm.service.MeetingCheckInManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CheckInState {
    object Idle        : CheckInState()
    object GettingGPS  : CheckInState()
    object Saving      : CheckInState()
    data class Success(
        val location  : CheckInLocation,
        val timestamp : String,
    ) : CheckInState()
    data class Error(val message: String) : CheckInState()
}

class MeetingCheckInViewModel : ViewModel() {

    private val api = ZohoServiceLocator.getApiService()

    private val _checkInState = MutableStateFlow<CheckInState>(CheckInState.Idle)
    val checkInState: StateFlow<CheckInState> = _checkInState.asStateFlow()

    private val _isCheckedIn  = MutableStateFlow(false)
    val isCheckedIn: StateFlow<Boolean> = _isCheckedIn.asStateFlow()

    private val _checkedInAt  = MutableStateFlow("")
    val checkedInAt: StateFlow<String> = _checkedInAt.asStateFlow()

    private val _checkedInAddress = MutableStateFlow("")
    val checkedInAddress: StateFlow<String> = _checkedInAddress.asStateFlow()

    // ── Check if already checked in (from meeting record) ────────────────────
    fun loadCheckInStatus(meetingZohoId: String) {
        viewModelScope.launch {
            runCatching {
                val response = api.getEventById(meetingZohoId)
                val record   = response.data?.firstOrNull()
                if (record != null) {
                    // Check if CheckIn_Status field is set
                    // You need to add CheckIn_Status to your EventsResponse DTO
                    val status = record.checkInStatus
                    _isCheckedIn.value      = status == "Checked In"
                    _checkedInAt.value      = record.checkInTime.orEmpty()
                    _checkedInAddress.value = record.checkInAddress.orEmpty()
                }
            }
        }
    }

    // ── Perform check-in ──────────────────────────────────────────────────────
    fun checkIn(context: Context, meetingZohoId: String) {
        viewModelScope.launch {
            _checkInState.value = CheckInState.GettingGPS

            // 1. Get GPS location
            val location = runCatching {
                MeetingCheckInManager.getCurrentLocation(context)
            }.getOrElse {
                _checkInState.value = CheckInState.Error(
                    "Could not get location: ${it.message}"
                )
                return@launch
            }

            _checkInState.value = CheckInState.Saving

            // 2. Save to Zoho CRM
            runCatching {
                api.updateEvent(
                    id   = meetingZohoId,
                    body = mapOf(
                        "data" to listOf(
                            mapOf(
                                "CheckIn_Status"  to "Checked In",
                                "CheckIn_Time"    to location.timestamp,
                                "CheckIn_Lat"     to location.latitude,
                                "CheckIn_Long"    to location.longitude,
                                "CheckIn_Address" to location.address,
                            )
                        )
                    )
                )
            }.fold(
                onSuccess = {
                    _isCheckedIn.value      = true
                    _checkedInAt.value      = location.timestamp
                    _checkedInAddress.value = location.address
                    _checkInState.value     = CheckInState.Success(
                        location  = location,
                        timestamp = location.timestamp,
                    )
                },
                onFailure = {
                    _checkInState.value = CheckInState.Error(
                        "Failed to save check-in: ${it.message}"
                    )
                }
            )
        }
    }

    fun resetState() { _checkInState.value = CheckInState.Idle }
}