package com.stu.calender2.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface TaskDao {
    // 插入任务
    @Insert
    long insert(Task task);

    // 批量插入任务
    @Insert
    long[] insertAll(Task... tasks);

    // 更新任务
    @Update
    void update(Task task);

    // 批量更新任务
    @Update
    void updateAll(Task... tasks);

    // 删除任务
    @Delete
    void delete(Task task);

    // 批量删除任务
    @Delete
    int deleteAll(Task... tasks);

    // 获取所有任务
    @Query("SELECT * FROM tasks ORDER BY startTime ASC")
    LiveData<List<Task>> getAllTasks();

    // 获取所有任务（非LiveData）
    @Query("SELECT * FROM tasks ORDER BY startTime ASC")
    List<Task> getAllTasksList();

    // 获取某天的所有任务
    @Query("SELECT * FROM tasks WHERE date(startTime/1000, 'unixepoch', 'localtime') = date(:date/1000, 'unixepoch', 'localtime') ORDER BY startTime ASC")
    LiveData<List<Task>> getTasksByDate(Date date);

    // 获取某天的所有任务（使用long时间戳，毫秒）
    @Query("SELECT * FROM tasks WHERE date(startTime/1000, 'unixepoch', 'localtime') = date(:date/1000, 'unixepoch', 'localtime') ORDER BY startTime ASC")
    List<Task> getTasksByDate(long date);

    // 根据日期范围获取任务
    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :start AND :end ORDER BY startTime ASC")
    LiveData<List<Task>> getTasksByDateRange(Date start, Date end);

    // 根据日期范围获取任务（使用long时间戳，毫秒）
    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :start AND :end ORDER BY startTime ASC")
    List<Task> getTasksBetweenDates(long start, long end);

    // 根据重要程度获取任务
    @Query("SELECT * FROM tasks WHERE importance = :importance ORDER BY startTime ASC")
    LiveData<List<Task>> getTasksByImportance(int importance);

    // 获取未完成的任务
    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY startTime ASC")
    LiveData<List<Task>> getIncompleteTasks();

    // 根据ID获取任务
    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getTaskById(long id);
} 