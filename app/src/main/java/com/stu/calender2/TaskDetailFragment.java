package com.stu.calender2;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.stu.calender2.data.Task;
import com.stu.calender2.data.TaskRepository;
import com.stu.calender2.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 任务详情Fragment，用于添加/编辑任务
 */
public class TaskDetailFragment extends Fragment {

    private TaskViewModel taskViewModel;
    
    // UI元素
    private EditText editTitle;
    private TextView textStartTime;
    private TextView textEndTime;
    private EditText editLocation;
    private RadioGroup radioGroupImportance;
    private EditText editNote;
    private Button buttonSave;
    private Button buttonDelete;
    
    // 日期时间选择
    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    
    // 当前任务ID（编辑模式下有效）
    private long taskId = -1;
    private boolean isEditMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化ViewModel
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        
        // 初始化UI元素
        initViews(view);
        
        // 设置默认结束时间（开始时间+1小时）
        endCalendar.add(Calendar.HOUR_OF_DAY, 1);
        updateTimeDisplay();
        
        // 获取传递的参数
        if (getArguments() != null) {
            taskId = getArguments().getLong("taskId", -1);
            if (taskId != -1) {
                // 编辑模式
                isEditMode = true;
                loadTask(taskId);
                buttonDelete.setVisibility(View.VISIBLE);
            }
        }
        
        // 设置监听器
        setupListeners();
    }

    private void initViews(View view) {
        editTitle = view.findViewById(R.id.edit_task_title);
        textStartTime = view.findViewById(R.id.text_start_time);
        textEndTime = view.findViewById(R.id.text_end_time);
        editLocation = view.findViewById(R.id.edit_task_location);
        radioGroupImportance = view.findViewById(R.id.radio_group_importance);
        editNote = view.findViewById(R.id.edit_task_note);
        buttonSave = view.findViewById(R.id.button_save_task);
        buttonDelete = view.findViewById(R.id.button_delete_task);
        
        // 默认选择"重要且紧急"
        ((RadioButton) view.findViewById(R.id.radio_importance_1)).setChecked(true);
    }

    private void setupListeners() {
        // 开始时间选择
        textStartTime.setOnClickListener(v -> showDateTimePicker(true));
        
        // 结束时间选择
        textEndTime.setOnClickListener(v -> showDateTimePicker(false));
        
        // 保存按钮
        buttonSave.setOnClickListener(v -> saveTask());
        
        // 删除按钮
        buttonDelete.setOnClickListener(v -> deleteTask());
    }

    /**
     * 显示日期时间选择器
     * @param isStart 是否是开始时间
     */
    private void showDateTimePicker(boolean isStart) {
        final Calendar calendar = isStart ? startCalendar : endCalendar;
        
        // 日期选择器
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    // 时间选择器
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (view1, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                
                                // 如果是开始时间，则更新结束时间为开始时间+1小时
                                if (isStart) {
                                    endCalendar.setTimeInMillis(calendar.getTimeInMillis());
                                    endCalendar.add(Calendar.HOUR_OF_DAY, 1);
                                }
                                
                                // 更新显示
                                updateTimeDisplay();
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * 更新时间显示
     */
    private void updateTimeDisplay() {
        textStartTime.setText(dateFormat.format(startCalendar.getTime()));
        textEndTime.setText(dateFormat.format(endCalendar.getTime()));
    }

    /**
     * 加载任务数据
     * @param taskId 任务ID
     */
    private void loadTask(long taskId) {
        taskViewModel.getTaskById(taskId, new TaskRepository.OnTaskOperationListener() {
            @Override
            public void onTaskInserted(long taskId) {
                // 不需要处理
            }

            @Override
            public void onTaskLoaded(Task task) {
                if (task != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // 填充UI
                        editTitle.setText(task.getTitle());
                        editLocation.setText(task.getLocation());
                        editNote.setText(task.getNote());
                        
                        // 设置时间
                        startCalendar.setTime(task.getStartTime());
                        endCalendar.setTime(task.getEndTime());
                        updateTimeDisplay();
                        
                        // 设置重要程度
                        int importance = task.getImportance();
                        switch (importance) {
                            case 1:
                                ((RadioButton) requireView().findViewById(R.id.radio_importance_1)).setChecked(true);
                                break;
                            case 2:
                                ((RadioButton) requireView().findViewById(R.id.radio_importance_2)).setChecked(true);
                                break;
                            case 3:
                                ((RadioButton) requireView().findViewById(R.id.radio_importance_3)).setChecked(true);
                                break;
                            case 4:
                                ((RadioButton) requireView().findViewById(R.id.radio_importance_4)).setChecked(true);
                                break;
                        }
                    });
                }
            }
        });
    }

    /**
     * 保存任务
     */
    private void saveTask() {
        String title = editTitle.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String note = editNote.getText().toString().trim();
        
        // 表单验证
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "请输入任务标题", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取重要程度
        int importance = 1; // 默认重要且紧急
        int checkedRadioButtonId = radioGroupImportance.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.radio_importance_1) {
            importance = 1;
        } else if (checkedRadioButtonId == R.id.radio_importance_2) {
            importance = 2;
        } else if (checkedRadioButtonId == R.id.radio_importance_3) {
            importance = 3;
        } else if (checkedRadioButtonId == R.id.radio_importance_4) {
            importance = 4;
        }
        
        // 创建任务对象
        final Task task = new Task(
                title,
                startCalendar.getTime(),
                endCalendar.getTime(),
                location,
                note,
                importance
        );
        
        if (isEditMode) {
            // 编辑模式 - 更新任务
            task.setId(taskId);
            taskViewModel.update(task);
            Toast.makeText(requireContext(), "任务已更新", Toast.LENGTH_SHORT).show();
            navigateBack();
        } else {
            // 添加模式 - 插入任务
            taskViewModel.insert(task, new TaskRepository.OnTaskOperationListener() {
                @Override
                public void onTaskInserted(long newTaskId) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "任务已保存", Toast.LENGTH_SHORT).show();
                            navigateBack();
                        });
                    }
                }

                @Override
                public void onTaskLoaded(Task task) {
                    // 不需要处理
                }
            });
        }
    }

    /**
     * 删除任务
     */
    private void deleteTask() {
        if (isEditMode) {
            taskViewModel.getTaskById(taskId, new TaskRepository.OnTaskOperationListener() {
                @Override
                public void onTaskInserted(long taskId) {
                    // 不需要处理
                }

                @Override
                public void onTaskLoaded(Task task) {
                    if (task != null && isAdded()) {
                        taskViewModel.delete(task);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "任务已删除", Toast.LENGTH_SHORT).show();
                            navigateBack();
                        });
                    }
                }
            });
        }
    }

    /**
     * 返回上一页
     */
    private void navigateBack() {
        try {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "导航返回失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            
            // 如果导航失败，尝试使用Activity的onBackPressed
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }
} 