package com.example.dailytaskwidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 任务本地存储：按日期（yyyy-MM-dd）存到 SharedPreferences，
 * 每个日期对应一个 JSON 数组 [{start, end, title}, ...]
 */
public class TaskStore {

    private static final String PREFS_NAME = "daily_tasks";

    /** 获取今天的日期 key，例如 "2026-11-23" */
    public static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** 获取某天的日期 key（offset 天，负数为过去） */
    public static String dateKey(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    /** 读取某天的任务列表（按开始时间排序） */
    public static List<Task> getTasks(Context context, String dateKey) {
        List<Task> list = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(dateKey, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Task t = new Task(
                        o.getInt("start"),
                        o.getInt("end"),
                        o.getString("title")
                );
                list.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(list, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Integer.compare(a.startMinute, b.startMinute);
            }
        });
        return list;
    }

    /** 保存某天的任务列表 */
    public static void saveTasks(Context context, String dateKey, List<Task> tasks) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray arr = new JSONArray();
        for (Task t : tasks) {
            JSONObject o = new JSONObject();
            try {
                o.put("start", t.startMinute);
                o.put("end", t.endMinute);
                o.put("title", t.title);
                arr.put(o);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sp.edit().putString(dateKey, arr.toString()).apply();
    }

    /**
     * 找到当前时间对应的任务。
     * 返回 -1 表示当前没有任务（空闲/间隙）。
     */
    public static int findCurrentTaskIndex(List<Task> tasks) {
        Calendar c = Calendar.getInstance();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).contains(now)) {
                return i;
            }
        }
        return -1;
    }
}
