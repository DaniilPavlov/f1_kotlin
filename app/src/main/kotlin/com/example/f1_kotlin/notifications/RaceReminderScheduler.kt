package com.example.f1_kotlin.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.RaceDateModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.repository.F1Repository
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.util.DateUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Локальные напоминания за 30 минут до сессий (как RaceReminderService во Flutter).
 *
 * Пересобирает alarms при смене расписания, часового пояса или языка приложения.
 */
@Singleton
class RaceReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: F1Repository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sync() {
        scope.launch {
            runCatching {
                val races = repository.getCurrentSchedule().getOrNull() ?: return@runCatching
                val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                val language = LocaleController.language.value
                val localizedContext = context.withAppLocale(language)
                val reminders = sessions(races, localizedContext)
                val timezone = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds / 60
                val plan = reminders.joinToString("|") { "${it.id}:${it.triggerAt}" }
                if (preferences.getString(KEY_PLAN, null) != plan ||
                    preferences.getInt(KEY_TIMEZONE, Int.MIN_VALUE) != timezone ||
                    preferences.getString(KEY_LOCALE, null) != language
                ) {
                    cancelPlanned(preferences.getString(KEY_IDS, "").orEmpty())
                    schedule(reminders)
                    preferences.edit()
                        .putString(KEY_PLAN, plan)
                        .putString(KEY_IDS, reminders.joinToString(",") { it.id.toString() })
                        .putInt(KEY_TIMEZONE, timezone)
                        .putString(KEY_LOCALE, language)
                        .apply()
                }
            }
        }
    }

    private fun sessions(races: List<RaceModel>, localizedContext: Context): List<Reminder> = buildList {
        races.forEach { race ->
            listOf(
                Triple("fp1", R.string.first_practice, race.firstPractice),
                Triple("fp2", R.string.second_practice, race.secondPractice),
                Triple("fp3", R.string.third_practice, race.thirdPractice),
                Triple("sprint_qualifying", R.string.sprint_qualifying, race.sprintQualifying),
                Triple("sprint", R.string.sprint, race.sprint),
                Triple("qualifying", R.string.qualifying, race.qualifying),
                Triple("race", R.string.race, RaceDateModel(race.date, race.time)),
            ).forEach { (key, titleRes, date) ->
                val session = date ?: return@forEach
                val local = DateUtils.toLocalDateTime(session.date, session.time) ?: return@forEach
                val trigger = local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - THIRTY_MINUTES
                if (trigger > System.currentTimeMillis()) {
                    add(
                        Reminder(
                            id = id(race.season, race.round, key),
                            triggerAt = trigger,
                            title = localizedContext.getString(titleRes),
                            body = "${race.raceName} · ${DateUtils.formatHourMinute(local)}",
                        ),
                    )
                }
            }
        }
    }

    private fun schedule(reminders: List<Reminder>) {
        createChannel()
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        reminders.forEach { reminder ->
            val pending = intent(reminder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending)
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending)
            }
        }
    }

    private fun cancelPlanned(ids: String) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        ids.split(",").mapNotNull(String::toIntOrNull).forEach { id ->
            manager.cancel(intent(Reminder(id, 0, "", "")))
        }
    }

    private fun intent(reminder: Reminder): PendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id,
        Intent(context, RaceReminderReceiver::class.java)
            .putExtra(EXTRA_ID, reminder.id)
            .putExtra(EXTRA_TITLE, reminder.title)
            .putExtra(EXTRA_BODY, reminder.body),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val language = LocaleController.language.value
            val localizedContext = context.withAppLocale(language)
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    localizedContext.getString(R.string.notification_channel_race_reminders),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
    }

    private fun id(season: String, round: String, type: String) = "$season:$round:$type".hashCode() and 0x7fffffff

    private data class Reminder(
        val id: Int,
        val triggerAt: Long,
        val title: String,
        val body: String,
    )

    companion object {
        const val CHANNEL_ID = "race_reminders"
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        private const val PREFERENCES = "race_reminders"
        private const val KEY_PLAN = "plan"
        private const val KEY_IDS = "planned_ids"
        private const val KEY_TIMEZONE = "timezone_offset_minutes"
        private const val KEY_LOCALE = "locale"
        private const val THIRTY_MINUTES = 30 * 60 * 1000L
    }
}

class RaceReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = intent.getStringExtra(RaceReminderScheduler.EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(RaceReminderScheduler.EXTRA_BODY).orEmpty()
        val notification = NotificationCompat.Builder(context, RaceReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify(intent.getIntExtra(RaceReminderScheduler.EXTRA_ID, 0), notification)
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            RaceReminderEntryPoint::class.java,
        ).raceReminderScheduler().sync()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RaceReminderEntryPoint {
    fun raceReminderScheduler(): RaceReminderScheduler
}

private fun Context.withAppLocale(language: String): Context {
    val locale = Locale.forLanguageTag(language)
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList(locale))
    val localized = createConfigurationContext(config)
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = localized.resources
        override fun getAssets(): AssetManager = localized.assets
    }
}
