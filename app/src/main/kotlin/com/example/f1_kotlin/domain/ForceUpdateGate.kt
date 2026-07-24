package com.example.f1_kotlin.domain

import com.example.f1_kotlin.data.firebase.RemoteConfigService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Гейт принудительного обновления по Remote Config [RemoteConfigService.minAppVersion].
 */
@Singleton
class ForceUpdateGate @Inject constructor(
    private val remoteConfig: RemoteConfigService,
) {
    private val _required = MutableStateFlow(false)
    val required: StateFlow<Boolean> = _required.asStateFlow()

    fun check() {
        _required.value = remoteConfig.isUpdateRequired()
    }

    suspend fun onResume() {
        remoteConfig.refresh()
        check()
    }
}
