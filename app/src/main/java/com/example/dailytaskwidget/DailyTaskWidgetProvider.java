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
 * 桌面小组件：显示当前时段的今日任务，时间到了自动切换到下一时段。
 *
 * 刷新机制：
 * 1. 系统定时 updatePeriodMillis（30分钟兜底）
 * 2. 通过 AlarmManager 每分钟精准触发一次更新（实现"到点自动跳转"）
 */
public class DailyTaskWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_UPDATE = "com.example.dailytaskwidget.ACTION_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
        // 启动定时刷新，保证到点切换
        scheduleNextAlarm(context);
    }

    @Override
    public void onEnabled(Context context) {
        // 首次添加小组件时启动定时刷新
        scheduleNextAlarm(context);
    }

    @Override
    public void onDisabled(Context context) {
        // 最后一个小组件被移除时取消定时刷新
        cancelAlarm(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, DailyTaskWidgetProvider.class);
            int[] ids = mgr.getAppWidgetIds(cn);
            for (int id : ids) {
                updateWidget(context, mgr, id);
            }
            // 安排下一次刷新
            scheduleNextAlarm(context);
        }
    }

    /** 更新单个小组件的内容 */
    private void updateWidget(Context context, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_daily_task);

        String todayKey = TaskStore.todayKey();
        List<Task> tasks = TaskStore.getTasks(context, todayKey);

        // 日期显示
        String dateStr = formatDate();
        views.setTextViewText(R.id.tv_date, dateStr);

        // 当前时间
        Calendar now = Calendar.getInstance();
        String clockStr = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        views.setTextViewText(R.id.tv_clock, clockStr);

        int idx = TaskStore.findCurrentTaskIndex(tasks);

        if (idx >= 0) {
            // 有当前任务：大字显示当前，小字显示下一个
            Task current = tasks.get(idx);
            views.setTextViewText(R.id.tv_now_label, "现在 · " + current.getStartTimeStr() + " - " + current.getEndTimeStr());
            views.setTextViewText(R.id.tv_task, current.title);
            views.setInt(R.id.tv_task, "setTextColor", 0xFF1A1C2E);

            // 下一个任务（idx+1）
            if (idx + 1 < tasks.size()) {
                Task next = tasks.get(idx + 1);
                views.setTextViewText(R.id.tv_next_label, "接下来 · " + next.getStartTimeStr() + " - " + next.getEndTimeStr());
                views.setTextViewText(R.id.tv_next_task, next.title);
            } else {
                views.setTextViewText(R.id.tv_next_label, "接下来");
                views.setTextViewText(R.id.tv_next_task, "这是今天最后一项啦");
            }

        } else if (!tasks.isEmpty()) {
            // 空闲/间隙：当前显示"空闲"，小字显示下一个任务
            Task next = findNextTask(tasks);
            if (next != null) {
                views.setTextViewText(R.id.tv_now_label, "现在 · 空闲时间");
                views.setTextViewText(R.id.tv_task, "休息一下");
                views.setInt(R.id.tv_task, "setTextColor", 0xFF1A1C2E);

                views.setTextViewText(R.id.tv_next_label, "接下来 · " + next.getStartTimeStr() + " - " + next.getEndTimeStr());
                views.setTextViewText(R.id.tv_next_task, next.title);
            } else {
                // 所有任务都结束了
                views.setTextViewText(R.id.tv_now_label, "现在");
                views.setTextViewText(R.id.tv_task, "今日任务全部完成");
                views.setInt(R.id.tv_task, "setTextColor", 0xFF1A1C2E);

                views.setTextViewText(R.id.tv_next_label, "接下来");
                views.setTextViewText(R.id.tv_next_task, "可以好好休息啦");
            }

        } else {
            // 今天没有任务
            views.setTextViewText(R.id.tv_now_label, "现在");
            views.setTextViewText(R.id.tv_task, "今日暂无安排");
            views.setInt(R.id.tv_task, "setTextColor", 0xFF1A1C2E);

            views.setTextViewText(R.id.tv_next_label, "接下来");
            views.setTextViewText(R.id.tv_next_task, "打开 App 添加任务");
        }

        // 点击小组件打开主界面
        Intent launch = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, launch,
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

    /** 格式化为 "11月23日 周一" */
    private String formatDate() {
        Calendar c = Calendar.getInstance();
        String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int w = c.get(Calendar.DAY_OF_WEEK) - 1;
        return month + "月" + day + "日 " + weeks[w];
    }

    /** 每分钟定时触发一次更新，实现到点自动切换 */
    private void scheduleNextAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyTaskWidgetProvider.class);
        intent.setAction(ACTION_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 60 秒后触发
        long triggerAt = SystemClock.elapsedRealtime() + 60 * 1000;
        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
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
