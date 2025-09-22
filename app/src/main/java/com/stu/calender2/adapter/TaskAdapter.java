package com.stu.calender2.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.stu.calender2.MyApplication;
import com.stu.calender2.R;
import com.stu.calender2.data.Task;
import com.stu.calender2.utils.TaskDiffCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 任务适配器，用于显示任务列表
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private static final String TAG = "TaskAdapter";
    private final CopyOnWriteArrayList<Task> taskList = new CopyOnWriteArrayList<>();
    private final OnTaskClickListener listener;
    private final Context context;
    
    // 颜色缓存
    private int whiteColor;
    private int blackColor;
    private int primaryColor;
    private int secondaryColor;
    
    // 视图类型和布局缓存
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_COMPLETED = 1;
    private final SparseArray<View> layoutCache = new SparseArray<>(2);
    
    // 日期格式化器缓存
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskToggleCompletion(Task task);
        void onDeleteTaskClick(Task task);
    }

    public TaskAdapter(Context context, OnTaskClickListener listener) {
        this.context = context;
        this.listener = listener;
        
        // 初始化颜色缓存
        initColorResources();
        
        // 设置初始容量以减少扩容开销
        setHasStableIds(true);
    }
    
    private void initColorResources() {
        MyApplication app = MyApplication.getInstance();
        if (app != null) {
            // 从应用程序缓存中获取颜色
            whiteColor = app.getCachedColor(R.color.white);
            blackColor = app.getCachedColor(R.color.black);
            primaryColor = app.getCachedColor(R.color.colorPrimary);
            secondaryColor = app.getCachedColor(R.color.colorAccent);
        } else {
            // 降级方案：直接从资源获取
            whiteColor = context.getResources().getColor(R.color.white, null);
            blackColor = context.getResources().getColor(R.color.black, null);
            primaryColor = context.getResources().getColor(R.color.colorPrimary, null);
            secondaryColor = context.getResources().getColor(R.color.colorAccent, null);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用缓存的布局，减少重复创建
        View itemView = layoutCache.get(viewType);
        if (itemView == null) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            layoutCache.put(viewType, itemView);
        } else {
            // 如果是从缓存中获取，需要先从之前的父视图中解除绑定
            if (itemView.getParent() != null) {
                ((ViewGroup) itemView.getParent()).removeView(itemView);
                // 重新创建，避免状态问题
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_task, parent, false);
            }
        }
        
        return new ViewHolder(itemView);
    }

    @Override
    public int getItemViewType(int position) {
        Task task = getItem(position);
        return task.isCompleted() ? VIEW_TYPE_COMPLETED : VIEW_TYPE_NORMAL;
    }
    
    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }
    
    private Task getItem(int position) {
        return taskList.get(position);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = getItem(position);
        
        // 避免不必要的文本设置
        if (!holder.taskTitle.getText().toString().equals(task.getTitle())) {
            holder.taskTitle.setText(task.getTitle());
        }
        
        // 根据任务状态设置样式
        updateTaskStyle(holder, task);
        
        // 设置日期文本
        updateDateText(holder, task);
        
        // 设置重要性标记
        updateImportanceIndicator(holder, task);
        
        // 设置点击事件处理
        setupClickListeners(holder, task);
    }
    
    private void updateTaskStyle(ViewHolder holder, Task task) {
        // 根据任务完成状态设置样式
        if (task.isCompleted()) {
            // 完成的任务样式
            holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.taskCard.setCardBackgroundColor(secondaryColor);
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                holder.taskTitle.setTextColor(blackColor);
                holder.taskTime.setTextColor(blackColor);
            } else {
                holder.taskTitle.setTextColor(whiteColor);
                holder.taskTime.setTextColor(whiteColor);
            }
        } else {
            // 未完成的任务样式
            holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.taskCard.setCardBackgroundColor(whiteColor);
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                holder.taskTitle.setTextColor(whiteColor);
                holder.taskTime.setTextColor(primaryColor);
            } else {
                holder.taskTitle.setTextColor(blackColor);
                holder.taskTime.setTextColor(primaryColor);
            }
        }
        
        // 设置复选框状态，避免触发监听器
        if (holder.taskCheckBox.isChecked() != task.isCompleted()) {
            holder.taskCheckBox.setOnCheckedChangeListener(null);
            holder.taskCheckBox.setChecked(task.isCompleted());
            holder.taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        listener.onTaskToggleCompletion(getItem(adapterPosition));
                    }
                }
            });
        }
    }
    
    private void updateDateText(ViewHolder holder, Task task) {
        Date startDate = task.getStartTime();
        Date endDate = task.getEndTime();
        if (startDate != null && endDate != null) {
            String dateStr = dateFormatter.format(startDate) + " - " + dateFormatter.format(endDate);
            if (!dateStr.equals(holder.taskTime.getText().toString())) {
                holder.taskTime.setText(dateStr);
                holder.taskTime.setVisibility(View.VISIBLE);
            }
        } else if (holder.taskTime.getVisibility() != View.GONE) {
            holder.taskTime.setVisibility(View.GONE);
        }
        
        // 设置地点
        String location = task.getLocation();
        if (location != null && !location.isEmpty()) {
            holder.taskLocation.setText(location);
            holder.taskLocation.setVisibility(View.VISIBLE);
        } else {
            holder.taskLocation.setVisibility(View.GONE);
        }
    }
    
    private void updateImportanceIndicator(ViewHolder holder, Task task) {
        // 根据任务重要性设置颜色标记
        int importance = task.getImportance();
        int colorResId;
        String importanceText = task.getQuadrantCategory();
        
        switch (importance) {
            case 1:
                colorResId = R.color.colorQuadrant1;
                break;
            case 2:
                colorResId = R.color.colorQuadrant2;
                break;
            case 3:
                colorResId = R.color.colorQuadrant3;
                break;
            case 4:
                colorResId = R.color.colorQuadrant4;
                break;
            default:
                colorResId = R.color.colorPrimary;
                break;
        }
        
        // 设置重要性文本和颜色
        holder.importanceIndicator.setText(importanceText);
        int color = context.getResources().getColor(colorResId, null);
        holder.importanceIndicator.setTextColor(color);
    }
    
    private void setupClickListeners(ViewHolder holder, Task task) {
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
        
        // 设置复选框点击事件
        holder.taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null && buttonView.isPressed()) {
                listener.onTaskToggleCompletion(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setTasks(List<Task> tasks) {
        if (tasks == null) {
            taskList.clear();
            notifyDataSetChanged();
            return;
        }
        
        if (taskList.isEmpty()) {
            // 首次加载，直接设置并通知
            taskList.clear();
            taskList.addAll(tasks);
            notifyDataSetChanged();
        } else {
            // 使用DiffUtil高效更新
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TaskDiffCallback(new ArrayList<>(taskList), tasks));
            taskList.clear();
            taskList.addAll(tasks);
            diffResult.dispatchUpdatesTo(this);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle;
        TextView taskTime;
        TextView taskLocation;
        TextView importanceIndicator;
        CardView taskCard;
        CheckBox taskCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 根据item_task.xml布局中的顶层元素是CardView
            taskCard = (CardView) itemView;
            taskTitle = itemView.findViewById(R.id.text_task_title);
            taskTime = itemView.findViewById(R.id.text_task_time);
            taskLocation = itemView.findViewById(R.id.text_task_location);
            importanceIndicator = itemView.findViewById(R.id.text_task_importance);
            taskCheckBox = itemView.findViewById(R.id.check_task_completed);
        }
    }
} 