package com.octfis.crm.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octfis.crm.data.model.CrmCall
import com.octfis.crm.data.remote.ZohoServiceLocator
import com.octfis.crm.data.repository.CallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactDetailViewModel : ViewModel() {

    private val repo = CallRepository(ZohoServiceLocator.getApiService())

    private val _calls = MutableStateFlow<List<CrmCall>>(emptyList())
    val calls: StateFlow<List<CrmCall>> = _calls.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadCallsForContact(contactZohoId: String) {
        if (contactZohoId.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            repo.getCallsForContact(contactZohoId)
                .onSuccess { _calls.value = it }
                .onFailure { /* silently ignore — section stays empty */ }
            _loading.value = false
        }
    }
}