package com.example.dailytaskwidget;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 主界面：配置今天/明天的任务。
 * 时间用下拉选项选择（8:00-23:00，每小时一段）。
 */
public class MainActivity extends AppCompatActivity {

    // 时间段选项：8点到23点，每小时一段（共15段）
    private static final int START_HOUR = 8;   // 最早 8 点
    private static final int END_HOUR = 23;    // 最晚 23 点（即 22:00-23:00 是最后一段）

    private RecyclerView rvTasks;
    private TextView tvEmpty;
    private TextView tvTitleDate;
    private TextView tabToday;
    private TextView tabTomorrow;
    private TaskAdapter adapter;
    private final List<Task> taskList = new ArrayList<>();

    private String currentDateKey;   // 当前查看的日期
    private boolean showingTomorrow = false; // 是否正在查看明天

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvTasks = findViewById(R.id.rv_tasks);
        tvEmpty = findViewById(R.id.tv_empty);
        tvTitleDate = findViewById(R.id.tv_title_date);
        tabToday = findViewById(R.id.tab_today);
        tabTomorrow = findViewById(R.id.tab_tomorrow);
        Button btnAdd = findViewById(R.id.btn_add);

        // 默认显示今天
        currentDateKey = TaskStore.todayKey();

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(taskList, new TaskAdapter.OnDeleteListener() {
            @Override
            public void onDelete(int position) {
                taskList.remove(position);
                saveAndRefresh();
            }
        });
        rvTasks.setAdapter(adapter);

        // 今天/明天切换
        tabToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToToday();
            }
        });
        tabTomorrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTomorrow();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

        updateTabUI();
        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void switchToToday() {
        showingTomorrow = false;
        currentDateKey = TaskStore.todayKey();
        updateTabUI();
        loadTasks();
    }

    private void switchToTomorrow() {
        showingTomorrow = true;
        currentDateKey = TaskStore.dateKey(1); // 明天
        updateTabUI();
        loadTasks();
    }

    /** 更新今天/明天标签的高亮样式 */
    private void updateTabUI() {
        // 今天标签
        if (showingTomorrow) {
            tabToday.setBackgroundResource(android.R.color.transparent);
            tabToday.setTextColor(0xFF8A8DA3);
        } else {
            tabToday.setBackgroundResource(R.color.primary);
            tabToday.setTextColor(0xFFFFFFFF);
        }
        // 明天标签
        if (showingTomorrow) {
            tabTomorrow.setBackgroundResource(R.color.primary);
            tabTomorrow.setTextColor(0xFFFFFFFF);
        } else {
            tabTomorrow.setBackgroundResource(android.R.color.transparent);
            tabTomorrow.setTextColor(0xFF8A8DA3);
        }
    }

    /** 加载当前日期的任务 */
    private void loadTasks() {
        taskList.clear();
        taskList.addAll(TaskStore.getTasks(this, currentDateKey));
        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateTitleDate();
    }

    private void updateTitleDate() {
        String prefix = showingTomorrow ? "明天 · " : "今天 · ";
        tvTitleDate.setText(prefix + formatDateTitle(currentDateKey));
    }

    private void updateEmptyState() {
        if (taskList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }
    }

    /** 保存并刷新小组件 */
    private void saveAndRefresh() {
        TaskStore.saveTasks(this, currentDateKey, taskList);
        adapter.notifyDataSetChanged();
        updateEmptyState();
        // 立即刷新桌面小组件
        DailyTaskWidgetProvider.refreshAll(this);
    }

    /** 弹出添加任务对话框 */
    private void showAddDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        Spinner spTime = dialogView.findViewById(R.id.sp_time);
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnOk = dialogView.findViewById(R.id.btn_ok);

        // 时间段选项：8:00-9:00, 9:00-10:00 ... 22:00-23:00
        List<String> timeOptions = new ArrayList<>();
        List<Integer> startMinutes = new ArrayList<>();
        for (int h = START_HOUR; h < END_HOUR; h++) {
            String label = String.format("%02d:00 - %02d:00", h, h + 1);
            timeOptions.add(label);
            startMinutes.add(h * 60);
        }
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, timeOptions);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTime.setAdapter(adapterSpinner);

        // 默认选中第一个（8:00-9:00），如果已有任务则选中最后一个的下一个时段
        if (!taskList.isEmpty()) {
            Task last = taskList.get(taskList.size() - 1);
            int nextStart = last.endMinute;
            int defaultPos = 0;
            for (int i = 0; i < startMinutes.size(); i++) {
                if (startMinutes.get(i) >= nextStart) {
                    defaultPos = i;
                    break;
                }
            }
            spTime.setSelection(defaultPos);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = spTime.getSelectedItemPosition();
                if (pos < 0 || pos >= startMinutes.size()) {
                    Toast.makeText(MainActivity.this, "请选择时间段", Toast.LENGTH_SHORT).show();
                    return;
                }
                int startMinute = startMinutes.get(pos);
                int endMinute = startMinute + 60; // 每小时一段
                String title = etTitle.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(MainActivity.this, "请填写任务内容", Toast.LENGTH_SHORT).show();
                    return;
                }

                taskList.add(new Task(startMinute, endMinute, title));
                saveAndRefresh();

                // 连续添加：不关闭弹窗，清空输入，自动选中下一个时段
                etTitle.setText("");
                if (pos + 1 < startMinutes.size()) {
                    spTime.setSelection(pos + 1);
                }
                Toast.makeText(MainActivity.this, "已保存，可继续添加（点完成退出）", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    /** 把 "yyyy-MM-dd" 转成 "11月24日 周二" */
    private String formatDateTitle(String dateKey) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date date = sdf.parse(dateKey);
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            return (c.get(Calendar.MONTH) + 1) + "月" + c.get(Calendar.DAY_OF_MONTH) + "日 " + weeks[c.get(Calendar.DAY_OF_WEEK) - 1];
        } catch (Exception e) {
            return dateKey;
        }
    }
}
