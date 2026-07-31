package com.example.f1_kotlin.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.f1_kotlin.data.analytics.AnalyticsEvent
import com.example.f1_kotlin.data.deeplink.F1PetDeepLinks
import com.example.f1_kotlin.data.model.EspnScoreboardEvent
import com.example.f1_kotlin.di.AppEntryPoint
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.ui.share.ShareCareerCard
import com.example.f1_kotlin.ui.share.ShareRaceResultsCard
import com.example.f1_kotlin.ui.share.ShareWeekendSummaryCard
import com.example.f1_kotlin.ui.theme.F1Theme
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Screens register a share action for the app bar; cleared on dispose. */
val LocalShareActionSetter = staticCompositionLocalOf<(((() -> Unit)?) -> Unit)> {
    error("LocalShareActionSetter not provided")
}

/** Registers [onShare] on the app bar while this composition is active. */
@Composable
fun RegisterShareAction(onShare: (() -> Unit)?) {
    val setShare = LocalShareActionSetter.current
    DisposableEffect(onShare) {
        setShare(onShare)
        onDispose { setShare(null) }
    }
}

@Composable
fun rememberShareCareerAction(title: String, races: Int, wins: Int, podiums: Int, poles: Int): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(title, races, wins, podiums, poles) {
        {
            scope.launch {
                ShareHelper.shareComposableAsPng(
                    context = context,
                    fileName = "f1_career_${System.currentTimeMillis()}.png",
                ) {
                    ShareCareerCard(
                        title = title,
                        races = races,
                        wins = wins,
                        podiums = podiums,
                        poles = poles,
                    )
                }
            }
        }
    }
}

@Composable
fun rememberShareRaceAction(race: Race): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(race.season, race.round, race.raceName, race.results) {
        {
            scope.launch {
                ShareHelper.shareComposableAsPng(
                    context = context,
                    fileName = "f1_race_${race.season}_${race.round}.png",
                ) {
                    ShareRaceResultsCard(race = race)
                }
            }
        }
    }
}

@Composable
fun rememberShareCircuitDeepLinkAction(circuitId: String, circuitName: String): () -> Unit {
    val context = LocalContext.current
    return remember(circuitId, circuitName) {
        {
            val uri = F1PetDeepLinks.circuit(circuitId)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$circuitName\n$uri")
            }
            context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            runCatching {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    AppEntryPoint::class.java,
                ).analyticsGateway().log(AnalyticsEvent.ShareTapped("circuit"))
            }
        }
    }
}

@Composable
fun rememberShareWeekendAction(event: EspnScoreboardEvent): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(event) {
        {
            scope.launch {
                ShareHelper.shareComposableAsPng(
                    context = context,
                    fileName = "f1_weekend_${System.currentTimeMillis()}.png",
                ) {
                    ShareWeekendSummaryCard(event = event)
                }
                runCatching {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AppEntryPoint::class.java,
                    ).analyticsGateway().log(AnalyticsEvent.ShareTapped("weekend_summary"))
                }
            }
        }
    }
}

/**
 * Renders [content] off-screen, saves PNG to cache, opens the system share sheet.
 */
object ShareHelper {
    suspend fun shareComposableAsPng(
        context: Context,
        fileName: String,
        content: @Composable () -> Unit,
    ) {
        val activity = context.findActivity() ?: return
        val bitmap = withContext(Dispatchers.Main) {
            captureComposable(activity, content)
        } ?: return
        withContext(Dispatchers.IO) {
            shareBitmap(activity, bitmap, fileName)
        }
    }

    private suspend fun captureComposable(
        activity: Activity,
        content: @Composable () -> Unit,
    ): Bitmap? = suspendCancellableCoroutine { cont ->
        // Off-screen WindowManager panels don't inherit Activity's ViewTree owners —
        // Compose 1.10+ requires them on ComposeView or composition crashes.
        val lifecycleOwner = activity as? LifecycleOwner
        val savedStateOwner = activity as? SavedStateRegistryOwner
        if (lifecycleOwner == null || savedStateOwner == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setContent {
                // Share cards are designed for a fixed light palette.
                F1Theme(darkTheme = false) {
                    content()
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            token = activity.window.decorView.windowToken
            x = -10_000
            y = 0
            title = "f1_share_capture"
        }

        fun cleanup() {
            runCatching {
                if (composeView.parent != null) windowManager.removeView(composeView)
            }
        }

        cont.invokeOnCancellation { cleanup() }

        try {
            windowManager.addView(composeView, params)
        } catch (_: Exception) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        composeView.post {
            composeView.post {
                try {
                    val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    composeView.measure(widthSpec, heightSpec)
                    val width = composeView.measuredWidth.coerceAtLeast(1)
                    val height = composeView.measuredHeight.coerceAtLeast(1)
                    composeView.layout(0, 0, width, height)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    composeView.draw(canvas)
                    cleanup()
                    if (cont.isActive) cont.resume(bitmap)
                } catch (_: Exception) {
                    cleanup()
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
