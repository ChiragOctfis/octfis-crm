package com.octfis.crm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.octfis.crm.data.remote.CatalystAuthManager
import com.octfis.crm.data.remote.SessionManager
import com.octfis.crm.data.remote.ZohoConstants
import com.octfis.crm.data.remote.ZohoServiceLocator
import com.octfis.crm.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// Models
// ─────────────────────────────────────────────────────────────────────────────

data class AppUser(
    val userId         : String,
    val name           : String,
    val email          : String,
    val role           : String,
    val userFieldValue : String,
    val isActive       : Boolean,
)

sealed class UserMgmtState {
    object Loading                               : UserMgmtState()
    data class Success(val users: List<AppUser>) : UserMgmtState()
    data class Error(val message: String)        : UserMgmtState()
}

sealed class UserActionState {
    object Idle                           : UserActionState()
    object Working                        : UserActionState()
    object Done                           : UserActionState()
    data class Err(val message: String)   : UserActionState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class UserManagementViewModel : ViewModel() {

    private val baseUrl = ZohoConstants.BASE_URL

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _state       = MutableStateFlow<UserMgmtState>(UserMgmtState.Loading)
    val state: StateFlow<UserMgmtState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<UserActionState>(UserActionState.Idle)
    val actionState: StateFlow<UserActionState> = _actionState.asStateFlow()

    init { loadUsers() }

