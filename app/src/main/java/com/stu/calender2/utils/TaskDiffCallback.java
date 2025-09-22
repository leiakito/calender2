package com.stu.calender2.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.stu.calender2.data.Task;

import java.util.List;

/**
 * 任务列表差异计算工具类，用于优化RecyclerView更新
 */
public class TaskDiffCallback extends DiffUtil.Callback {
    private final List<Task> oldTasks;
    private final List<Task> newTasks;

    public TaskDiffCallback(List<Task> oldTasks, List<Task> newTasks) {
        this.oldTasks = oldTasks;
        this.newTasks = newTasks;
    }

    @Override
    public int getOldListSize() {
        return oldTasks.size();
    }

    @Override
    public int getNewListSize() {
        return newTasks.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // 比较项ID是否相同
        Task oldTask = oldTasks.get(oldItemPosition);
        Task newTask = newTasks.get(newItemPosition);
        return oldTask.getId() == newTask.getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // 比较任务详细内容是否相同
        Task oldTask = oldTasks.get(oldItemPosition);
        Task newTask = newTasks.get(newItemPosition);
        
        // 根据任务的关键属性进行比较
        boolean sameTitle = (oldTask.getTitle() == null && newTask.getTitle() == null) ||
                (oldTask.getTitle() != null && oldTask.getTitle().equals(newTask.getTitle()));
                
        boolean sameDate = (oldTask.getStartTime() == null && newTask.getStartTime() == null) ||
                (oldTask.getStartTime() != null && oldTask.getStartTime().equals(newTask.getStartTime()));
                
        boolean sameImportance = oldTask.getImportance() == newTask.getImportance();
        boolean sameCompleted = oldTask.isCompleted() == newTask.isCompleted();
        
        // 所有属性都相同则返回true
        return sameTitle && sameDate && sameImportance && sameCompleted;
    }
    
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // 可以提供变更的详细负载信息，用于部分更新
        // 这里简化处理，返回null将触发完整的onBindViewHolder
        return null;
    }
} 