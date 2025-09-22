package com.stu.calender2.utils;

import android.util.Log;
import android.util.LruCache;

import com.stu.calender2.MyApplication;
import com.stu.calender2.data.AppDatabase;
import com.stu.calender2.data.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 数据库操作优化工具类
 * 提供缓存机制和批量操作功能，减少数据库IO操作
 */
public class DatabaseOptimizer {
    private static final String TAG = "DatabaseOptimizer";
    
    // 单例模式
    private static volatile DatabaseOptimizer instance;
    
    // 任务缓存，增大缓存容量以提高性能
    private final LruCache<Long, Task> taskCache;
    
    // 数据库实例引用
    private final AppDatabase database;
    
    // 线程池，用于异步操作
    private final ExecutorService executor;
    
    // 批量操作的分批大小
    private static final int BATCH_SIZE = 50;
    
    // 缓存大小
    private static final int CACHE_SIZE = 200;
    
    // 延迟写入时间（毫秒）
    private static final long WRITE_DELAY_MS = 300;
    
    // 用于跟踪延迟写入操作
    private final Map<String, Runnable> pendingWrites = new ConcurrentHashMap<>();
    
    // 私有构造函数，确保单例模式
    private DatabaseOptimizer() {
        database = MyApplication.getInstance().getDatabase();
        
        // 创建固定大小的线程池，适合I/O操作
        executor = Executors.newFixedThreadPool(4);
        
        // 创建缓存
        taskCache = new LruCache<>(CACHE_SIZE);
        
        Log.d(TAG, "数据库优化器初始化完成，缓存大小: " + CACHE_SIZE);
    }
    
    // 获取单例实例，使用双重检查锁定确保线程安全
    public static DatabaseOptimizer getInstance() {
        if (instance == null) {
            synchronized (DatabaseOptimizer.class) {
                if (instance == null) {
                    instance = new DatabaseOptimizer();
                }
            }
        }
        return instance;
    }
    
