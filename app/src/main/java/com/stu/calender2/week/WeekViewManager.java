package com.stu.calender2.week;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stu.calender2.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 周视图管理器，负责管理周视图的初始化和显示
 */
public class WeekViewManager {
    
    private Context context;
    private View weekView;
    private TextView[] weekDayHeaders;
    
    public WeekViewManager(Context context) {
        this.context = context;
    }
    
    /**
     * 初始化周视图
     * @param container 容器视图
     * @return 初始化后的周视图
     */
    public View initializeView(ViewGroup container) {
        LayoutInflater inflater = LayoutInflater.from(context);
        weekView = inflater.inflate(R.layout.view_week_calendar, container, false);
        
        // 初始化周视图标题
        weekDayHeaders = new TextView[7];
        weekDayHeaders[0] = weekView.findViewById(R.id.text_week_sun);
        weekDayHeaders[1] = weekView.findViewById(R.id.text_week_mon);
        weekDayHeaders[2] = weekView.findViewById(R.id.text_week_tue);
        weekDayHeaders[3] = weekView.findViewById(R.id.text_week_wed);
        weekDayHeaders[4] = weekView.findViewById(R.id.text_week_thu);
        weekDayHeaders[5] = weekView.findViewById(R.id.text_week_fri);
        weekDayHeaders[6] = weekView.findViewById(R.id.text_week_sat);
        
        return weekView;
    }
    
    /**
     * 设置周视图的日期标题
     * @param startOfWeek 一周的开始日期
     */
    public void setWeekHeaders(Calendar startOfWeek) {
        if (weekDayHeaders == null) {
            return;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
        Calendar calendar = (Calendar) startOfWeek.clone();
        
        for (int i = 0; i < 7; i++) {
            String dateText = dateFormat.format(calendar.getTime());
            weekDayHeaders[i].setText(dateText);
            calendar.add(Calendar.DAY_OF_WEEK, 1);
        }
    }
    
    /**
     * 清除周视图中的所有任务
     */
    public void clearTasks() {
        // 在这里实现清除周视图任务的逻辑
        // 通常是遍历周视图的任务容器，并删除所有子视图
    }
    
    /**
     * 获取周视图
     * @return 周视图
     */
    public View getWeekView() {
        return weekView;
    }
    
    /**
     * 根据日期获取对应的日视图容器
     * @param dayOfWeek 星期几 (Calendar.SUNDAY, Calendar.MONDAY, ...)
     * @return 对应的日视图容器
     */
    public ViewGroup getDayContainer(int dayOfWeek) {
        int index = dayOfWeek - Calendar.SUNDAY;
        if (index < 0 || index > 6) {
            return null;
        }
        
        int[] dayContainerIds = {
            R.id.week_sunday_container,
            R.id.week_monday_container,
            R.id.week_tuesday_container,
            R.id.week_wednesday_container,
            R.id.week_thursday_container,
            R.id.week_friday_container,
            R.id.week_saturday_container
        };
        
        return weekView.findViewById(dayContainerIds[index]);
    }
} 