package com.example.dailytaskwidget;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 主界面：配置今日（也可切换查看其他日期）的时间段任务
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private TextView tvEmpty;
    private TextView tvTitleDate;
    private TaskAdapter adapter;
    private final List<Task> taskList = new ArrayList<>();

    private String currentDateKey; // 当前查看的日期

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvTasks = findViewById(R.id.rv_tasks);
        tvEmpty = findViewById(R.id.tv_empty);
        tvTitleDate = findViewById(R.id.tv_title_date);
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

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
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
        tvTitleDate.setText(formatDateTitle(currentDateKey));
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
        TimePicker tpStart = dialogView.findViewById(R.id.tp_start);
        TimePicker tpEnd = dialogView.findViewById(R.id.tp_end);
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnOk = dialogView.findViewById(R.id.btn_ok);

        tpStart.setIs24HourView(true);
        tpEnd.setIs24HourView(true);

        // 默认时间：如果已有任务，衔接最后一个任务的结束时间；否则默认下一个整点
        int defaultStart, defaultEnd;
        if (!taskList.isEmpty()) {
            Task last = taskList.get(taskList.size() - 1);
            defaultStart = last.endMinute;
            defaultEnd = Math.min(defaultStart + 60, 24 * 60 - 1);
        } else {
            Calendar c = Calendar.getInstance();
            defaultStart = ((c.get(Calendar.HOUR_OF_DAY) + 1) % 24) * 60;
            defaultEnd = defaultStart + 60;
        }
        tpStart.setHour(defaultStart / 60);
        tpStart.setMinute(defaultStart % 60);
        tpEnd.setHour(defaultEnd / 60);
        tpEnd.setMinute(defaultEnd % 60);

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
                int startMinute = tpStart.getHour() * 60 + tpStart.getMinute();
                int endMinute = tpEnd.getHour() * 60 + tpEnd.getMinute();
                String title = etTitle.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(MainActivity.this, "请填写任务内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (endMinute <= startMinute) {
                    Toast.makeText(MainActivity.this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show();
                    return;
                }

                taskList.add(new Task(startMinute, endMinute, title));
                saveAndRefresh();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /** 把 "yyyy-MM-dd" 转成 "11月23日 周一" */
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
