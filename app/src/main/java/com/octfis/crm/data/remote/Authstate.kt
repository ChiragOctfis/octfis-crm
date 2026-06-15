// app/src/main/java/com/octfis/crm/data/remote/Authstate.kt
package com.octfis.crm.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthState {
    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess = _loginSuccess.asStateFlow()

    fun onLoginSuccess() { _loginSuccess.value = true }
    fun reset()          { _loginSuccess.value = false }
}