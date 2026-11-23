package com.example.dailytaskwidget;

/**
 * 一个时段任务：从 startMinute 到 endMinute（当天分钟数），内容是 title。
 * 例如 09:00-10:00 开会 -> startMinute=540, endMinute=600
 */
public class Task {
    public int startMinute;   // 开始时间（0-1439，单位分钟）
    public int endMinute;     // 结束时间（0-1439，单位分钟）
    public String title;      // 任务内容

    public Task() {
    }

    public Task(int startMinute, int endMinute, String title) {
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.title = title;
    }

    /** 格式化为 "09:00" 这样的时间字符串 */
    public static String formatTime(int minute) {
        int h = minute / 60;
        int m = minute % 60;
        return String.format("%02d:%02d", h, m);
    }

    public String getStartTimeStr() {
        return formatTime(startMinute);
    }

    public String getEndTimeStr() {
        return formatTime(endMinute);
    }

    /** 当前分钟数是否落在这个时段内 */
    public boolean contains(int minute) {
        return minute >= startMinute && minute < endMinute;
    }
}
