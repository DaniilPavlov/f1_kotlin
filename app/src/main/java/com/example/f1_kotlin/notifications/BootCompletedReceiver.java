package com.example.f1_kotlin.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import dagger.hilt.android.EntryPointAccessors;

/**
 * Re-schedules race reminders after boot / timezone change.
 * Verifies {@link Intent#getAction()} so third-party explicit intents are ignored (CodeQL).
 */
public final class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            return;
        }
        EntryPointAccessors.fromApplication(
                context.getApplicationContext(),
                RaceReminderEntryPoint.class
        ).raceReminderScheduler().sync();
    }
}