    /**
     * 关闭资源
     * 应在应用程序结束时调用
     */
    public void shutdown() {
        try {
            // 执行所有待处理的写操作
            for (Runnable pendingWrite : pendingWrites.values()) {
                pendingWrite.run();
            }
            pendingWrites.clear();
            
            // 关闭线程池
            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            
            // 清除缓存
            taskCache.evictAll();
            
            Log.d(TAG, "数据库优化器已关闭");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "关闭数据库优化器时发生中断", e);
        }
    }
    
    /**
     * 获取任务，优先从缓存中读取
     */
    public void getTask(long taskId, TaskCallback callback) {
        // 先检查缓存
        Task cachedTask = taskCache.get(taskId);
        if (cachedTask != null) {
            Log.d(TAG, "从缓存获取任务: " + taskId);
            callback.onTaskLoaded(cachedTask);
            return;
        }
        
        // 缓存未命中，从数据库加载
        executor.execute(() -> {
            Task task = database.taskDao().getTaskById(taskId);
            if (task != null) {
                // 加入缓存
                taskCache.put(taskId, task);
                // 回调结果
                MyApplication.getInstance().postToMainThread(() -> 
                    callback.onTaskLoaded(task)
                );
            } else {
                MyApplication.getInstance().postToMainThread(() -> 
                    callback.onDataNotAvailable()
                );
            }
        });
    }
    
    /**
     * 根据日期获取任务列表
     */
    public void getTasksByDate(long date, TasksCallback callback) {
        executor.execute(() -> {
            // 使用非LiveData版本的DAO方法
            List<Task> tasks = database.taskDao().getTasksByDate(date);
            
            if (tasks != null) {
                // 更新缓存
                for (Task task : tasks) {
                    taskCache.put(task.getId(), task);
                }
                
                // 返回结果
                MyApplication.getInstance().postToMainThread(() -> 
                    callback.onTasksLoaded(tasks)
                );
            } else {
                MyApplication.getInstance().postToMainThread(() -> 
                    callback.onTasksLoaded(new ArrayList<>())
                );
            }
        });
    }
    
    /**
     * 预加载指定日期范围内的任务到缓存
     * 适用于提前加载接下来几天的任务
     */
    public void preloadTasksByDateRange(long startDate, long endDate) {
        executor.execute(() -> {
            // 使用添加的非LiveData方法
            List<Task> tasks = database.taskDao().getTasksBetweenDates(startDate, endDate);
            if (tasks != null) {
                Log.d(TAG, "预加载任务到缓存: " + tasks.size() + "条");
                for (Task task : tasks) {
                    taskCache.put(task.getId(), task);
                }
            }
        });
    }
    
    /**
     * 批量获取所有任务
     */
    public void getAllTasks(TasksCallback callback) {
        executor.execute(() -> {
            // 使用非LiveData版本的方法
            List<Task> tasks = database.taskDao().getAllTasksList();
            
            if(tasks != null) {
                // 更新缓存
                for (Task task : tasks) {
                    taskCache.put(task.getId(), task);
                }
                
                // 返回结果
                MyApplication.getInstance().postToMainThread(() -> 
                    callback.onTasksLoaded(tasks)
                );
            } else {
                MyApplication.getInstance().postToMainThread(() -> 
                    callback.onTasksLoaded(new ArrayList<>())
                );
            }
        });
    }
    
    /**
     * 更新任务，使用延迟写入策略
     * 短时间内多次更新同一任务只会触发一次数据库写入
     */
    public void updateTask(Task task, OperationCallback callback) {
        // 先更新缓存
        taskCache.put(task.getId(), task);
        
        // 创建唯一标识符
        final String operationId = "update_" + task.getId();
        
        // 取消之前的待处理更新（如果有）
        Runnable previousUpdate = pendingWrites.remove(operationId);
        if (previousUpdate != null) {
            MyApplication.getInstance().removeMainThreadCallback(previousUpdate);
        }
        
        Runnable updateRunnable = () -> {
            executor.execute(() -> {
                // 更新数据库
                database.taskDao().update(task);
                
                // 返回结果
                if (callback != null) {
                    MyApplication.getInstance().postToMainThread(callback::onSuccess);
                }
                
                // 操作完成后从待处理列表中移除
                pendingWrites.remove(operationId);
            });
        };
        
        // 添加到待处理列表并延迟执行
        pendingWrites.put(operationId, updateRunnable);
        MyApplication.getInstance().postToMainThreadDelayed(updateRunnable, WRITE_DELAY_MS);
    }
    
    /**
     * 批量更新任务，分批处理以避免ANR
     */
    public void updateTasks(List<Task> tasks, OperationCallback callback) {
        if (tasks == null || tasks.isEmpty()) {
            if (callback != null) {
                MyApplication.getInstance().postToMainThread(callback::onSuccess);
            }
            return;
        }
        
        // 先更新缓存
        for (Task task : tasks) {
            taskCache.put(task.getId(), task);
        }
        
        executor.execute(() -> {
            // 分批处理
            for (int i = 0; i < tasks.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, tasks.size());
                List<Task> batch = tasks.subList(i, end);
                
                // 转换为数组以便批量更新
                Task[] taskArray = batch.toArray(new Task[0]);
                database.taskDao().updateAll(taskArray);
            }
            
            // 返回结果
            if (callback != null) {
                MyApplication.getInstance().postToMainThread(callback::onSuccess);
            }
        });
    }
    
    /**
     * 插入任务，使用延迟写入策略
     */
    public void insertTask(Task task, InsertCallback callback) {
        // 创建唯一标识符
        final String operationId = "insert_" + System.currentTimeMillis();
        
        Runnable insertRunnable = () -> {
            executor.execute(() -> {
                // 插入数据库
                long id = database.taskDao().insert(task);
                
                // 设置生成的ID
                task.setId(id);
                
                // 更新缓存
                taskCache.put(id, task);
                
                // 返回结果
                if (callback != null) {
                    MyApplication.getInstance().postToMainThread(() -> 
                        callback.onTaskInserted(id)
                    );
                }
                
                // 操作完成后从待处理列表中移除
                pendingWrites.remove(operationId);
            });
        };
        
        // 添加到待处理列表并延迟执行
        pendingWrites.put(operationId, insertRunnable);
        MyApplication.getInstance().postToMainThreadDelayed(insertRunnable, WRITE_DELAY_MS);
    }
    
    /**
     * 批量插入任务，分批处理
     */
    public void insertTasks(List<Task> tasks, BatchOperationCallback callback) {
        if (tasks == null || tasks.isEmpty()) {
            if (callback != null) {
                MyApplication.getInstance().postToMainThread(() -> callback.onComplete(true, 0));
            }
            return;
        }
        
        executor.execute(() -> {
            int totalInserted = 0;
            boolean success = true;
            
            // 分批处理
            for (int i = 0; i < tasks.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, tasks.size());
                List<Task> batch = tasks.subList(i, end);
                
                try {
                    // 转换为数组以便批量插入
                    Task[] taskArray = batch.toArray(new Task[0]);
                    long[] ids = database.taskDao().insertAll(taskArray);
                    
                    // 更新缓存和ID
                    for (int j = 0; j < ids.length; j++) {
                        if (ids[j] > 0) {
                            Task task = batch.get(j);
                            task.setId(ids[j]);
                            taskCache.put(ids[j], task);
                            totalInserted++;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "批量插入任务失败", e);
                    success = false;
                }
            }
            
            final boolean finalSuccess = success;
            final int finalTotal = totalInserted;
            
            if (callback != null) {
                MyApplication.getInstance().postToMainThread(() -> 
                    callback.onComplete(finalSuccess, finalTotal)
                );
            }
        });
    }
    
    /**
     * 删除任务，使用延迟写入策略
     */
    public void deleteTask(Task task, OperationCallback callback) {
        // 先从缓存中移除
        taskCache.remove(task.getId());
        
        // 创建唯一标识符
        final String operationId = "delete_" + task.getId();
        
        Runnable deleteRunnable = () -> {
            executor.execute(() -> {
                // 从数据库删除
                database.taskDao().delete(task);
                
                // 返回结果
                if (callback != null) {
                    MyApplication.getInstance().postToMainThread(callback::onSuccess);
                }
                
                // 操作完成后从待处理列表中移除
                pendingWrites.remove(operationId);
            });
        };
        
        // 添加到待处理列表并延迟执行
        pendingWrites.put(operationId, deleteRunnable);
        MyApplication.getInstance().postToMainThreadDelayed(deleteRunnable, WRITE_DELAY_MS);
    }
    
    /**
     * 批量删除任务，分批处理
     */
    public void deleteTasks(List<Task> tasks, BatchOperationCallback callback) {
        if (tasks == null || tasks.isEmpty()) {
            if (callback != null) {
                MyApplication.getInstance().postToMainThread(() -> callback.onComplete(true, 0));
            }
            return;
        }
        
        // 先从缓存中移除
        for (Task task : tasks) {
            taskCache.remove(task.getId());
        }
        
        executor.execute(() -> {
            int totalDeleted = 0;
            boolean success = true;
            
            // 分批处理
            for (int i = 0; i < tasks.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, tasks.size());
                List<Task> batch = tasks.subList(i, end);
                
                try {
                    Task[] taskArray = batch.toArray(new Task[0]);
                    int rowsAffected = database.taskDao().deleteAll(taskArray);
                    totalDeleted += rowsAffected;
                } catch (Exception e) {
                    Log.e(TAG, "批量删除任务失败", e);
                    success = false;
                }
            }
            
            final boolean finalSuccess = success;
            final int finalTotal = totalDeleted;
            
            if (callback != null) {
                MyApplication.getInstance().postToMainThread(() -> 
                    callback.onComplete(finalSuccess, finalTotal)
                );
            }
        });
    }
    
    /**
     * 提交所有待处理的写操作
     * 在应用关闭或需要立即持久化数据时调用
     */
    public void flushPendingWrites() {
        Log.d(TAG, "提交所有待处理的写操作: " + pendingWrites.size() + "个");
        
        for (Runnable operation : pendingWrites.values()) {
            MyApplication.getInstance().removeMainThreadCallback(operation);
            operation.run();
        }
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        taskCache.evictAll();
        Log.d(TAG, "任务缓存已清空");
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return "缓存大小: " + taskCache.size() + "/" + taskCache.maxSize() +
               ", 命中次数: " + taskCache.hitCount() +
               ", 未命中次数: " + taskCache.missCount();
    }
    
    /**
     * 任务加载回调接口
     */
    public interface TaskCallback {
        void onTaskLoaded(Task task);
        void onDataNotAvailable();
    }
    
    /**
     * 任务批量加载回调接口
     */
    public interface TasksCallback {
        void onTasksLoaded(List<Task> tasks);
    }
    
    /**
     * 操作回调接口
     */
    public interface OperationCallback {
        void onSuccess();
    }
    
    /**
     * 插入操作回调接口
     */
    public interface InsertCallback {
        void onTaskInserted(long taskId);
    }
    
    /**
     * 批量操作回调接口
     */
    public interface BatchOperationCallback {
        void onComplete(boolean success, int count);
    }
} 