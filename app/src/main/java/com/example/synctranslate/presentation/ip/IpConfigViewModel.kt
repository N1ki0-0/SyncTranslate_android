package com.example.synctranslate.presentation.ip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.synctranslate.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IpConfigViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _ipAddress = MutableStateFlow(preferencesManager.serverIp ?: "")
    val ipAddress = _ipAddress.asStateFlow()

    private val _port = MutableStateFlow(preferencesManager.serverPort.toString())
    val port = _port.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    fun onIpAddressChanged(newIp: String) { _ipAddress.value = newIp }
    fun onPortChanged(newPort: String) { _port.value = newPort }

    fun saveSettings() {
        val portNumber = _port.value.toIntOrNull()
        if (_ipAddress.value.isNotBlank() && portNumber != null) {
            viewModelScope.launch {
                preferencesManager.serverIp = _ipAddress.value.trim()
                preferencesManager.serverPort = portNumber
                _isSaved.value = true
            }
        }
    }
}