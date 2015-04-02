package com.vngrs.android.pomodoro.shared;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.NotificationManagerCompat;

import com.vngrs.android.pomodoro.shared.model.ActivityType;

import javax.inject.Inject;


public abstract class BaseNotificationReceiver extends BroadcastReceiver {

    public static final String EXTRA_ACTIVITY_TYPE = "com.vngrs.android.pomodoro.extra.ACTIVITY_TYPE";

    public static final String ACTION_START = "com.vngrs.android.pomodoro.action.START";
    public static final String ACTION_STOP = "com.vngrs.android.pomodoro.action.STOP";
    //    public static final String ACTION_PAUSE = "com.vngrs.android.pomodoro.action.PAUSE";
//    public static final String ACTION_RESUME = "com.vngrs.android.pomodoro.action.RESUME";
//    public static final String ACTION_RESET = "com.vngrs.android.pomodoro.action.RESET";
    public static final String ACTION_UPDATE = "com.vngrs.android.pomodoro.action.UPDATE";
    public static final String ACTION_FINISH_ALARM = "com.vngrs.android.pomodoro.action.ALARM";

    public static final Intent START_INTENT = new Intent(ACTION_START);
    public static final Intent STOP_INTENT = new Intent(ACTION_STOP);
    //    public static final Intent PAUSE_INTENT = new Intent(ACTION_PAUSE);
//    public static final Intent RESUME_INTENT = new Intent(ACTION_RESUME);
//    public static final Intent RESET_INTENT = new Intent(ACTION_RESET);
    public static final Intent UPDATE_INTENT = new Intent(ACTION_UPDATE);
    public static final Intent FINISH_ALARM_INTENT = new Intent(ACTION_FINISH_ALARM);

    private static final int REQUEST_UPDATE = 1;
    private static final int REQUEST_FINISH = 2;

    protected static final int NOTIFICATION_ID = 1;

    @Inject PomodoroMaster pomodoroMaster;

    @Inject PowerManager powerManager;
    @Inject NotificationManagerCompat notificationManager;
    @Inject AlarmManager alarmManager;


    @Override
    public void onReceive(Context context, Intent intent) {
        pomodoroMaster.check();

        if (intent != null) {
            switch (intent.getAction()) {
                case ACTION_STOP:
                    stop(context);
                    break;
                case ACTION_FINISH_ALARM:
                    finishAlarm(context);
                    break;
                case ACTION_START:
                    final ActivityType activityType =
                            ActivityType.fromValue(intent.getIntExtra(EXTRA_ACTIVITY_TYPE, 0));
                    start(context, activityType);
                    break;
                default:
                    break;
            }
        }
        updateNotification(context, pomodoroMaster);
    }

    public abstract void updateNotification(Context context,
                                            PomodoroMaster pomodoroMaster);

    private void start(Context context, ActivityType activityType) {
        if (activityType != ActivityType.NONE) {
            pomodoroMaster.start(activityType);

            setAlarm(context, REQUEST_FINISH, FINISH_ALARM_INTENT,
                    pomodoroMaster.getNextPomodoro().getMillis());
            setRepeatingAlarm(context, REQUEST_UPDATE, UPDATE_INTENT);
        }
    }

    private ActivityType stop(Context context) {
        notificationManager.cancel(NOTIFICATION_ID);

        cancelAlarm(context, REQUEST_FINISH, FINISH_ALARM_INTENT);
        cancelAlarm(context, REQUEST_UPDATE, UPDATE_INTENT);

        return pomodoroMaster.stop();
    }

    private void finishAlarm(Context context) {
        ActivityType justStoppedActivityType = stop(context);
        final ActivityType nextActivityType;
        if (justStoppedActivityType.isPomodoro()) {
            if ((pomodoroMaster.getPomodorosDone() + 1) % Constants.POMODORO_NUMBER_FOR_LONG_BREAK == 0) {
                nextActivityType = ActivityType.LONG_BREAK;
            } else {
                nextActivityType = ActivityType.SHORT_BREAK;
            }
        } else {
            nextActivityType = ActivityType.POMODORO;
        }
        pomodoroMaster.setActivityType(nextActivityType);
    }


    private void setRepeatingAlarm(Context context, int requestCode, Intent intent) {
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pendingIntent);
    }

    private boolean isAlarmSet(Context context, int requestCode, Intent intent) {
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE) != null;
    }

    private void setAlarm(Context context, int requestCode, Intent intent, long time) {
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }

    private void cancelAlarm(Context context, int requestCode, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

//    private void startDisplayService() {
//        startService(new Intent(this, PomodoroNotificationService.class));
//    }
//
//    private void stopDisplayService() {
//        stopService(new Intent(this, PomodoroNotificationService.class));
//    }

    @SuppressWarnings("deprecation")
    private boolean isScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return powerManager.isInteractive();
        } else {
            return powerManager.isScreenOn();
        }
    }
}