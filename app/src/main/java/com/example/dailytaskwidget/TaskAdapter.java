package com.example.dailytaskwidget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 任务列表适配器
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    private final List<Task> tasks;
    private final OnDeleteListener deleteListener;

    public TaskAdapter(List<Task> tasks, OnDeleteListener deleteListener) {
        this.tasks = tasks;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskHolder holder, int position) {
        Task t = tasks.get(position);
        holder.tvTime.setText(t.getStartTimeStr() + " - " + t.getEndTimeStr());
        holder.tvTitle.setText(t.title);

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteListener.onDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, btnDelete;

        TaskHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time_range);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
