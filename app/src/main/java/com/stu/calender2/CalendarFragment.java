package com.stu.calender2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.*;
import com.stu.calender2.data.Task;
import com.stu.calender2.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.stu.calender2.utils.EInkDisplayHelper;
import com.stu.calender2.utils.EInkAnimationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.widget.AbsListView;

public class CalendarFragment extends Fragment implements View.OnClickListener {

    private CalendarView calendarView;
    private Button btnThreeDay, btnWeek, btnMonth;
    private LinearLayout threeDayView, weekView, monthView;
    private FloatingActionButton fabAddEvent;

    // 三日视图的日期标题
    private TextView textDay1, textDay2, textDay3;

    // 周视图的日期标题
    private TextView textWeekSun, textWeekMon, textWeekTue, textWeekWed, textWeekThu, textWeekFri, textWeekSat;

    private Calendar currentCalendar;
    private int currentViewMode = 2; // 0: 三日, 1: 周, 2: 月

    // 任务ViewModel
    private TaskViewModel taskViewModel;

    // 添加月视图任务指示器
    private TextView monthTaskIndicator;
    private Map<String, Integer> monthTasksCountByDate = new HashMap<>();

    // 保存对countBadge的引用，用于更新任务数量
    private TextView monthTaskBadge;

    public CalendarFragment() {
        // Required empty public constructor
    }

    public static CalendarFragment newInstance() {
        return new CalendarFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 禁用所有视图的动画
        EInkDisplayHelper.disableAnimationsRecursively(view);
        
        // 禁用 ScrollView 的滚动动画
        ScrollView scrollView = view.findViewById(R.id.three_day_scrollview);
        if (scrollView != null) {
            EInkDisplayHelper.disableScrollViewAnimations(scrollView);
        }
        
        // 初始化日历
        currentCalendar = Calendar.getInstance();

        // 初始化任务ViewModel
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        // 初始化视图
        initViews(view);

        // 设置监听器
        setupListeners();

        // 默认显示月视图
        showMonthView();
        updateViewModeButtons();

        // 监听当天任务
        observeTasks();

        // 优化CalendarView性能
        optimizeCalendarViewForEInk();

        if (calendarView != null) {
            // 设置日期选中颜色
            calendarView.setSelectedWeekBackgroundColor(getResources().getColor(R.color.colorPrimary));
            calendarView.setFocusedMonthDateColor(getResources().getColor(R.color.colorPrimary));
            calendarView.setUnfocusedMonthDateColor(getResources().getColor(R.color.colorPrimaryDark));
            
            // 对于水墨屏优化，禁用动画和滚动效果
            calendarView.setShowWeekNumber(false);
            calendarView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            
            // 尝试通过反射设置更多样式属性
            try {
                // 获取内部日期视图
                Field dateTextAppearanceField = CalendarView.class.getDeclaredField("mDateTextAppearanceResId");
                dateTextAppearanceField.setAccessible(true);
                dateTextAppearanceField.set(calendarView, R.style.CalendarDateTextAppearance);
                
                // 刷新视图以应用样式
                calendarView.invalidate();
            } catch (Exception e) {
                // 忽略反射异常
            }
        }

        // 初始化ScrollView ID，确保能正确找到
        initViewIds();
    }

    private void initViews(View view) {
        // 获取切换按钮
        btnThreeDay = view.findViewById(R.id.btn_three_day);
        btnWeek = view.findViewById(R.id.btn_week);
        btnMonth = view.findViewById(R.id.btn_month);

        // 获取各种视图
        threeDayView = view.findViewById(R.id.three_day_view);
        weekView = view.findViewById(R.id.week_view);
        monthView = view.findViewById(R.id.month_view);
        calendarView = view.findViewById(R.id.calendarView);

        // 获取三日视图的日期标题
        textDay1 = view.findViewById(R.id.text_day_1);
        textDay2 = view.findViewById(R.id.text_day_2);
        textDay3 = view.findViewById(R.id.text_day_3);

        // 获取周视图的日期标题
        textWeekSun = view.findViewById(R.id.text_week_sun);
        textWeekMon = view.findViewById(R.id.text_week_mon);
        textWeekTue = view.findViewById(R.id.text_week_tue);
        textWeekWed = view.findViewById(R.id.text_week_wed);
        textWeekThu = view.findViewById(R.id.text_week_thu);
        textWeekFri = view.findViewById(R.id.text_week_fri);
        textWeekSat = view.findViewById(R.id.text_week_sat);

        // 添加事件按钮
        fabAddEvent = view.findViewById(R.id.fab_add_event);
        if (fabAddEvent == null) {
            // 如果布局中没有，则动态创建
            fabAddEvent = new FloatingActionButton(requireContext());
            fabAddEvent.setId(R.id.fab_add_event);
            fabAddEvent.setImageResource(R.drawable.ic_add);
            fabAddEvent.setContentDescription("添加事件");
            fabAddEvent.setBackgroundTintList(getResources().getColorStateList(R.color.colorCalendar));

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
            params.setMargins(0, 0, 32, 32);

            ((ViewGroup) view).addView(fabAddEvent, params);
        }

        // 添加月视图任务指示器
        // 使用CardView作为容器
        CardView monthTaskCard = new CardView(requireContext());
        monthTaskCard.setCardElevation(4);
        monthTaskCard.setRadius(8);
        monthTaskCard.setContentPadding(0, 0, 0, 0);
        monthTaskCard.setCardBackgroundColor(getResources().getColor(R.color.white));
        monthTaskCard.setUseCompatPadding(true);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(16, 16, 16, 16);
        monthTaskCard.setLayoutParams(cardParams);

        // 创建标题和内容的垂直容器
        LinearLayout taskCardContent = new LinearLayout(requireContext());
        taskCardContent.setOrientation(LinearLayout.VERTICAL);
        taskCardContent.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 创建标题栏
        LinearLayout titleBar = new LinearLayout(requireContext());
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        titleBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        titleBar.setPadding(16, 8, 16, 8);

        // 标题文本
        TextView titleText = new TextView(requireContext());
        titleText.setText("今日任务");
        titleText.setTextColor(getResources().getColor(android.R.color.white));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        // 任务数量标志
        TextView countBadge = new TextView(requireContext());
        countBadge.setTextColor(getResources().getColor(android.R.color.white));
        countBadge.setTextSize(14);
        countBadge.setTypeface(null, Typeface.BOLD);
        countBadge.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        countBadge.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 任务内容区域
        monthTaskIndicator = new TextView(requireContext());
        monthTaskIndicator.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        monthTaskIndicator.setPadding(16, 16, 16, 16);
        monthTaskIndicator.setVisibility(View.GONE);

        // 组装视图
        titleBar.addView(titleText);
        titleBar.addView(countBadge);
        taskCardContent.addView(titleBar);
        taskCardContent.addView(monthTaskIndicator);
        monthTaskCard.addView(taskCardContent);

        // 添加到monthView
        if (monthView != null) {
            monthView.addView(monthTaskCard, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // 保存对countBadge的引用，用于更新任务数量
        this.monthTaskBadge = countBadge;
    }

    private void setupListeners() {
        // 设置按钮点击监听器
        btnThreeDay.setOnClickListener(this);
        btnWeek.setOnClickListener(this);
        btnMonth.setOnClickListener(this);
        // 设置日期选择监听器1
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            currentCalendar.set(year, month, dayOfMonth);
            String date = year + "年" + (month + 1) + "月" + dayOfMonth + "日";

            // 显示当天的任务数量
            String dateKey = formatDateKey(year, month, dayOfMonth);
            int taskCount = 0;
            if (monthTasksCountByDate.containsKey(dateKey)) {
                taskCount = monthTasksCountByDate.get(dateKey);
            }
            
            if (taskCount > 0) {
                String taskMessage = "当天有 " + taskCount + " 个任务";
                Toast.makeText(getContext(), date + " - " + taskMessage, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "选择了: " + date, Toast.LENGTH_SHORT).show();
            }
        
            // 根据当前视图模式更新日期标题
            if (currentViewMode == 0) {
                updateThreeDayHeaders();
            } else if (currentViewMode == 1) {
                updateWeekHeaders();
            }
        
            // 更新任务显示
            observeTasks();
        });

        // 设置添加事件按钮点击监听器
        fabAddEvent.setOnClickListener(v -> {
            navigateToAddTask();
        });
    }

