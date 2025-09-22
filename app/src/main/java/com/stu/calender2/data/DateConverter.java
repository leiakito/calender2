package com.stu.calender2.data;

import androidx.room.TypeConverter;
import java.util.Date;

/**
 * 日期转换器，用于Room数据库中Date类型的转换
 */
public class DateConverter {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
} 