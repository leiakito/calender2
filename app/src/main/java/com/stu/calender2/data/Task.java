package com.stu.calender2.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

/**
 * 任务实体类，用于Room数据库存储
 */
@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String title;         // 任务标题
    private Date startTime;       // 开始时间
    private Date endTime;         // 结束时间
    private String location;      // 地点
    private String note;          // 备注
    private int importance;       // 重要程度（1-4，对应四象限：1-重要紧急，2-重要不紧急，3-紧急不重要，4-不紧急不重要）
    private boolean completed;    // 是否已完成
    
    // 构造函数
    public Task(String title, Date startTime, Date endTime, String location, String note, int importance) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.note = note;
        this.importance = importance;
        this.completed = false;  // 默认未完成
    }

    // Getters和Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    // 根据重要程度获取四象限类别
    public String getQuadrantCategory() {
        switch (importance) {
            case 1:
                return "重要且紧急";
            case 2:
                return "重要不紧急";
            case 3:
                return "紧急不重要";
            case 4:
                return "不紧急不重要";
            default:
                return "未分类";
        }
    }
} 