    private void navigateToAddTask() {
        try {
            NavController navController = NavHostFragment.findNavController(this);

            // 获取选定的日期
            Bundle bundle = new Bundle();
            bundle.putLong("selectedDate", currentCalendar.getTimeInMillis());

            // 导航到添加任务界面
            navController.navigate(R.id.action_calendarFragment_to_taskDetailFragment, bundle);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "导航失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * 监听任务并更新显示
     */
    private void observeTasks() {
        try {
            // 创建日期范围
            Calendar startOfDay = (Calendar) currentCalendar.clone();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);

            Calendar endOfDay = (Calendar) startOfDay.clone();
            endOfDay.add(Calendar.DAY_OF_MONTH, 1);

            // 获取当天的任务
            taskViewModel.getTasksByDateRange(startOfDay.getTime(), endOfDay.getTime())
                    .observe(getViewLifecycleOwner(), this::displayTasks);

            // 根据当前视图模式加载不同范围的任务
            if (currentViewMode == 0) { // 三日视图
                // 创建三天的日期范围
                Calendar startOfThreeDays = (Calendar) startOfDay.clone();
                Calendar endOfThreeDays = (Calendar) startOfDay.clone();
                endOfThreeDays.add(Calendar.DAY_OF_MONTH, 3); // 加三天
                
                // 获取三天的所有任务
                taskViewModel.getTasksByDateRange(startOfThreeDays.getTime(), endOfThreeDays.getTime())
                        .observe(getViewLifecycleOwner(), tasks -> {
                            if (tasks == null) return;
                            
                            // 按天分组任务
                            List<Task>[] tasksByDay = new List[3];
                            for (int i = 0; i < 3; i++) {
                                tasksByDay[i] = new ArrayList<>();
                            }
                            
                            // 对任务进行分组
                            Calendar taskDate = Calendar.getInstance();
                            for (Task task : tasks) {
                                taskDate.setTime(task.getStartTime());
                                
                                // 计算任务与开始日期的差值（天数）
                                int dayDiff = calculateDayDifference(startOfThreeDays, taskDate);
                                
                                // 如果在三天范围内，添加到对应的列表
                                if (dayDiff >= 0 && dayDiff < 3) {
                                    tasksByDay[dayDiff].add(task);
                                }
                            }
                            
                            // 显示每天的任务
                            for (int day = 0; day < 3; day++) {
                                displayTasksForDay(tasksByDay[day], day);
                            }
                        });
                
            } else if (currentViewMode == 1) { // 周视图
                // 获取本周的起始日期（周日）
                Calendar startOfWeek = (Calendar) currentCalendar.clone();
                startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
                startOfWeek.set(Calendar.MINUTE, 0);
                startOfWeek.set(Calendar.SECOND, 0);
                startOfWeek.set(Calendar.MILLISECOND, 0);

                Calendar endOfWeek = (Calendar) startOfWeek.clone();
                endOfWeek.add(Calendar.DAY_OF_MONTH, 7); // 加7天

                // 获取一周的任务
                taskViewModel.getTasksByDateRange(startOfWeek.getTime(), endOfWeek.getTime())
                        .observe(getViewLifecycleOwner(), this::displayWeekTasks);
                
            } else { // 月视图
                // 获取当月的开始和结束日期
                Calendar startOfMonth = (Calendar) currentCalendar.clone();
                startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
                startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
                startOfMonth.set(Calendar.MINUTE, 0);
                startOfMonth.set(Calendar.SECOND, 0);
                startOfMonth.set(Calendar.MILLISECOND, 0);

                Calendar endOfMonth = (Calendar) startOfMonth.clone();
                endOfMonth.add(Calendar.MONTH, 1);

                // 获取整个月的任务
                taskViewModel.getTasksByDateRange(startOfMonth.getTime(), endOfMonth.getTime())
                        .observe(getViewLifecycleOwner(), tasks -> {
                            updateMonthTasksCount(tasks);
                            displayMonthTasks(tasks);
                        });
            }
        } catch (Exception e) {
            Log.e("CalendarFragment", "加载任务出错", e);
            Toast.makeText(requireContext(), "加载任务出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 计算两个日期之间的天数差异
     * @param date1 第一个日期
     * @param date2 第二个日期
     * @return 天数差（date2 - date1）
     */
    private int calculateDayDifference(Calendar date1, Calendar date2) {
        // 复制Calendar对象，确保不会修改原始对象
        Calendar d1 = (Calendar) date1.clone();
        Calendar d2 = (Calendar) date2.clone();
        
        // 重置时分秒，只比较日期部分
        d1.set(Calendar.HOUR_OF_DAY, 0);
        d1.set(Calendar.MINUTE, 0);
        d1.set(Calendar.SECOND, 0);
        d1.set(Calendar.MILLISECOND, 0);
        
        d2.set(Calendar.HOUR_OF_DAY, 0);
        d2.set(Calendar.MINUTE, 0);
        d2.set(Calendar.SECOND, 0);
        d2.set(Calendar.MILLISECOND, 0);
        
        // 计算天数差
        long diff = d2.getTimeInMillis() - d1.getTimeInMillis();
        return (int) (diff / (24 * 60 * 60 * 1000));
    }

    /**
     * 显示任务
     * @param tasks 任务列表
     */
    private void displayTasks(List<Task> tasks) {
        if (currentViewMode == 0) {
            // 三日视图第一天
            displayTasksForDay(tasks, 0);
        } else if (currentViewMode == 1) {
            // 周视图
            displayWeekTasks(tasks);
        } else {
            // 月视图
            displayMonthTasks(tasks);
        }
    }

    /**
     * 在三日视图中为指定天显示任务
     * @param tasks 任务列表
     * @param dayIndex 日期索引（0: 第一天, 1: 第二天, 2: 第三天）
     */
    private void displayTasksForDay(List<Task> tasks, int dayIndex) {
        // 先清除该天所有时间单元格中的任务
        for (int hour = 0; hour <= 24; hour++) {
            try {
                // 使用更可靠的方式查找单元格
                FrameLayout cell = findDayCell(dayIndex, hour);
                if (cell != null) {
                    cell.removeAllViews();
                }
            } catch (Exception e) {
                // 忽略错误
            }
        }

        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        // 按时间对任务进行分组
        Map<Integer, List<Task>> tasksByHour = new HashMap<>();

        for (Task task : tasks) {
            Calendar taskStartTime = Calendar.getInstance();
            taskStartTime.setTime(task.getStartTime());
            int hour = taskStartTime.get(Calendar.HOUR_OF_DAY);

            if (hour >= 0 && hour <= 24) {
                if (!tasksByHour.containsKey(hour)) {
                    tasksByHour.put(hour, new ArrayList<>());
                }
                tasksByHour.get(hour).add(task);
            }
        }

        // 为每个小时显示任务
        for (int hour : tasksByHour.keySet()) {
            List<Task> hourTasks = tasksByHour.get(hour);
            if (hourTasks == null || hourTasks.isEmpty()) {
                continue;
            }

            // 找到对应的时间单元格
            FrameLayout cell = findDayCell(dayIndex, hour);
            if (cell != null) {
                // 创建垂直布局容器
                LinearLayout taskContainer = new LinearLayout(requireContext());
                taskContainer.setOrientation(LinearLayout.VERTICAL);
                taskContainer.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                // 添加每个任务
                for (Task task : hourTasks) {
                    addTaskViewToContainer(taskContainer, task);
                }

                // 将容器添加到单元格
                cell.removeAllViews();
                cell.addView(taskContainer);
            }
        }
    }

    // 修改findDayCell方法，支持使用标签和ID查找单元格
    private FrameLayout findDayCell(int dayIndex, int hour) {
        if (dayIndex < 0 || dayIndex > 2 || hour < 0 || hour > 24) {
            return null;
        }
        
        try {
            Log.d("CalendarFragment", "开始查找单元格: day=" + dayIndex + ", hour=" + hour);
            
            // 获取三日视图的ScrollView
            ScrollView scrollView = threeDayView.findViewById(R.id.three_day_scrollview);
            if (scrollView == null || scrollView.getChildCount() == 0) {
                Log.d("CalendarFragment", "三日视图的ScrollView不存在或为空");
                return null;
            }
            
            // 获取ScrollView的内容LinearLayout (包含所有时间行)
            View contentView = scrollView.getChildAt(0);
            if (!(contentView instanceof LinearLayout)) {
                Log.d("CalendarFragment", "ScrollView的内容不是LinearLayout");
                return null;
            }
            
            LinearLayout timeRowsContainer = (LinearLayout) contentView;
            
            // 遍历所有时间行，查找对应小时的行
            for (int i = 0; i < timeRowsContainer.getChildCount(); i++) {
                View child = timeRowsContainer.getChildAt(i);
                if (!(child instanceof LinearLayout)) continue;
                
                LinearLayout timeRow = (LinearLayout) child;
                
                // 检查第一个子视图是否是时间标签
                if (timeRow.getChildCount() <= 1 || !(timeRow.getChildAt(0) instanceof TextView)) continue;
                
                TextView timeLabel = (TextView) timeRow.getChildAt(0);
                String timeText = timeLabel.getText().toString().trim();
                
                // 检查是否是我们要找的小时
                if (timeText.contains(String.format("%02d:00", hour))) {
                    Log.d("CalendarFragment", "找到小时行: " + timeText);
                    
                    // 现在查找对应的日期单元格
                    // 第二个子视图可能包含三个日期单元格
                    if (timeRow.getChildCount() <= 1) continue;
                    
                    // 获取单元格容器(可能是LinearLayout或include的布局)
                    View cellsContainer = timeRow.getChildAt(1);
                    
                    // 如果是ViewGroup,则查找其中的单元格
                    if (cellsContainer instanceof ViewGroup) {
                        ViewGroup container = (ViewGroup) cellsContainer;
                        
                        // 1. 首先尝试通过tag查找
                        String targetTag = "day" + (dayIndex + 1);
                        for (int j = 0; j < container.getChildCount(); j++) {
                            View frameChild = container.getChildAt(j);
                            if (frameChild instanceof FrameLayout) {
                                String tag = (String) frameChild.getTag();
                                if (tag != null && tag.equals(targetTag)) {
                                    Log.d("CalendarFragment", "通过tag找到单元格: " + tag);
                                    return (FrameLayout) frameChild;
                                }
                            }
                        }
                        
                        // 2. 如果通过tag找不到,通过子视图索引查找
                        // 需要考虑布局中包含分隔线
                        int frameCount = 0;
                        for (int j = 0; j < container.getChildCount(); j++) {
                            View frameChild = container.getChildAt(j);
                            if (frameChild instanceof FrameLayout) {
                                if (frameCount == dayIndex) {
                                    Log.d("CalendarFragment", "通过索引找到单元格: day=" + dayIndex);
                                    return (FrameLayout) frameChild;
                                }
                                frameCount++;
                            }
                        }
                    }
                }
            }
            
            Log.d("CalendarFragment", "找不到单元格: day=" + dayIndex + ", hour=" + hour);
        } catch (Exception e) {
            Log.e("CalendarFragment", "查找单元格出错: " + e.getMessage(), e);
        }
        
        // 如果找不到，返回null而不是创建临时单元格
        // 这样可以更清晰地发现错误
        return null;
    }

    // 添加新方法，将任务添加到容器中
    private void addTaskViewToContainer(LinearLayout container, Task task) {
        // 创建任务视图容器
        LinearLayout taskContainer = new LinearLayout(requireContext());
        taskContainer.setOrientation(LinearLayout.VERTICAL);
        taskContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 根据重要程度设置背景颜色
        int colorRes;
        switch (task.getImportance()) {
            case 1: // 重要紧急
                colorRes = R.color.colorQuadrant1;
                break;
            case 2: // 重要不紧急
                colorRes = R.color.colorQuadrant2;
                break;
            case 3: // 紧急不重要
                colorRes = R.color.colorQuadrant3;
                break;
            case 4: // 不紧急不重要
                colorRes = R.color.colorQuadrant4;
                break;
            default:
                colorRes = R.color.colorPrimaryDark;
                break;
        }
        taskContainer.setBackgroundColor(getResources().getColor(colorRes));

        // 创建任务标题视图
        TextView titleView = new TextView(requireContext());
        titleView.setText(task.getTitle());
        titleView.setTextColor(getResources().getColor(android.R.color.white));
        titleView.setPadding(8, 4, 8, 2);
        titleView.setMaxLines(1);
        titleView.setTextSize(14);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setEllipsize(TextUtils.TruncateAt.END);

        // 设置外边距
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titleView.setLayoutParams(titleParams);

        // 创建任务备注视图
        TextView noteView = new TextView(requireContext());
        if (!TextUtils.isEmpty(task.getNote())) {
            noteView.setText(task.getNote());
            noteView.setTextColor(getResources().getColor(android.R.color.white));
            noteView.setPadding(8, 0, 8, 4);
            noteView.setMaxLines(1);
            noteView.setTextSize(12);
            noteView.setAlpha(0.8f);
            noteView.setEllipsize(TextUtils.TruncateAt.END);

            // 设置外边距
            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            noteView.setLayoutParams(noteParams);
        } else {
            noteView.setVisibility(View.GONE);
        }

        // 添加到容器
        taskContainer.addView(titleView);
        taskContainer.addView(noteView);

        // 设置点击事件，跳转到任务详情
        taskContainer.setOnClickListener(v -> {
            try {
                NavController navController = NavHostFragment.findNavController(this);
                Bundle bundle = new Bundle();
                bundle.putLong("taskId", task.getId());
                navController.navigate(R.id.action_calendarFragment_to_taskDetailFragment, bundle);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "导航失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        // 添加到外部容器
        container.addView(taskContainer);
    }

    /**
     * 在周视图中显示任务
     * @param tasks 任务列表
     */
    private void displayWeekTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        // 清除之前的所有任务视图
        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour <= 24; hour++) {
                try {
                    FrameLayout cell = findWeekCell(day, hour);
                    if (cell != null) {
                        cell.removeAllViews();
                    }
                } catch (Exception e) {
                    // 如果找不到单元格，忽略错误
                }
            }
        }

        // 按星期几和小时对任务进行分组
        Map<Integer, Map<Integer, List<Task>>> tasksByDayAndHour = new HashMap<>();
        for (int day = 0; day < 7; day++) {
            tasksByDayAndHour.put(day, new HashMap<>());
        }

        for (Task task : tasks) {
            Calendar taskStartTime = Calendar.getInstance();
            taskStartTime.setTime(task.getStartTime());
            int dayOfWeek = taskStartTime.get(Calendar.DAY_OF_WEEK) - 1; // 0: 周日, 1: 周一, ...
            int hour = taskStartTime.get(Calendar.HOUR_OF_DAY);

            if (hour >= 0 && hour <= 24) {
                Map<Integer, List<Task>> dayTasks = tasksByDayAndHour.get(dayOfWeek);
                if (!dayTasks.containsKey(hour)) {
                    dayTasks.put(hour, new ArrayList<>());
                }
                dayTasks.get(hour).add(task);
            }
        }

        // 为每个单元格显示任务
        for (int day = 0; day < 7; day++) {
            Map<Integer, List<Task>> dayTasks = tasksByDayAndHour.get(day);
            for (int hour : dayTasks.keySet()) {
                List<Task> hourTasks = dayTasks.get(hour);
                if (hourTasks == null || hourTasks.isEmpty()) {
                    continue;
                }

                // 找到对应的单元格
                FrameLayout cell = findWeekCell(day, hour);
                if (cell != null) {
                    // 创建垂直布局容器
                    LinearLayout taskContainer = new LinearLayout(requireContext());
                    taskContainer.setOrientation(LinearLayout.VERTICAL);
                    taskContainer.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    // 添加每个任务
                    for (Task task : hourTasks) {
                        addTaskViewToContainer(taskContainer, task);
                    }

                    // 将容器添加到单元格
                    cell.removeAllViews();
                    cell.addView(taskContainer);
                }
            }
        }
    }

    /**
     * 查找周视图中对应日期和时间的单元格
     * @param dayOfWeek 星期几（0-6，对应周日到周六）
     * @param hour 小时（0-24）
     * @return 对应的单元格，如果找不到则返回null
     */
    private FrameLayout findWeekCell(int dayOfWeek, int hour) {
        if (dayOfWeek < 0 || dayOfWeek > 6 || hour < 0 || hour > 24) {
            Log.d("CalendarFragment", "参数无效: day=" + dayOfWeek + ", hour=" + hour);
            return null;
        }

        try {
            Log.d("CalendarFragment", "开始查找周视图单元格: day=" + dayOfWeek + ", hour=" + hour);
            
            // 确保周视图可见
            if (weekView == null || weekView.getVisibility() != View.VISIBLE) {
                Log.d("CalendarFragment", "周视图不可见或为null");
                return null;
            }
            
            // 先检查初始化IDs
            initViewIds();
            
            // 获取周视图的ScrollView
            ScrollView scrollView = weekView.findViewById(R.id.week_scrollview);
            if (scrollView == null) {
                Log.d("CalendarFragment", "周视图的ScrollView找不到，尝试查找未命名的ScrollView");
                // 尝试查找第一个ScrollView
                for (int i = 0; i < weekView.getChildCount(); i++) {
                    View child = weekView.getChildAt(i);
                    if (child instanceof ScrollView) {
                        scrollView = (ScrollView) child;
                        scrollView.setId(R.id.week_scrollview);
                        Log.d("CalendarFragment", "找到并设置了周视图ScrollView的ID");
                        break;
                    }
                }
                
                if (scrollView == null) {
                    Log.d("CalendarFragment", "周视图中找不到ScrollView");
                    return null;
                }
            }
            
            if (scrollView.getChildCount() == 0) {
                Log.d("CalendarFragment", "周视图的ScrollView中没有子视图");
                return null;
            }
            
            // 获取ScrollView的内容LinearLayout(包含所有时间行)
            View contentView = scrollView.getChildAt(0);
            if (!(contentView instanceof LinearLayout)) {
                Log.d("CalendarFragment", "周视图ScrollView的内容不是LinearLayout，而是 " + contentView.getClass().getSimpleName());
                return null;
            }
            
            LinearLayout timeRowsContainer = (LinearLayout) contentView;
            Log.d("CalendarFragment", "周视图时间行容器中有 " + timeRowsContainer.getChildCount() + " 个子视图");
            
            // 遍历所有时间行，查找对应小时的行
            for (int i = 0; i < timeRowsContainer.getChildCount(); i++) {
                View child = timeRowsContainer.getChildAt(i);
                if (!(child instanceof LinearLayout)) {
                    Log.d("CalendarFragment", "时间行容器中第 " + i + " 个子视图不是LinearLayout");
                    continue;
                }
                
                LinearLayout timeRow = (LinearLayout) child;
                
                // 检查第一个子视图是否是时间标签
                if (timeRow.getChildCount() <= 1 || !(timeRow.getChildAt(0) instanceof TextView)) {
                    Log.d("CalendarFragment", "时间行 " + i + " 中没有时间标签或子视图不足");
                    continue;
                }
                
                TextView timeLabel = (TextView) timeRow.getChildAt(0);
                String timeText = timeLabel.getText().toString().trim();
                
                // 检查是否是我们要找的小时
                if (timeText.contains(String.format("%02d:00", hour))) {
                    Log.d("CalendarFragment", "找到周视图小时行: " + timeText + "，包含 " + timeRow.getChildCount() + " 个子视图");
                    
                    // 第二个子视图可能包含七个日期单元格
                    if (timeRow.getChildCount() <= 1) {
                        Log.d("CalendarFragment", "时间行没有足够的子视图");
                        continue;
                    }
                    
                    // 获取单元格容器(可能是LinearLayout或include的布局)
                    View cellsContainer = timeRow.getChildAt(1);
                    Log.d("CalendarFragment", "单元格容器类型: " + cellsContainer.getClass().getSimpleName());
                    
                    // 如果是ViewGroup,则查找其中的单元格
                    if (cellsContainer instanceof ViewGroup) {
                        ViewGroup container = (ViewGroup) cellsContainer;
                        Log.d("CalendarFragment", "单元格容器包含 " + container.getChildCount() + " 个子视图");
                        
                        // 首先尝试通过tag查找
                        String targetTag = "day" + dayOfWeek;
                        for (int j = 0; j < container.getChildCount(); j++) {
                            View frameChild = container.getChildAt(j);
                            if (frameChild instanceof FrameLayout) {
                                String tag = (String) frameChild.getTag();
                                if (tag != null && tag.equals(targetTag)) {
                                    Log.d("CalendarFragment", "通过tag找到周视图单元格: " + tag);
                                    return (FrameLayout) frameChild;
                                }
                            }
                        }
                        
                        // 查找对应星期几的单元格
                        // 需要考虑布局中可能包含分隔线
                        int frameCount = 0;
                        for (int j = 0; j < container.getChildCount(); j++) {
                            View frameChild = container.getChildAt(j);
                            if (frameChild instanceof FrameLayout) {
                                if (frameCount == dayOfWeek) {
                                    Log.d("CalendarFragment", "找到周视图单元格: day=" + dayOfWeek);
                                    // 设置tag以便后续查找
                                    frameChild.setTag("day" + dayOfWeek);
                                    return (FrameLayout) frameChild;
                                }
                                frameCount++;
                            }
                        }
                    }
                    
                    // 如果在merged布局中找不到，尝试直接在行中查找
                    int frameCount = 0;
                    for (int j = 1; j < timeRow.getChildCount(); j++) {  // 跳过第一个时间标签
                        View child2 = timeRow.getChildAt(j);
                        Log.d("CalendarFragment", "检查时间行直接子视图 " + j + ": " + child2.getClass().getSimpleName());
                        
                        if (child2 instanceof FrameLayout) {
                            if (frameCount == dayOfWeek) {
                                Log.d("CalendarFragment", "在行中直接找到单元格: day=" + dayOfWeek);
                                // 设置tag以便后续查找
                                child2.setTag("day" + dayOfWeek);
                                return (FrameLayout) child2;
                            }
                            frameCount++;
                        } else if (child2 instanceof ViewGroup) {
                            // 可能是某种容器中包含了FrameLayout
                            ViewGroup vg = (ViewGroup) child2;
                            for (int k = 0; k < vg.getChildCount(); k++) {
                                View innerChild = vg.getChildAt(k);
                                if (innerChild instanceof FrameLayout && frameCount == dayOfWeek) {
                                    Log.d("CalendarFragment", "在内部ViewGroup中找到单元格: day=" + dayOfWeek);
                                    innerChild.setTag("day" + dayOfWeek);
                                    return (FrameLayout) innerChild;
                                }
                                if (innerChild instanceof FrameLayout) {
                                    frameCount++;
                                }
                            }
                        }
                    }
                }
            }
            
            Log.d("CalendarFragment", "找不到周视图单元格: day=" + dayOfWeek + ", hour=" + hour);
        } catch (Exception e) {
            Log.e("CalendarFragment", "查找周视图单元格出错: " + e.getMessage(), e);
        }

        return null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        // 记录之前的视图模式以便后续处理
        int previousViewMode = currentViewMode;

        if (id == R.id.btn_three_day) {
            currentViewMode = 0;
            showThreeDayView();
        } else if (id == R.id.btn_week) {
            currentViewMode = 1;
            showWeekView();
        } else if (id == R.id.btn_month) {
            currentViewMode = 2;
            showMonthView();
        }

        // 确保ScrollView的ID被正确设置
        initViewIds();

        updateViewModeButtons();
        
        // 如果从三日视图切换到周视图，需要特别处理
        if (previousViewMode == 0 && currentViewMode == 1) {
            // 增加一点延迟以确保视图切换完成
            threeDayView.post(() -> {
                initViewIds();  // 再次确保ID设置正确
                observeTasks(); // 更新任务显示
            });
        } else {
            observeTasks(); // 正常更新任务显示
        }
    }

    private void updateViewModeButtons() {
        // 重置所有按钮样式
        btnThreeDay.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnWeek.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnMonth.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // 设置选中按钮样式
        switch (currentViewMode) {
            case 0:
                btnThreeDay.setBackgroundResource(R.color.colorAccent);
                break;
            case 1:
                btnWeek.setBackgroundResource(R.color.colorAccent);
                break;
            case 2:
                btnMonth.setBackgroundResource(R.color.colorAccent);
                break;
        }
    }

    private void showThreeDayView() {
        threeDayView.setVisibility(View.VISIBLE);
        weekView.setVisibility(View.GONE);
        monthView.setVisibility(View.GONE);

        // 更新三日视图的日期标题
        updateThreeDayHeaders();
    }

    private void showWeekView() {
        threeDayView.setVisibility(View.GONE);
        weekView.setVisibility(View.VISIBLE);
        monthView.setVisibility(View.GONE);

        // 更新周视图的日期标题
        updateWeekHeaders();
    }

    private void showMonthView() {
        threeDayView.setVisibility(View.GONE);
        weekView.setVisibility(View.GONE);
        monthView.setVisibility(View.VISIBLE);

        // 设置CalendarView的日期
        calendarView.setDate(currentCalendar.getTimeInMillis());
    }

    /**
     * 更新三日视图的日期标题
     */
    private void updateThreeDayHeaders() {
        if (textDay1 == null || textDay2 == null || textDay3 == null) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日", Locale.getDefault());
        SimpleDateFormat sdfWeekday = new SimpleDateFormat("E", Locale.getDefault());

        // 创建临时Calendar对象，不修改currentCalendar
        Calendar tempCalendar = (Calendar) currentCalendar.clone();
        
        // 第一天（当前日期）
        Date day1 = tempCalendar.getTime();
        String day1Str = sdf.format(day1);
        String weekday1 = sdfWeekday.format(day1);
        textDay1.setText(day1Str + "\n" + weekday1);

        // 第二天
        tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
        Date day2 = tempCalendar.getTime();
        String day2Str = sdf.format(day2);
        String weekday2 = sdfWeekday.format(day2);
        textDay2.setText(day2Str + "\n" + weekday2);

        // 第三天
        tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
        Date day3 = tempCalendar.getTime();
        String day3Str = sdf.format(day3);
        String weekday3 = sdfWeekday.format(day3);
        textDay3.setText(day3Str + "\n" + weekday3);
        
        // 不需要恢复tempCalendar，因为它是currentCalendar的克隆
    }

    /**
     * 更新周视图的日期标题
     */
    private void updateWeekHeaders() {
        if (textWeekSun == null || textWeekMon == null || textWeekTue == null ||
                textWeekWed == null || textWeekThu == null || textWeekFri == null ||
                textWeekSat == null) {
            Log.e("CalendarFragment", "周视图的日期标题TextView为null");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd", Locale.getDefault());

        try {
            // 创建临时Calendar对象，不修改currentCalendar
            Calendar tempCalendar = (Calendar) currentCalendar.clone();
    
            // 设置为本周的周日
            tempCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
            tempCalendar.set(Calendar.MINUTE, 0);
            tempCalendar.set(Calendar.SECOND, 0);
            tempCalendar.set(Calendar.MILLISECOND, 0);
            
            // 高亮当前日期
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            Log.d("CalendarFragment", "更新周视图日期标题，从 " + sdf.format(tempCalendar.getTime()));
    
            // 周日
            Date dateSun = tempCalendar.getTime();
            String textSun = "日\n" + sdf.format(dateSun);
            textWeekSun.setText(textSun);
            highlightIfToday(textWeekSun, tempCalendar, today);
    
            // 周一
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
            Date dateMon = tempCalendar.getTime();
            String textMon = "一\n" + sdf.format(dateMon);
            textWeekMon.setText(textMon);
            highlightIfToday(textWeekMon, tempCalendar, today);
    
            // 周二
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
            Date dateTue = tempCalendar.getTime();
            String textTue = "二\n" + sdf.format(dateTue);
            textWeekTue.setText(textTue);
            highlightIfToday(textWeekTue, tempCalendar, today);
    
            // 周三
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
            Date dateWed = tempCalendar.getTime();
            String textWed = "三\n" + sdf.format(dateWed);
            textWeekWed.setText(textWed);
            highlightIfToday(textWeekWed, tempCalendar, today);
    
            // 周四
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
            Date dateThu = tempCalendar.getTime();
            String textThu = "四\n" + sdf.format(dateThu);
            textWeekThu.setText(textThu);
            highlightIfToday(textWeekThu, tempCalendar, today);
    
            // 周五
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
            Date dateFri = tempCalendar.getTime();
            String textFri = "五\n" + sdf.format(dateFri);
            textWeekFri.setText(textFri);
            highlightIfToday(textWeekFri, tempCalendar, today);
    
            // 周六
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
            Date dateSat = tempCalendar.getTime();
            String textSat = "六\n" + sdf.format(dateSat);
            textWeekSat.setText(textSat);
            highlightIfToday(textWeekSat, tempCalendar, today);
            
            Log.d("CalendarFragment", "周视图日期标题更新完成");
        } catch (Exception e) {
            Log.e("CalendarFragment", "更新周视图日期标题出错: " + e.getMessage(), e);
        }
    }

    // 高亮显示当天日期
    private void highlightIfToday(TextView textView, Calendar date, Calendar today) {
        if (date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                date.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
            
            textView.setTypeface(null, Typeface.BOLD);
            textView.setTextColor(getResources().getColor(R.color.colorAccent));
            textView.setBackgroundResource(R.color.colorPrimaryLight);
        } else {
            textView.setTypeface(null, Typeface.NORMAL);
            textView.setTextColor(getResources().getColor(android.R.color.black));
            textView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // 添加月视图任务显示方法

    /**
     * 显示月视图中选定日期的任务
     * 优化的任务展示方法，使用更美观的卡片样式和格式化文本
     */
    private void displayMonthTasks(List<Task> tasks) {
        if (tasks == null) {
            monthTaskIndicator.setVisibility(View.GONE);
            monthTaskBadge.setText("");
            return;
        }

        // 获取当前选中日期
        Date selectedDate = currentCalendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = dateFormat.format(selectedDate);

        // 筛选出当天的任务
        List<Task> todayTasks = new ArrayList<>();
        for (Task task : tasks) {
            String taskDateStr = dateFormat.format(task.getStartTime());
            if (taskDateStr.equals(selectedDateStr)) {
                todayTasks.add(task);
            }
        }

        // 显示当天任务数量
        if (!todayTasks.isEmpty()) {
            // 更新任务数量标志
            monthTaskBadge.setText(String.valueOf(todayTasks.size()));

            // 创建任务列表
            SpannableStringBuilder taskContent = new SpannableStringBuilder();

            // 按开始时间排序任务
            Collections.sort(todayTasks, new Comparator<Task>() {
                @Override
                public int compare(Task t1, Task t2) {
                    return t1.getStartTime().compareTo(t2.getStartTime());
                }
            });

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            // 显示所有任务，使用格式化的样式
            for (int i = 0; i < todayTasks.size(); i++) {
                Task task = todayTasks.get(i);

                // 添加分隔线，除了第一个任务
                if (i > 0) {
                    taskContent.append("\n\n").append("────────────────").append("\n\n");
                }

                // 添加时间
                String timeStr = timeFormat.format(task.getStartTime()) + " - " +
                        timeFormat.format(task.getEndTime());
                SpannableString timeSpan = new SpannableString(timeStr + "\n");
                timeSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimaryDark)),
                        0, timeSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                timeSpan.setSpan(new StyleSpan(Typeface.BOLD),
                        0, timeSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                taskContent.append(timeSpan);

                // 添加任务标题
                SpannableString titleSpan = new SpannableString(task.getTitle() + "\n");
                titleSpan.setSpan(new StyleSpan(Typeface.BOLD),
                        0, titleSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleSpan.setSpan(new RelativeSizeSpan(1.2f),
                        0, titleSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                taskContent.append(titleSpan);

                // 如果有地点，添加地点信息
                if (!TextUtils.isEmpty(task.getLocation())) {
                    SpannableString locationSpan = new SpannableString("📍 " + task.getLocation() + "\n");
                    locationSpan.setSpan(new ForegroundColorSpan(Color.GRAY),
                            0, locationSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    locationSpan.setSpan(new StyleSpan(Typeface.ITALIC),
                            0, locationSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    taskContent.append(locationSpan);
                }

                // 添加备注（如果有）
                if (!TextUtils.isEmpty(task.getNote())) {
                    SpannableString noteSpan = new SpannableString(task.getNote() + "\n");
                    noteSpan.setSpan(new ForegroundColorSpan(Color.GRAY),
                            0, noteSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    noteSpan.setSpan(new RelativeSizeSpan(0.9f),
                            0, noteSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    taskContent.append(noteSpan);
                }

                // 添加重要程度标签
                String importanceStr = "";
                int color = Color.WHITE;
                switch (task.getImportance()) {
                    case 1:
                        importanceStr = "重要且紧急";
                        color = getResources().getColor(R.color.colorQuadrant1);
                        break;
                    case 2:
                        importanceStr = "重要不紧急";
                        color = getResources().getColor(R.color.colorQuadrant2);
                        break;
                    case 3:
                        importanceStr = "紧急不重要";
                        color = getResources().getColor(R.color.colorQuadrant3);
                        break;
                    case 4:
                        importanceStr = "不紧急不重要";
                        color = getResources().getColor(R.color.colorQuadrant4);
                        break;
                }

                if (!TextUtils.isEmpty(importanceStr)) {
                    SpannableString importanceSpan = new SpannableString(importanceStr);
                    importanceSpan.setSpan(new BackgroundColorSpan(color),
                            0, importanceSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    importanceSpan.setSpan(new ForegroundColorSpan(Color.WHITE),
                            0, importanceSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    importanceSpan.setSpan(new StyleSpan(Typeface.BOLD),
                            0, importanceSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    taskContent.append(importanceSpan);
                }

                // 如果任务已完成，添加完成标志
                if (task.isCompleted()) {
                    taskContent.append("  ✓");
                }
            }

            monthTaskIndicator.setText(taskContent);
            monthTaskIndicator.setVisibility(View.VISIBLE);
        } else {
            // 没有任务时显示提示信息
            monthTaskBadge.setText("0");
            SpannableString noTasksSpan = new SpannableString("今天没有任务安排\n点击 + 按钮添加新任务");
            noTasksSpan.setSpan(new ForegroundColorSpan(Color.GRAY),
                    0, noTasksSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            noTasksSpan.setSpan(new StyleSpan(Typeface.ITALIC),
                    0, noTasksSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            noTasksSpan.setSpan(new RelativeSizeSpan(1.1f),
                    0, noTasksSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            noTasksSpan.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                    0, noTasksSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            monthTaskIndicator.setText(noTasksSpan);
            monthTaskIndicator.setVisibility(View.VISIBLE);
        }
    }     // 添加updateMonthTasksCount方法，更新月视图的任务计数
    private void updateMonthTasksCount(List<Task> tasks) {
        // 清空原有计数
        monthTasksCountByDate.clear();

        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        // 按日期统计任务
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (Task task : tasks) {
            String dateStr = dateFormat.format(task.getStartTime());
            // 替换 getOrDefault 方法，兼容 API 级别 22
            Integer count = monthTasksCountByDate.get(dateStr);
            if (count == null) {
                count = 0;
            }
            monthTasksCountByDate.put(dateStr, count + 1);
        }

        // 更新当前显示
        displayMonthTasks(tasks);
    }

    // 添加一个辅助方法来格式化日期键
    private String formatDateKey(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
    }

    private void optimizeCalendarViewForEInk() {
        if (calendarView == null) {
            return;
        }
    
        try {
            // 禁用滚动动画
            calendarView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            
            // 尝试通过反射获取内部ListView并优化
            try {
                // 尝试获取mListView字段
                Field listViewField = CalendarView.class.getDeclaredField("mListView");
                listViewField.setAccessible(true);
                Object listView = listViewField.get(calendarView);
                
                if (listView instanceof AbsListView) {
                    AbsListView absListView = (AbsListView) listView;
                    absListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                    absListView.setFriction(ViewConfiguration.getScrollFriction() * 4); // 增加摩擦力减缓滚动
                }
            } catch (NoSuchFieldException e) {
                // mListView字段不存在，尝试其他可能的字段名
                Log.d("CalendarFragment", "mListView字段不存在，尝试其他方法");
                
                // 尝试获取其他可能的字段
                String[] possibleFields = {"mDaySelector", "mMonthView", "mDayPickerView", "mYearPickerView"};
                for (String fieldName : possibleFields) {
                    try {
                        Field field = CalendarView.class.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object view = field.get(calendarView);
                        
                        if (view instanceof View) {
                            ((View) view).setOverScrollMode(View.OVER_SCROLL_NEVER);
                            Log.d("CalendarFragment", "成功优化字段: " + fieldName);
                        }
                    } catch (Exception ex) {
                        // 忽略此字段的错误
                    }
                }
                
                // 尝试使用通用方法查找并优化所有子视图
                disableScrollingEffectsRecursively(calendarView);
            } catch (Exception e) {
                // 其他反射错误，使用通用方法
                Log.d("CalendarFragment", "反射访问失败，使用通用方法优化");
                disableScrollingEffectsRecursively(calendarView);
            }
            
            // 设置日期选择时不显示动画
            try {
                Method setShowWeekTransitionMethod = CalendarView.class.getDeclaredMethod("setShowWeekTransition", boolean.class);
                setShowWeekTransitionMethod.setAccessible(true);
                setShowWeekTransitionMethod.invoke(calendarView, false);
            } catch (Exception e) {
                // 忽略此方法不存在的错误
            }
            
        } catch (Exception e) {
            Log.e("CalendarFragment", "优化CalendarView失败", e);
        }
    }

/**
 * 递归禁用视图中的滚动效果
 */
private void disableScrollingEffectsRecursively(View view) {
    if (view == null) return;
    
    // 禁用过度滚动
    view.setOverScrollMode(View.OVER_SCROLL_NEVER);
    
    // 对于特定类型的视图进行特殊处理
    if (view instanceof AbsListView) {
        AbsListView listView = (AbsListView) view;
        listView.setFriction(ViewConfiguration.getScrollFriction() * 4);
    } else if (view instanceof ScrollView) {
        ScrollView scrollView = (ScrollView) view;
        EInkDisplayHelper.disableScrollViewAnimations(scrollView);
    }
    
    // 递归处理子视图
    if (view instanceof ViewGroup) {
        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            disableScrollingEffectsRecursively(viewGroup.getChildAt(i));
        }
    }
}

/**
 * 初始化ScrollView ID，确保能正确找到
 */
private void initViewIds() {
    try {
        Log.d("CalendarFragment", "开始初始化视图ID");
        
        // 检查三日视图是否可用
        if (threeDayView != null) {
            // 为了确保能找到ScrollView，检查并设置ID
            ScrollView threeDayScrollView = threeDayView.findViewById(R.id.three_day_scrollview);
            if (threeDayScrollView == null) {
                // 如果找不到，尝试递归查找所有子视图中的ScrollView
                Log.d("CalendarFragment", "三日视图中找不到ID为three_day_scrollview的ScrollView，尝试递归查找");
                threeDayScrollView = findScrollViewRecursively(threeDayView);
                
                if (threeDayScrollView != null) {
                    threeDayScrollView.setId(R.id.three_day_scrollview);
                    Log.d("CalendarFragment", "为三日视图ScrollView设置ID成功");
                } else {
                    Log.e("CalendarFragment", "在三日视图中找不到任何ScrollView");
                }
            } else {
                Log.d("CalendarFragment", "三日视图ScrollView ID已存在");
            }
        }
        
        // 检查周视图是否可用
        if (weekView != null) {
            // 同样为周视图的ScrollView设置ID
            ScrollView weekScrollView = weekView.findViewById(R.id.week_scrollview);
            if (weekScrollView == null) {
                // 如果找不到，尝试递归查找所有子视图中的ScrollView
                Log.d("CalendarFragment", "周视图中找不到ID为week_scrollview的ScrollView，尝试递归查找");
                weekScrollView = findScrollViewRecursively(weekView);
                
                if (weekScrollView != null) {
                    weekScrollView.setId(R.id.week_scrollview);
                    Log.d("CalendarFragment", "为周视图ScrollView设置ID成功");
                } else {
                    Log.e("CalendarFragment", "在周视图中找不到任何ScrollView");
                }
            } else {
                Log.d("CalendarFragment", "周视图ScrollView ID已存在");
            }
        }
    } catch (Exception e) {
        Log.e("CalendarFragment", "初始化视图ID出错: " + e.getMessage(), e);
    }
}

/**
 * 递归查找ViewGroup中的第一个ScrollView
 * @param viewGroup 要搜索的ViewGroup
 * @return 找到的第一个ScrollView，如果没找到则返回null
 */
private ScrollView findScrollViewRecursively(ViewGroup viewGroup) {
    if (viewGroup == null) return null;
    
    // 先在直接子视图中查找
    for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = viewGroup.getChildAt(i);
        if (child instanceof ScrollView) {
            Log.d("CalendarFragment", "在直接子视图中找到ScrollView");
            return (ScrollView) child;
        }
    }
    
    // 如果直接子视图中没找到，递归查找更深层次的视图
    for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = viewGroup.getChildAt(i);
        if (child instanceof ViewGroup) {
            ScrollView found = findScrollViewRecursively((ViewGroup) child);
            if (found != null) {
                return found;
            }
        }
    }
    
    return null;
}

    @Override
    public void onResume() {
        super.onResume();
        
        // 视图显示时禁用动画
        View rootView = getView();
        if (rootView != null) {
            EInkAnimationUtils.disableAnimationsRecursively(rootView);
        }
        
        // 设置页面刷新模式
        setEInkRefreshMode();
    }

    /**
     * 设置适合水墨屏的刷新模式
     */
    private void setEInkRefreshMode() {
        // 获取根视图
        View rootView = getView();
        if (rootView == null) return;
        
        // 设置软件渲染
        rootView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        // 禁用动画缓存
        try {
            Field field = View.class.getDeclaredField("mAttachInfo");
            field.setAccessible(true);
            Object attachInfo = field.get(rootView);
            
            if (attachInfo != null) {
                Field animationScaleField = attachInfo.getClass().getDeclaredField("mApplicationScale");
                animationScaleField.setAccessible(true);
                animationScaleField.setFloat(attachInfo, 0.0f);
            }
        } catch (Exception e) {
            // 忽略反射错误
        }
    }
}