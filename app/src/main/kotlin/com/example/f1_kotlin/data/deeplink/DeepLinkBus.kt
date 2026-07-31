package com.example.f1_kotlin.data.deeplink

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Holds pending deep-link targets from cold start / [android.app.Activity.onNewIntent].
 * [com.example.f1_kotlin.ui.navigation.F1App] consumes and navigates.
 */
@Singleton
class DeepLinkBus @Inject constructor() {
    private val _targets = MutableSharedFlow<DeepLinkTarget>(extraBufferCapacity = 8)
    val targets: SharedFlow<DeepLinkTarget> = _targets.asSharedFlow()

    fun offer(uri: Uri?) {
        val target = uri?.toDeepLinkTarget() ?: return
        _targets.tryEmit(target)
    }

    fun offer(target: DeepLinkTarget) {
        _targets.tryEmit(target)
    }
}
