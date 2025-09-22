package com.stu.calender2.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * 应用数据库类
 */
@Database(entities = {Task.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    // 单例模式
    private static volatile AppDatabase instance;
    
    // 获取DAO
    public abstract TaskDao taskDao();
    
    // 获取数据库实例
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "app_database")
                    .fallbackToDestructiveMigration()
                    // 启用WAL模式，提高写入性能
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    // 允许主线程查询（仅用于简单查询，复杂操作仍应使用异步）
                    .allowMainThreadQueries()
                    // 设置数据库升级的回调
                    .addCallback(new Callback() {
                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            // 使用rawQuery代替execSQL执行PRAGMA命令
                            db.query("PRAGMA synchronous = NORMAL").close();
                            db.query("PRAGMA journal_size_limit = 1048576").close();
                            db.query("PRAGMA temp_store = MEMORY").close();
                            db.query("PRAGMA cache_size = 1000").close();
                        }
                    })
                    .build();
        }
        return instance;
    }
    
    /**
     * 关闭数据库连接
     * 在应用退出时调用，以释放资源
     */
    public static void closeDatabase() {
        if (instance != null && instance.isOpen()) {
            instance.close();
            instance = null;
        }
    }
    
    /**
     * 检查数据库是否打开
     */
    public boolean isOpen() {
        SupportSQLiteDatabase db = this.getOpenHelper().getWritableDatabase();
        return db.isOpen();
    }
} 