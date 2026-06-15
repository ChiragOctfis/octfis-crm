// app/src/main/java/com/octfis/crm/data/remote/SessionManager.kt
package com.octfis.crm.data.remote

data class UserSession(
    val name           : String,
    val email          : String,
    val role           : String,
    val userFieldValue : String,
) {
    val isAdmin : Boolean get() = role == "admin"
}

object SessionManager {
    var currentUser: UserSession? = null

    fun isAdmin()          = currentUser?.isAdmin == true
    fun userFieldValue()   = currentUser?.userFieldValue ?: ""
    fun userName()         = currentUser?.name ?: ""
    fun clear()            { currentUser = null }
}