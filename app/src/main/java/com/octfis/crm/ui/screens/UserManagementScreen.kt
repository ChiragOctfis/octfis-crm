// app/src/main/java/com/octfis/crm/ui/screens/UserManagementScreen.kt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.octfis.crm.data.remote.SessionManager
import com.octfis.crm.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class AppUser(
    val userId         : String,
    val name           : String,
    val email          : String,
    val role           : String,
    val userFieldValue : String,
    val isActive       : Boolean,
)

sealed class UserMgmtState {
    object Loading : UserMgmtState()
    data class Success(val users: List<AppUser>) : UserMgmtState()
    data class Error(val message: String) : UserMgmtState()
}

sealed class UserActionState {
    object Idle    : UserActionState()
    object Working : UserActionState()
    object Done    : UserActionState()
    data class Error(val message: String) : UserActionState()
}

class UserManagementViewModel : ViewModel() {

    private val baseUrl =
        "https://crm-mobile-app-927349475.development.catalystserverless.com"
    private val http    = OkHttpClient()

    private val _state       = MutableStateFlow<UserMgmtState>(UserMgmtState.Loading)
    val state: StateFlow<UserMgmtState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<UserActionState>(UserActionState.Idle)
    val actionState: StateFlow<UserActionState> = _actionState.asStateFlow()

    init { loadUsers() }

    fun loadUsers() {
        viewModelScope.launch {
            _state.value = UserMgmtState.Loading
            runCatching {
                val request = Request.Builder()
                    .url("$baseUrl/server/adminapi/users")
                    .header("Authorization", "Bearer ${getToken()}")
                    .get()
                    .build()
                val response = http.newCall(request).execute()
                val json     = JSONObject(response.body!!.string())
                val arr      = json.getJSONArray("users")
                val users    = (0 until arr.length()).map { i ->
                    val u = arr.getJSONObject(i)
                    AppUser(
                        userId         = u.getString("user_id"),
                        name           = u.getString("name"),
                        email          = u.getString("email"),
                        role           = u.getString("role"),
                        userFieldValue = u.getString("zoho_user_field_value"),
                        isActive       = u.getBoolean("is_active"),
                    )
                }
                _state.value = UserMgmtState.Success(users)
            }.onFailure {
                _state.value = UserMgmtState.Error(it.message ?: "Failed to load users")
            }
        }
    }

    fun createUser(
        name           : String,
        email          : String,
        password       : String,
        role           : String,
        userFieldValue : String,
    ) {
        viewModelScope.launch {
            _actionState.value = UserActionState.Working
            runCatching {
                val body = JSONObject().apply {
                    put("name",                   name)
                    put("email",                  email)
                    put("password",               password)
                    put("role",                   role)
                    put("zoho_user_field_value",  userFieldValue)
                }.toString()
                val request = Request.Builder()
                    .url("$baseUrl/server/adminapi/users")
                    .header("Authorization", "Bearer ${getToken()}")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val response = http.newCall(request).execute()
                val json     = JSONObject(response.body!!.string())
                if (!json.getBoolean("success")) error(json.getString("message"))
                _actionState.value = UserActionState.Done
                loadUsers()
            }.onFailure {
                _actionState.value = UserActionState.Error(it.message ?: "Failed to create user")
            }
        }
    }

    fun toggleUserActive(userId: String, currentlyActive: Boolean) {
        viewModelScope.launch {
            _actionState.value = UserActionState.Working
            runCatching {
                val body = JSONObject().apply {
                    put("is_active", !currentlyActive)
                }.toString()
                val request = Request.Builder()
                    .url("$baseUrl/server/adminapi/users/$userId")
                    .header("Authorization", "Bearer ${getToken()}")
                    .put(body.toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(request).execute()
                _actionState.value = UserActionState.Done
                loadUsers()
            }.onFailure {
                _actionState.value = UserActionState.Error(it.message ?: "Failed")
            }
        }
    }

    fun resetActionState() { _actionState.value = UserActionState.Idle }

    private fun getToken(): String =
        com.octfis.crm.data.remote.ZohoServiceLocator.getTokenStore()
            .let { store ->
                kotlinx.coroutines.runBlocking { store.getJwtToken() ?: "" }
            }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    navController : NavController,
    vm            : UserManagementViewModel = viewModel(),
) {
    // Guard — only admin can access
    if (!SessionManager.isAdmin()) {
        navController.popBackStack()
        return
    }

    val state       by vm.state.collectAsState()
    val actionState by vm.actionState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHost  = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is UserActionState.Done  -> { vm.resetActionState(); showAddDialog = false }
            is UserActionState.Error -> {
                snackbarHost.showSnackbar(s.message)
                vm.resetActionState()
            }
            else -> Unit
        }
    }

    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, email, password, role, fieldValue ->
                vm.createUser(name, email, password, role, fieldValue)
            },
            isSaving = actionState is UserActionState.Working,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Users", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
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
                onClick        = { showAddDialog = true },
                containerColor = CrmPrimary,
            ) {
                Icon(Icons.Default.PersonAdd, "Add User", tint = androidx.compose.ui.graphics.Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (val s = state) {
            is UserMgmtState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CrmPrimary)
                }
            }
            is UserMgmtState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.loadUsers() }) { Text("Retry") }
                    }
                }
            }
            is UserMgmtState.Success -> {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
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
                    items(s.users) { user ->
                        UserCard(
                            user           = user,
                            onToggleActive = { vm.toggleUserActive(user.userId, user.isActive) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun UserCard(user: AppUser, onToggleActive: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (user.role) {
                            "admin"   -> CrmPrimary.copy(alpha = 0.15f)
                            "manager" -> CrmWarning.copy(alpha = 0.15f)
                            else      -> CrmSuccess.copy(alpha = 0.15f)
                        },
                    ) {
                        Text(
                            text     = user.role.uppercase(),
                            fontSize = 9.sp,
                            color    = when (user.role) {
                                "admin"   -> CrmPrimary
                                "manager" -> CrmWarning
                                else      -> CrmSuccess
                            },
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(
                    "CRM Field: ${user.userFieldValue}",
                    fontSize = 11.sp,
                    color    = CrmPrimary,
                )
            }
            Switch(
                checked         = user.isActive,
                onCheckedChange = { onToggleActive() },
                colors          = SwitchDefaults.colors(
                    checkedTrackColor = CrmPrimary,
                ),
            )
        }
    }
}

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
                    label         = { Text("CRM App_User Picklist Value") },
                    placeholder   = { Text("e.g. Rahul") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                // Role dropdown
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
                enabled  = !isSaving && name.isNotBlank() && email.isNotBlank() &&
                        password.isNotBlank() && userFieldValue.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = CrmPrimary),
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Create User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}