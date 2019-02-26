# HijackDemo
替换当前手机栈顶应用(com.tencent.mobileqq)的activity

需要
```
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
 tools:ignore="ProtectedPermissions" />
 ```


* 参考
- [AndroidProcesses](https://github.com/jaredrummler/AndroidProcesses)

- [AndroidProcess](https://github.com/wenmingvs/AndroidProcess)



- 低于5.0：使用getRunningTasks

  Android 5.0+ killed getRunningTasks(int) and getRunningAppProcesses(). Both of these methods are now deprecated and only return the caller’s application process.

  Android 5.0 introduced UsageStatsManager which provides access to device usage history and statistics. This API requires the permission android.permission.PACKAGE_USAGE_STATS, which is a system-level permission and will not be granted to third-party apps. However,
  declaring the permission implies intention to use the API and the user of the device can grant permission through the Settings application.