    // ── Load all users ────────────────────────────────────────────────────────
    fun loadUsers() {
        viewModelScope.launch {
            _state.value = UserMgmtState.Loading
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val request = Request.Builder()
                        .url("$baseUrl/server/appauth/users")
                        .header("x-jwt-token", "Bearer ${token()}")
                        .get()
                        .build()

                    val response = http.newCall(request).execute()
                    val raw      = response.body!!.string()
                    val json     = JSONObject(raw)

                    if (!json.optBoolean("success", false)) {
                        error(json.optString("message", "Failed to load users"))
                    }

                    val arr = json.getJSONArray("users")
                    (0 until arr.length()).map { i ->
                        val u = arr.getJSONObject(i)
                        AppUser(
                            // FIX: match exact field names returned by appauth function
                            userId         = u.optString("userId"),
                            name           = u.optString("name"),
                            email          = u.optString("email"),
                            role           = u.optString("role"),
                            userFieldValue = u.optString("userFieldValue"),
                            isActive       = u.optBoolean("isActive", true),
                        )
                    }
                }
            }
            result.fold(
                onSuccess = { _state.value = UserMgmtState.Success(it) },
                onFailure = { _state.value = UserMgmtState.Error(it.message ?: "Failed to load users") }
            )
        }
    }

    // ── Create user ───────────────────────────────────────────────────────────
    fun createUser(
        name           : String,
        email          : String,
        password       : String,
        role           : String,
        userFieldValue : String,
    ) {
        viewModelScope.launch {
            _actionState.value = UserActionState.Working
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val body = JSONObject().apply {
                        put("name",                  name.trim())
                        put("email",                 email.trim().lowercase())
                        put("password",              password)
                        put("role",                  role)
                        put("zoho_user_field_value", userFieldValue.trim())
                    }.toString()

                    val request = Request.Builder()
                        .url("$baseUrl/server/appauth/register")
                        .header("x-jwt-token", "Bearer ${token()}")
                        .header("Content-Type", "application/json")
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = http.newCall(request).execute()
                    val raw      = response.body!!.string()
                    val json     = JSONObject(raw)

                    if (!json.optBoolean("success", false)) {
                        error(json.optString("message", "Failed to create user"))
                    }
                }
            }
            result.fold(
                onSuccess = { _actionState.value = UserActionState.Done; loadUsers() },
                onFailure = { _actionState.value = UserActionState.Err(it.message ?: "Failed to create user") }
            )
        }
    }

    // ── Toggle active/inactive ────────────────────────────────────────────────
    fun toggleUserActive(userId: String, currentlyActive: Boolean) {
        viewModelScope.launch {
            _actionState.value = UserActionState.Working
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val body = JSONObject().apply {
                        put("is_active", !currentlyActive)
                    }.toString()

                    val request = Request.Builder()
                        .url("$baseUrl/server/appauth/users/$userId")
                        .header("x-jwt-token", "Bearer ${token()}")
                        .header("Content-Type", "application/json")
                        .put(body.toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = http.newCall(request).execute()
                    val raw      = response.body!!.string()
                    val json     = JSONObject(raw)

                    if (!json.optBoolean("success", false)) {
                        error(json.optString("message", "Failed to update user"))
                    }
                }
            }
            result.fold(
                onSuccess = { _actionState.value = UserActionState.Done; loadUsers() },
                onFailure = { _actionState.value = UserActionState.Err(it.message ?: "Failed to update user") }
            )
        }
    }

    fun resetActionState() { _actionState.value = UserActionState.Idle }

    // FIX: suspend function — no runBlocking needed
    private suspend fun token(): String =
        ZohoServiceLocator.getTokenStore().getJwtToken() ?: ""
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    navController : NavController,
    vm            : UserManagementViewModel = viewModel(),
) {
    if (!SessionManager.isAdmin()) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    val state       by vm.state.collectAsState()
    val actionState by vm.actionState.collectAsState()
    var showDialog  by remember { mutableStateOf(false) }
    val snackbar    = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is UserActionState.Done -> { vm.resetActionState(); showDialog = false }
            is UserActionState.Err  -> { snackbar.showSnackbar(s.message); vm.resetActionState() }
            else -> Unit
        }
    }

    if (showDialog) {
        AddUserDialog(
            onDismiss = { showDialog = false },
            onConfirm = { n, e, p, r, f -> vm.createUser(n, e, p, r, f) },
            isSaving  = actionState is UserActionState.Working,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Manage Users", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.loadUsers() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = CrmPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showDialog = true },
                containerColor = CrmPrimary,
            ) {
                Icon(Icons.Default.PersonAdd, "Add User", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        when (val s = state) {
            is UserMgmtState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = CrmPrimary) }

            is UserMgmtState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudOff,
                        null,
                        tint     = CrmError,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.loadUsers() },
                        colors  = ButtonDefaults.buttonColors(containerColor = CrmPrimary),
                    ) { Text("Retry") }
                }
            }

            is UserMgmtState.Success -> LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "${s.users.size} users",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                items(s.users, key = { it.userId }) { user ->
                    UserCard(
                        user     = user,
                        onToggle = { vm.toggleUserActive(user.userId, user.isActive) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UserCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UserCard(user: AppUser, onToggle: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    RoleBadge(user.role)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    user.email,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "CRM field: ${user.userFieldValue}",
                    fontSize = 11.sp,
                    color    = CrmPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (user.isActive) "Active" else "Inactive",
                    fontSize = 11.sp,
                    color    = if (user.isActive) CrmSuccess else CrmError,
                    fontWeight = FontWeight.Medium,
                )
            }
            Switch(
                checked         = user.isActive,
                onCheckedChange = { onToggle() },
                colors          = SwitchDefaults.colors(
                    checkedTrackColor   = CrmPrimary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val (bg, fg) = when (role) {
        "admin"    -> CrmPrimary.copy(alpha = 0.15f) to CrmPrimary
        "manager"  -> CrmWarning.copy(alpha = 0.15f) to CrmWarning
        else       -> CrmSuccess.copy(alpha = 0.15f) to CrmSuccess
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            text       = role.uppercase(),
            fontSize   = 9.sp,
            color      = fg,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AddUserDialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUserDialog(
    onDismiss : () -> Unit,
    onConfirm : (String, String, String, String, String) -> Unit,
    isSaving  : Boolean,
) {
    var name           by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var role           by remember { mutableStateOf("sales_rep") }
    var userFieldValue by remember { mutableStateOf("") }
    var roleExpanded   by remember { mutableStateOf(false) }
    val roles          = listOf("sales_rep", "manager", "admin")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New User", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Full Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it },
                    label         = { Text("Email") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value                = password,
                    onValueChange        = { password = it },
                    label                = { Text("Password") },
                    singleLine           = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier             = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = userFieldValue,
                    onValueChange = { userFieldValue = it },
                    label         = { Text("CRM App_User value") },
                    placeholder   = { Text("e.g. Rahul") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded         = roleExpanded,
                    onExpandedChange = { roleExpanded = it },
                ) {
                    OutlinedTextField(
                        value         = role,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Role") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded         = roleExpanded,
                        onDismissRequest = { roleExpanded = false },
                    ) {
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text    = { Text(r) },
                                onClick = { role = r; roleExpanded = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(name, email, password, role, userFieldValue) },
                enabled  = !isSaving && name.isNotBlank() && email.isNotBlank()
                        && password.isNotBlank() && userFieldValue.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = CrmPrimary),
            ) {
                if (isSaving)
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = Color.White,
                    )
                else
                    Text("Create User", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}