package com.ihemingway.helloworld.shortcut;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Description:android  快捷键创建工具
 * Data：2018/7/16-13:46
 * 启动的activity需要加入行为
 * <intent-filter>
 * <action android:name="android.intent.action.CREATE_SHORTCUT"/>
 * </intent-filter>
 * <p>
 * Author: hemingway
 */
public class ShortCutUtils {
    private static final String ACTION_ADD_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    //移除快捷方式
    private static final String ACTION_REMOVE_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";
    private static final String TAG = ShortCutUtils.class.getSimpleName();
    private static final String SP_SHORT_CUT_AUTO_BUILD = "short_cut_auto_build";

    /**
     * 自动添加shortCut
     * 用于首页添加
     * @param context
     * @param scName
     * @param scIconId
     * @param launchActivity
     */
    public static void autoAddShortCutOnce(Context context, String scName, int scIconId, Class launchActivity){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_SHORT_CUT_AUTO_BUILD, Context.MODE_PRIVATE);
        boolean hasAdded= sharedPreferences.getBoolean(SP_SHORT_CUT_AUTO_BUILD, false);
        if(hasAdded){
            return;
        }
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean(SP_SHORT_CUT_AUTO_BUILD,true);
        edit.apply();

        addShortCut(context,scName,scIconId,launchActivity);
    }

    public static void autoAddShortCutOnceByName(Context context, String scName, int scIconId, String bName,Class launchActivity){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_SHORT_CUT_AUTO_BUILD, Context.MODE_PRIVATE);
        boolean hasAdded= sharedPreferences.getBoolean(SP_SHORT_CUT_AUTO_BUILD, false);
        if(hasAdded){
            return;
        }
        SharedPreferences.Editor edit = sharedPreferences.edit();
        StringBuffer sb = new StringBuffer();
        sb.append(SP_SHORT_CUT_AUTO_BUILD).append(bName);
        edit.putBoolean(sb.toString(),true);
        edit.apply();

        addShortCut(context,scName,scIconId,launchActivity);
    }

    /**
     * 启动的activity需要加入行为
     * <intent-filter>
     * <action android:name="android.intent.action.CREATE_SHORTCUT"/>
     * <action android:name="com.android.launcher.action.INSTALL_SHORTCUT" />
     * </intent-filter>
     *
     * @param context
     * @param scName
     * @param scIconId
     * @param launchActivity
     */
    public static void addShortCut(Context context, String scName, int scIconId, Class launchActivity) {

        //vivo等部分机型不适合该方法，防止自动多创建多个快件键，故使用sharePreference
        //有效机型 小米
        if(hasShortcut(context,scName)){
//            removeShortcut(context,scName,launchActivity);
            Log.d(TAG,"hasShortcut");
            return;
        }else{
            Log.d(TAG,"addShortCut");
        }
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.setClass(context, launchActivity);
        launcherIntent.setAction(Intent.ACTION_CREATE_SHORTCUT);
        launcherIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //不能加入该句 oppo手机无法使用快捷方式 因为再fest文件已经对MainActivity做了声明
        //launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addShortCutAboveO(context, scName, scIconId, launcherIntent,launchActivity.getSimpleName());
        } else {
            addShortCutBelowO(context, scName, scIconId, launcherIntent);
        }
    }

    private static void addShortCutBelowO(Context context, String scName, int scIconId, Intent launchIntent) {
        Intent addShortcutIntent = new Intent(ACTION_ADD_SHORTCUT);
        // 不允许重复创建，不是根据快捷方式的名字判断重复的
        //小米手机 version < 8.0 设置为false 不能创建出icon
        //经测试不是根据快捷方式的名字判断重复的
        // 应该是根据快链的Intent来判断是否重复的,即Intent.EXTRA_SHORTCUT_INTENT字段的value
        // 但是名称不同时，虽然有的手机系统会显示Toast提示重复，仍然会建立快链
        // 屏幕上没有空间时会提示
        // 注意：重复创建的行为MIUI和三星手机上不太一样，小米上似乎不能重复创建快捷方式
        addShortcutIntent.putExtra("duplicate", false);
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, scName);
        //图标
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, scIconId));
        // 设置关联程序
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
        // 发送广播
        context.sendBroadcast(addShortcutIntent);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void addShortCutAboveO(Context context, String scName, int scIconId, Intent launchIntent, String simpleName) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            ShortcutInfo info = new ShortcutInfo.Builder(context, simpleName)
                    .setIcon(Icon.createWithResource(context, scIconId))
                    .setShortLabel(scName)
                    .setIntent(launchIntent)
                    .build();
            //当添加快捷方式的确认弹框弹出来时，将被回调
            PendingIntent shortcutCallbackIntent = PendingIntent.getBroadcast(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            shortcutManager.requestPinShortcut(info, shortcutCallbackIntent.getIntentSender());
        }
    }

    private static void removeShortcut(Context context, String name, Class launchActivity) {
        // remove shortcut的方法在小米系统上不管用，在三星上可以移除
        Intent intent = new Intent(ACTION_REMOVE_SHORTCUT);
        // 名字
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        // 设置关联程序
        Intent launcherIntent = new Intent(context,
                launchActivity).setAction(Intent.ACTION_MAIN);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
        // 发送广播
        context.sendBroadcast(intent);
    }

    public static String getAuthorityFromPermission(Context context) {
        // 先得到默认的Launcher
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager mPackageManager = context.getPackageManager();
        ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return null;
        }
//        PackageManager.MATCH_DEFAULT_ONLY PackageManager.GET_PROVIDERS
        List<ProviderInfo> info = mPackageManager.queryContentProviders(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.applicationInfo.uid, PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null) {
            for (int j = 0; j < info.size(); j++) {
                ProviderInfo provider = info.get(j);
                if (provider.readPermission == null) {
                    continue;
                }
                Log.d(TAG, provider.readPermission);
                Log.d(TAG, provider.authority);
                if (Pattern.matches(".*launcher.*READ_SETTINGS", provider.readPermission)) {
                    return provider.authority;
                }
            }
        }
        return null;
    }

    /**
     * oppo 无效 小米有效
     *
     * @param context
     * @param appName
     * @return
     */
    public static boolean hasShortcut(Context context, String appName) {
        Log.d(TAG, appName);
        String authority = getAuthorityFromPermission(context);
        if (authority == null) {
            return false;
        }
        String url = "content://" + authority + "/favorites?notify=true";
        Log.d(TAG, "url_" + url);
        try {
            Uri CONTENT_URI = Uri.parse(url);
            Cursor c = context.getContentResolver().query(CONTENT_URI, new String[]{"title"}, "title = ?", new String[]{appName}, null);
            if (c != null && c.moveToNext()) {
                c.close();
                Log.d(TAG, "TRUE");
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        Log.d(TAG, "FALSE");
        return false;
    }

}
