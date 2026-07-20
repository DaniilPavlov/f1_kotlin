package com.example.f1_kotlin.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.f1_kotlin.MainActivity;

/**
 * Explicit PendingIntents — destination classes are compile-time literals so CodeQL
 * can prove the Intent is not implicit (unlike {@code Class<?>} parameters).
 */
final class ExplicitPendingIntents {
    private ExplicitPendingIntents() {}

    static PendingIntent raceReminder(
            Context context,
            int id,
            String title,
            String body
    ) {
        Intent intent = new Intent(context, RaceReminderReceiver.class);
        intent.setPackage(context.getPackageName());
        intent.putExtra(RaceReminderScheduler.EXTRA_ID, id);
        intent.putExtra(RaceReminderScheduler.EXTRA_TITLE, title);
        intent.putExtra(RaceReminderScheduler.EXTRA_BODY, body);
        return PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    static PendingIntent openMainActivity(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    static void schedule(Context context, long triggerAt, PendingIntent pending) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) {
            return;
        }
        boolean canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms();
        if (canExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
        }
    }

    static void cancel(Context context, PendingIntent pending) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) {
            manager.cancel(pending);
        }
    }
}
