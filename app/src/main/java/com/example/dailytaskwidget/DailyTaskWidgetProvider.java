package com.example.dailytaskwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.List;

/**
 * 桌面小组件：显示当前时段的今日任务，时间到了自动切换。
 *
 * 刷新机制（三重保障）：
 * 1. 每 60 秒定时刷新（到点切换时段）
 * 2. 午夜零点精确闹钟刷新（跨天自动切到新一天）
 * 3. 点击小组件时刷新（手动兜底）
 */
public class DailyTaskWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_UPDATE = "com.example.dailytaskwidget.ACTION_UPDATE";
    private static final String ACTION_CLICK = "com.example.dailytaskwidget.ACTION_CLICK";
    private static final String ACTION_MIDNIGHT = "com.example.dailytaskwidget.ACTION_MIDNIGHT";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
        scheduleNextAlarm(context);
        scheduleMidnightAlarm(context);
    }

    @Override
    public void onEnabled(Context context) {
        scheduleNextAlarm(context);
        scheduleMidnightAlarm(context);
    }

    @Override
    public void onDisabled(Context context) {
        cancelAlarm(context);
        cancelMidnightAlarm(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // 开机后重新注册闹钟
            scheduleNextAlarm(context);
            scheduleMidnightAlarm(context);
        } else if (ACTION_UPDATE.equals(action) || ACTION_MIDNIGHT.equals(action)) {
            refreshAllWidgets(context);
            scheduleNextAlarm(context);
            scheduleMidnightAlarm(context);
        } else if (ACTION_CLICK.equals(action)) {
            // 点击小组件：先刷新，再打开 App
            refreshAllWidgets(context);
            Intent launch = new Intent(context, MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
        }
    }

    /** 刷新所有小组件 */
    private void refreshAllWidgets(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, DailyTaskWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        for (int id : ids) {
            updateWidget(context, mgr, id);
        }
    }

    /** 更新单个小组件的内容 */
    private void updateWidget(Context context, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_daily_task);

        String todayKey = TaskStore.todayKey();
        List<Task> tasks = TaskStore.getTasks(context, todayKey);

        // 日期显示（今天 + 具体日期）
        String dateStr = "今天 · " + formatDate();
        views.setTextViewText(R.id.tv_date, dateStr);

        // 当前时间
        Calendar now = Calendar.getInstance();
        String clockStr = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        views.setTextViewText(R.id.tv_clock, clockStr);

        int idx = TaskStore.findCurrentTaskIndex(tasks);

        if (idx >= 0) {
            // 有当前任务
            Task t = tasks.get(idx);
            views.setTextViewText(R.id.tv_now_label, "现在 · " + t.getStartTimeStr() + " - " + t.getEndTimeStr());
            views.setTextViewText(R.id.tv_task, t.title);
        } else if (!tasks.isEmpty()) {
            // 没有进行中的任务：显示空闲或下一场
            Task next = findNextTask(tasks);
            if (next != null) {
                views.setTextViewText(R.id.tv_now_label, "接下来 · " + next.getStartTimeStr());
                views.setTextViewText(R.id.tv_task, next.title);
            } else {
                views.setTextViewText(R.id.tv_now_label, "今日任务已全部完成");
                views.setTextViewText(R.id.tv_task, "可以休息啦");
            }
        } else {
            // 今天没有任务
            views.setTextViewText(R.id.tv_now_label, "今日暂无安排");
            views.setTextViewText(R.id.tv_task, "打开 App 添加任务");
        }

        // 点击小组件：发广播（刷新 + 打开 App）
        Intent click = new Intent(context, DailyTaskWidgetProvider.class);
        click.setAction(ACTION_CLICK);
        PendingIntent pi = PendingIntent.getBroadcast(context, 1, click,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pi);

        mgr.updateAppWidget(widgetId, views);
    }

    /** 找到当前时间之后最近的下一个任务 */
    private Task findNextTask(List<Task> tasks) {
        Calendar c = Calendar.getInstance();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        for (Task t : tasks) {
            if (t.startMinute > now) {
                return t;
            }
        }
        return null;
    }

    /** 格式化为 "11月24日 周二" */
    private String formatDate() {
        Calendar c = Calendar.getInstance();
        String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int w = c.get(Calendar.DAY_OF_WEEK) - 1;
        return month + "月" + day + "日 " + weeks[w];
    }

    /** 每 60 秒定时触发一次更新，实现到点自动切换时段 */
    private void scheduleNextAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyTaskWidgetProvider.class);
        intent.setAction(ACTION_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAt = SystemClock.elapsedRealtime() + 60 * 1000;
        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        }
    }

    /** 午夜零点设置一个精确闹钟，跨天时自动刷新到新一天 */
    private void scheduleMidnightAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyTaskWidgetProvider.class);
        intent.setAction(ACTION_MIDNIGHT);
        PendingIntent pi = PendingIntent.getBroadcast(context, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 计算下一个午夜的 00:00:30
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 30);
        c.set(Calendar.MILLISECOND, 0);
        long midnightMillis = c.getTimeInMillis();

        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnightMillis, pi);
        }
    }

    private void cancelAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyTaskWidgetProvider.class);
        intent.setAction(ACTION_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) {
            am.cancel(pi);
        }
    }

    private void cancelMidnightAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyTaskWidgetProvider.class);
        intent.setAction(ACTION_MIDNIGHT);
        PendingIntent pi = PendingIntent.getBroadcast(context, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) {
            am.cancel(pi);
        }
    }

    /** 供 MainActivity 在任务保存后立即刷新小组件 */
    public static void refreshAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, DailyTaskWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        Intent update = new Intent(context, DailyTaskWidgetProvider.class);
        update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(update);
    }
}
