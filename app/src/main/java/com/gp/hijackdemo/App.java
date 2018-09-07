package com.gp.hijackdemo;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class App extends Application{
    private static final String TAG = App.class.getSimpleName();

    private static List<String> packageList = new ArrayList<>();
    public static App applicationContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
    }

    public static boolean hasBeHijacked(String packageStr){
        if (packageList.contains(packageStr)){
            return true;
        }
        return false;
    }

    public static void addToHijacked(String packageStr){
        packageList.add(packageStr);
        Log.w(TAG,"packageList.size() = "+ packageList.size());
    }

    public static void clearHijacked(){
        packageList.clear();
        Log.w(TAG,"packageList.size() = "+ packageList.size());
    }

}
