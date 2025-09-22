package com.stu.calender2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.stu.calender2.service.KeepAliveService;
import com.stu.calender2.utils.EInkDisplayHelper;
import com.stu.calender2.utils.EInkAnimationUtils;
import com.stu.calender2.utils.UIPerformanceOptimizer;
import com.stu.calender2.utils.DatabaseOptimizer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton btnCalendar;
    private ImageButton btnTasks;
    private NavController navController;

    // 添加全局触摸事件监听器
    private long lastRefreshTime = 0;
    private static final long MIN_REFRESH_INTERVAL = 1200; // 增加刷新间隔，降低频率
    private Runnable pendingRefreshRunnable = null;
    
    // 权限请求码
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 1001;
    
    // UI性能优化器
    private UIPerformanceOptimizer uiOptimizer;
    
    // 数据库优化器
    private DatabaseOptimizer dbOptimizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setWindowAnimations(0);
        super.onCreate(savedInstanceState);
        
        // 初始化UI性能优化器（在setContentView之前）
        uiOptimizer = UIPerformanceOptimizer.getInstance();
        uiOptimizer.setEInkOptimizationLevel(1); // 设置适中优化级别
        
        // 初始化数据库优化器
        dbOptimizer = DatabaseOptimizer.getInstance();
        
        setContentView(R.layout.activity_main);
        
        // 注册Activity以应用UI优化
        uiOptimizer.registerActivity(this);
        
        // 设置最佳刷新率
        uiOptimizer.setOptimalRefreshRate(this);
        
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // 启动常驻后台服务
        startKeepAliveService();
        
        // 请求忽略电池优化
        requestIgnoreBatteryOptimizations();
        
        // Fix NavController setup - replace the current implementation with this:
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // Create navigation options with no animations
            NavOptions navOptions = new NavOptions.Builder()
                    .setEnterAnim(0)
                    .setExitAnim(0)
                    .setPopEnterAnim(0)
                    .setPopExitAnim(0)
                    .build();
                    
            // We'll use these options when navigating
        } else {
            Log.e("MainActivity", "NavHostFragment not found!");
        }
        
        // 替换这两行:
        // navController.setGraph(navController.getGraph(), navOptions);
        // navController.setNavOptions(navOptions);
        
        // 使用这种方式:
        NavGraph graph = navController.getGraph();
        navController.setGraph(graph);
        
        // 如果需要应用导航选项，可以在导航时使用:
        // 例如，当你需要导航到某个目的地时:
        // navController.navigate(R.id.destination_id, bundle, navOptions);
        
        // If setNavOptions is not available, use this alternative:
        // NavInflater inflater = navController.getNavInflater();
        // NavGraph graph = inflater.inflate(R.navigation.nav_graph);
        // navController.setGraph(graph);
        // Then apply the options when navigating
        
        // 禁用动画
        final View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            EInkAnimationUtils.disableAnimationsRecursively(contentView);
        }

        // 应用水墨屏优化
        EInkDisplayHelper.applyEInkOptimizations(this);

        // 设置全屏和横屏显示
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // 设置状态栏和导航栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            
            // 设置状态栏和导航栏图标为深色，仅在Android 6.0及以上支持
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
                getWindow().setNavigationBarColor(Color.TRANSPARENT);
            }
        }
        
        // 初始化导航按钮
        btnCalendar = findViewById(R.id.btn_calendar);
        btnTasks = findViewById(R.id.btn_tasks);

        // 设置点击监听器
        btnCalendar.setOnClickListener(this);
        btnTasks.setOnClickListener(this);
        
        // 初始化按钮状态
        resetButtonState();
        updateButtonState(btnCalendar);
        
        // 预加载未来7天的任务到缓存中，提高性能
        preloadUpcomingTasks();
        
        // 查找并优化所有RecyclerView
        optimizeRecyclerViews(contentView);
        
        Log.d("MainActivity", "UI性能优化完成。缓存状态: " + dbOptimizer.getCacheStats());
    }
    
    /**
     * 预加载未来7天的任务到缓存
     */
    private void preloadUpcomingTasks() {
        long currentTime = System.currentTimeMillis();
        // 一天的毫秒数
        long dayInMillis = 24 * 60 * 60 * 1000;
        // 预加载7天的数据
        long endTime = currentTime + (7 * dayInMillis);
        
        // 异步预加载
        dbOptimizer.preloadTasksByDateRange(currentTime, endTime);
    }
    
    /**
     * 递归查找并优化所有RecyclerView
     */
    private void optimizeRecyclerViews(View view) {
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            uiOptimizer.optimizeRecyclerView(recyclerView);
            Log.d("MainActivity", "已优化RecyclerView: " + recyclerView.getId());
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                optimizeRecyclerViews(viewGroup.getChildAt(i));
            }
        }
    }
    
    /**
     * 启动常驻后台服务
     */
    private void startKeepAliveService() {
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    
    /**
     * 请求忽略电池优化
     */
    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName);
            
            if (!isIgnoringBatteryOptimizations) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName());
                
                if (isIgnoringBatteryOptimizations) {
                    Toast.makeText(this, "已忽略电池优化，应用将保持运行", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "未忽略电池优化，应用在后台可能被系统关闭", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已授予，应用将保持运行", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "权限被拒绝，应用在后台可能被系统关闭", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // 再次确保所有视图的动画被禁用
            View rootView = findViewById(android.R.id.content);
            EInkDisplayHelper.disableAnimationsRecursively(rootView);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 仅在用户完成操作后进行刷新，减少频繁重绘
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            // 用户松开手指时触发刷新，但要控制频率
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRefreshTime > MIN_REFRESH_INTERVAL) {
                // 取消之前的刷新任务
                if (pendingRefreshRunnable != null) {
                    new android.os.Handler().removeCallbacks(pendingRefreshRunnable);
                }
                
                // 创建新的刷新任务，使用UIPerformanceOptimizer延迟更新
                pendingRefreshRunnable = () -> {
                    // 改用优化器提供的延迟更新功能
                    View focusedView = getCurrentFocus();
                    if (focusedView != null) {
                        // 使用优化器延迟更新视图
                        uiOptimizer.scheduleDelayedUpdate(focusedView);
                        
                        // 同时更新父视图
                        ViewGroup parent = (ViewGroup) focusedView.getParent();
                        if (parent != null) {
                            uiOptimizer.scheduleDelayedUpdate(parent);
                        }
                    } else {
                        // 如果没有焦点视图则进行轻量级刷新
                        triggerLightRefresh();
                    }
                    
                    lastRefreshTime = System.currentTimeMillis();
                    pendingRefreshRunnable = null;
                };
                
                // 添加延迟，减少刷新频率
                new Handler().postDelayed(pendingRefreshRunnable, 150);
            }
        }
        
        return super.dispatchTouchEvent(ev);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 重新注册Activity以确保优化生效
        uiOptimizer.registerActivity(this);
    }
    
    @Override
    public void onBackPressed() {
        // 阻止返回键，避免用户意外退出应用
        Toast.makeText(this, "按Home键最小化应用", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清除所有待处理的UI更新
        uiOptimizer.clearPendingUpdates();
        
        // 提交所有待处理的数据库写入
        dbOptimizer.flushPendingWrites();
        
        // 关闭数据库优化器
        dbOptimizer.shutdown();
    }
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        
        // 重置所有按钮状态
        resetButtonState();
        
        if (id == R.id.btn_calendar) {
            updateButtonState(btnCalendar);
            // 使用预设的无动画导航选项
            navController.navigate(R.id.calendarFragment);
        } else if (id == R.id.btn_tasks) {
            updateButtonState(btnTasks);
            navController.navigate(R.id.tasksFragment);
        }
        
        // 延迟刷新以确保UI已更新
        new Handler().postDelayed(this::triggerLightRefresh, 100);
    }
    
    private void updateButtonState(ImageButton button) {
        button.setSelected(true);
        // 使用优化器延迟更新按钮视图
        uiOptimizer.scheduleDelayedUpdate(button);
    }
    
    private void resetButtonState() {
        btnCalendar.setSelected(false);
        btnTasks.setSelected(false);
        
        // 优化批量更新按钮
        View buttonContainer = (View) btnCalendar.getParent();
        if (buttonContainer != null) {
            uiOptimizer.scheduleDelayedUpdate(buttonContainer);
        }
    }
    
    // 轻量级UI刷新 - 不刷新整个画面，只刷新活动区域
    private void triggerLightRefresh() {
        View currentFragment = null;
        
        // 获取当前显示的Fragment视图
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null && navHostFragment.getChildFragmentManager().getFragments().size() > 0) {
            currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0).getView();
        }
        
        if (currentFragment != null) {
            // 使用优化器延迟更新当前Fragment
            uiOptimizer.scheduleDelayedUpdate(currentFragment);
        } else {
            // 如果找不到当前Fragment，则刷新左侧导航按钮区域
            View navigationPanel = findViewById(R.id.left_navigation_panel);
            if (navigationPanel != null) {
                uiOptimizer.scheduleDelayedUpdate(navigationPanel);
            }
        }
        
        lastRefreshTime = System.currentTimeMillis();
    }
    
    // 保留原有的全局刷新方法，但改为使用UIPerformanceOptimizer
    private void triggerGlobalRefresh() {
        Log.d("MainActivity", "触发全局刷新");
        View rootView = getWindow().getDecorView().getRootView();
        uiOptimizer.scheduleDelayedUpdate(rootView);
        lastRefreshTime = System.currentTimeMillis();
    }
    
    // 保留原有的完全刷新方法以备不时之需
    private void triggerFullRefresh() {
        Log.d("MainActivity", "触发完全刷新");
        
        // 获取根视图
        final View rootView = getWindow().getDecorView().getRootView();
        
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 确保只执行一次
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                
                // 强制重绘整个视图层次结构
                rootView.invalidate();
                
                // 记录最后刷新时间
                lastRefreshTime = System.currentTimeMillis();
            }
        });
        
        // 触发布局变化
        rootView.requestLayout();
    }
}