package com.stu.calender2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stu.calender2.adapter.TaskAdapter;
import com.stu.calender2.data.Task;
import com.stu.calender2.viewmodel.TaskViewModel;

/**
 * 任务列表Fragment
 */
public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {
    
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private TaskViewModel taskViewModel;
    private FloatingActionButton fabAddTask;
    private FloatingActionButton fabSettings;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化ViewModel
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        
        // 初始化UI
        recyclerView = view.findViewById(R.id.recycler_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);

        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(requireContext(), this);
        recyclerView.setAdapter(adapter);
        
        // 监听LiveData
        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            adapter.setTasks(tasks);
        });
        
        // 添加任务按钮点击事件
        fabAddTask.setOnClickListener(v -> {
            navigateToTaskDetail(-1);
        });
        

    }
    
    /**
     * 显示应用信息
     */

    /**
     * 导航到任务详情
     * @param taskId 任务ID，-1表示新任务
     */
    private void navigateToTaskDetail(long taskId) {
        try {
            NavController navController = NavHostFragment.findNavController(this);
            Bundle bundle = new Bundle();
            if (taskId != -1) {
                bundle.putLong("taskId", taskId);
            }
            navController.navigate(R.id.action_tasks_to_task_detail, bundle);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "导航错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskClick(Task task) {
        // 点击任务项，跳转到任务详情
        navigateToTaskDetail(task.getId());
    }

    @Override
    public void onTaskToggleCompletion(Task task) {
        // 修改任务完成状态
        task.setCompleted(!task.isCompleted());
        taskViewModel.update(task);
        
        if (task.isCompleted()) {
            Toast.makeText(requireContext(), "任务已完成", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "任务已恢复", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDeleteTaskClick(Task task) {
        // 弹出确认对话框
        new AlertDialog.Builder(requireContext())
            .setTitle("删除任务")
            .setMessage("确定要删除这个任务吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                // 确认删除
                taskViewModel.delete(task);
                Toast.makeText(requireContext(), "任务已删除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }
} 