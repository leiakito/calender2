package com.stu.calender2.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.stu.calender2.data.Task;
import com.stu.calender2.data.TaskRepository;

import java.util.Date;
import java.util.List;

/**
 * 任务ViewModel类，连接UI和数据
 */
public class TaskViewModel extends AndroidViewModel {
    private TaskRepository repository;
    private LiveData<List<Task>> allTasks;
    private MutableLiveData<Task> selectedTask = new MutableLiveData<>();

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        allTasks = repository.getAllTasks();
    }

    // 获取所有任务
    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    // 获取某天的所有任务
    public LiveData<List<Task>> getTasksByDate(Date date) {
        return repository.getTasksByDate(date);
    }

    // 根据日期范围获取任务
    public LiveData<List<Task>> getTasksByDateRange(Date start, Date end) {
        return repository.getTasksByDateRange(start, end);
    }

    // 根据重要程度获取任务
    public LiveData<List<Task>> getTasksByImportance(int importance) {
        return repository.getTasksByImportance(importance);
    }

    // 获取未完成的任务
    public LiveData<List<Task>> getIncompleteTasks() {
        return repository.getIncompleteTasks();
    }

    // 插入任务
    public void insert(Task task, TaskRepository.OnTaskOperationListener listener) {
        repository.insert(task, listener);
    }

    // 更新任务
    public void update(Task task) {
        repository.update(task);
    }

    // 删除任务
    public void delete(Task task) {
        repository.delete(task);
    }

    // 根据ID获取任务
    public void getTaskById(long id, TaskRepository.OnTaskOperationListener listener) {
        repository.getTaskById(id, listener);
    }

    // 设置选中的任务
    public void selectTask(Task task) {
        selectedTask.setValue(task);
    }

    // 获取选中的任务
    public LiveData<Task> getSelectedTask() {
        return selectedTask;
    }
} 