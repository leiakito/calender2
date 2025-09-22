package com.stu.calender2.week;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stu.calender2.R;
import com.stu.calender2.data.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 周视图任务适配器，负责在周视图中显示任务
 */
public class WeekTasksAdapter {
    private Context context;
    private WeekViewManager weekViewManager;
    private List<Task> taskList;
    
    public WeekTasksAdapter(Context context, WeekViewManager weekViewManager) {
        this.context = context;
        this.weekViewManager = weekViewManager;
        this.taskList = new ArrayList<>();
    }
    
    /**
     * 设置任务列表
     * @param tasks 任务列表
     */
    public void setTasks(List<Task> tasks) {
        this.taskList.clear();
        if (tasks != null) {
            this.taskList.addAll(tasks);
        }
        updateWeekView();
    }
    
    /**
     * 更新周视图显示
     */
    private void updateWeekView() {
        // 首先清除所有任务视图
        weekViewManager.clearTasks();
        
        // 按照日期分组任务
        for (Task task : taskList) {
            Calendar taskDate = Calendar.getInstance();
            taskDate.setTime(task.getStartTime());
            
            // 获取对应的日容器
            ViewGroup dayContainer = weekViewManager.getDayContainer(taskDate.get(Calendar.DAY_OF_WEEK));
            if (dayContainer != null) {
                // 添加任务视图
                addTaskView(dayContainer, task);
            }
        }
    }
    
    /**
     * 向日视图容器添加任务视图
     * @param container 日视图容器
     * @param task 任务对象
     */
    private void addTaskView(ViewGroup container, Task task) {
        // 创建任务视图
        LayoutInflater inflater = LayoutInflater.from(context);
        View taskView = inflater.inflate(R.layout.item_week_task, container, false);
        
        // 设置任务内容
        TextView taskTitle = taskView.findViewById(R.id.task_title);
        TextView taskTime = taskView.findViewById(R.id.task_time);
        
        taskTitle.setText(task.getTitle());
        
        // 设置任务时间
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeText = timeFormat.format(task.getStartTime());
        if (task.getEndTime() != null) {
            timeText += " - " + timeFormat.format(task.getEndTime());
        }
        taskTime.setText(timeText);
        
        // 设置任务优先级或类别的颜色
        View colorIndicator = taskView.findViewById(R.id.task_color_indicator);
        if (colorIndicator != null) {
            // 根据任务类型或优先级设置颜色
            int color = getTaskColorByImportance(task.getImportance());
            colorIndicator.setBackgroundColor(color);
        }
        
        // 添加到容器中
        container.addView(taskView);
    }
    
    /**
     * 根据任务重要性获取颜色
     * @param importance 任务重要性
     * @return 对应的颜色值
     */
    private int getTaskColorByImportance(int importance) {
        switch (importance) {
            case 1: // 重要且紧急
                return Color.RED;
            case 2: // 重要不紧急
                return Color.YELLOW;
            case 3: // 紧急不重要
                return Color.GREEN;
            case 4: // 不紧急不重要
                return Color.GRAY;
            default:
                return Color.GRAY;
        }
    }
    
    /**
     * 获取任务列表
     * @return 任务列表
     */
    public List<Task> getTaskList() {
        return taskList;
    }
} 