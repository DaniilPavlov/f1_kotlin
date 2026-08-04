package com.example.f1_kotlin.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kotlin.F1Application
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.analytics.AnalyticsEvent
import com.example.f1_kotlin.di.AppEntryPoint
import com.example.f1_kotlin.domain.AppThemePreference
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.domain.ThemeController
import com.example.f1_kotlin.domain.auth.AuthUser
import com.example.f1_kotlin.domain.auth.authErrorStringRes
import com.example.f1_kotlin.ui.components.BlackButton
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.viewmodel.ProfileUiState
import com.example.f1_kotlin.viewmodel.ProfileViewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSignIn: () -> Unit,
) {
    val user by viewModel.user.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    ProfileScreenContent(
        user = user,
        uiState = uiState,
        raceRemindersEnabled = viewModel.effectivelyEnabled,
        practiceRemindersEnabled = viewModel.practiceEffectivelyEnabled,
        canTogglePractice = viewModel.canTogglePractice,
        onClearToast = viewModel::clearToast,
        onSignIn = onSignIn,
        onSignOut = viewModel::signOut,
        onResendVerification = viewModel::resendVerification,
        onRefreshVerification = viewModel::refreshVerification,
        onRaceRemindersChange = viewModel::setRaceRemindersEnabled,
        onPracticeRemindersChange = viewModel::setPracticeRemindersEnabled,
        onLocaleChanged = viewModel::onLocaleChanged,
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
fun ProfileScreenContent(
    user: AuthUser?,
    uiState: ProfileUiState,
    raceRemindersEnabled: Boolean,
    practiceRemindersEnabled: Boolean,
    canTogglePractice: Boolean,
    onClearToast: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onRaceRemindersChange: (Boolean) -> Unit,
    onPracticeRemindersChange: (Boolean) -> Unit,
    onLocaleChanged: () -> Unit,
) {
    val colors = appColors()
    val context = LocalContext.current
    val language by LocaleController.language.collectAsState()
    val themePreference by ThemeController.preference.collectAsState()

    LaunchedEffect(uiState.toastMessageKey) {
        val key = uiState.toastMessageKey ?: return@LaunchedEffect
        val message = when (key) {
            ProfileViewModel.TOAST_VERIFICATION_SENT -> context.getString(R.string.profile_verification_sent)
            ProfileViewModel.TOAST_EMAIL_VERIFIED -> context.getString(R.string.profile_email_verified)
            ProfileViewModel.TOAST_STILL_NOT_VERIFIED -> context.getString(R.string.profile_still_not_verified)
            else -> {
                val res = authErrorStringRes(key)
                if (res != 0) context.getString(res) else context.getString(R.string.auth_error_generic)
            }
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        onClearToast()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppDimens.horizontalPadding.dp,
                vertical = 16.dp,
            ),
    ) {
        Text(
            text = stringResource(R.string.profile_account_section),
            style = AppStyles.h3.copy(fontSize = 18.sp, lineHeight = 22.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = user?.email?.let { stringResource(R.string.profile_signed_in_as, it) }
                ?: stringResource(R.string.profile_not_signed_in),
            style = AppStyles.body,
        )
        if (user != null && !user.emailVerified) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.profile_email_not_verified),
                style = AppStyles.caption.copy(color = colors.textGray),
            )
            Spacer(Modifier.height(12.dp))
            BlackButton(
                text = stringResource(R.string.profile_resend_verification),
                onClick = onResendVerification,
                enabled = !uiState.isBusy,
            )
            Spacer(Modifier.height(12.dp))
            BlackButton(
                text = stringResource(R.string.profile_refresh_verification),
                onClick = onRefreshVerification,
                enabled = !uiState.isBusy,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (user == null) {
            BlackButton(
                text = stringResource(R.string.profile_sign_in),
                onClick = onSignIn,
            )
        } else {
            BlackButton(
                text = stringResource(R.string.profile_sign_out),
                onClick = onSignOut,
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.profile_appearance_section),
            style = AppStyles.h3.copy(fontSize = 18.sp, lineHeight = 22.sp),
        )
        Spacer(Modifier.height(8.dp))
        ProfileSettingsRow(
            title = stringResource(R.string.profile_theme),
            trailing = {
                Icon(
                    imageVector = when (themePreference) {
                        AppThemePreference.System -> Icons.Filled.BrightnessAuto
                        AppThemePreference.Light -> Icons.Filled.LightMode
                        AppThemePreference.Dark -> Icons.Filled.DarkMode
                    },
                    contentDescription = null,
                    tint = F1Red,
                )
            },
            onClick = {
                val next = ThemeController.cycle(context)
                runCatching {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AppEntryPoint::class.java,
                    ).analyticsGateway().log(
                        AnalyticsEvent.ThemeChanged(
                            when (next) {
                                AppThemePreference.System -> "system"
                                AppThemePreference.Light -> "light"
                                AppThemePreference.Dark -> "dark"
                            },
                        ),
                    )
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        ProfileSettingsRow(
            title = stringResource(R.string.profile_language),
            trailing = {
                Text(
                    text = language.uppercase(),
                    style = AppStyles.body.copy(color = F1Red),
                )
            },
            onClick = {
                val app = context.applicationContext as? F1Application
                val next = app?.toggleLocale() ?: LocaleController.toggle(context)
                onLocaleChanged()
                runCatching {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AppEntryPoint::class.java,
                    ).analyticsGateway().log(AnalyticsEvent.LocaleChanged(next))
                }
            },
        )

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.profile_notifications_section),
            style = AppStyles.h3.copy(fontSize = 18.sp, lineHeight = 22.sp),
        )
        Spacer(Modifier.height(8.dp))
        ProfileSwitchRow(
            title = stringResource(R.string.profile_race_reminders),
            subtitle = stringResource(R.string.profile_race_reminders_subtitle),
            checked = raceRemindersEnabled,
            enabled = true,
            onCheckedChange = onRaceRemindersChange,
        )
        ProfileSwitchRow(
            title = stringResource(R.string.profile_practice_reminders),
            subtitle = stringResource(R.string.profile_practice_reminders_subtitle),
            checked = practiceRemindersEnabled,
            enabled = canTogglePractice,
            onCheckedChange = onPracticeRemindersChange,
        )
    }
}

@Composable
private fun ProfileSettingsRow(
    title: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = AppStyles.body, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ProfileSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = appColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppStyles.body.copy(
                    color = if (enabled) colors.black else colors.textGray,
                ),
            )
            Text(
                text = subtitle,
                style = AppStyles.caption.copy(color = colors.textGray),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = F1Red),
        )
    }
}
