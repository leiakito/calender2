package com.stu.calender2.data;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 任务仓库类，封装数据库操作
 */
public class TaskRepository {
    private TaskDao taskDao;
    private LiveData<List<Task>> allTasks;
    private ExecutorService executorService;

    public TaskRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        allTasks = taskDao.getAllTasks();
        executorService = Executors.newSingleThreadExecutor();
    }

    // 获取所有任务
    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    // 获取某天的所有任务
    public LiveData<List<Task>> getTasksByDate(Date date) {
        return taskDao.getTasksByDate(date);
    }

    // 根据日期范围获取任务
    public LiveData<List<Task>> getTasksByDateRange(Date start, Date end) {
        return taskDao.getTasksByDateRange(start, end);
    }

    // 根据重要程度获取任务
    public LiveData<List<Task>> getTasksByImportance(int importance) {
        return taskDao.getTasksByImportance(importance);
    }

    // 获取未完成的任务
    public LiveData<List<Task>> getIncompleteTasks() {
        return taskDao.getIncompleteTasks();
    }

    // 插入任务
    public void insert(Task task, OnTaskOperationListener listener) {
        executorService.execute(() -> {
            long id = taskDao.insert(task);
            if (listener != null) {
                listener.onTaskInserted(id);
            }
        });
    }

    // 更新任务
    public void update(Task task) {
        executorService.execute(() -> {
            taskDao.update(task);
        });
    }

    // 删除任务
    public void delete(Task task) {
        executorService.execute(() -> {
            taskDao.delete(task);
        });
    }

    // 根据ID获取任务
    public void getTaskById(long id, OnTaskOperationListener listener) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(id);
            if (listener != null) {
                listener.onTaskLoaded(task);
            }
        });
    }

    // 任务操作监听器接口
    public interface OnTaskOperationListener {
        void onTaskInserted(long taskId);
        void onTaskLoaded(Task task);
    }
} 