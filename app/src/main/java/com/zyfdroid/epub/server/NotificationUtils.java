package com.zyfdroid.epub.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.zyfdroid.epub.R;

public class NotificationUtils {

    public static final int notificationId=238454;
    public static final String channelid="epubium_foreground";
    public static final String channelname="EPUBium服务器正在运行的通知";

    public static void showNotificationSetting(Context ctx){
        NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(channelid,channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);

            Intent i = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);

            i.putExtra(Settings.EXTRA_APP_PACKAGE,ctx.getPackageName());
            i.putExtra(Settings.EXTRA_CHANNEL_ID,channelid);
            ctx.startActivity(i);
        }else{

            try{
                Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                i.putExtra(Intent.EXTRA_PACKAGE_NAME,ctx.getPackageName());
                ctx.startActivity(i);
            }catch (Exception ex){
                Toast.makeText(ctx, "打开设置失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static Notification postNoification(Context ctx, String title, String msg){
        Notification.Builder nb;
        NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(channelid,channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
            nb= new Notification.Builder(ctx,channelid);
        }else{
            nb = new Notification.Builder(ctx);

        }

        Intent activityIntent = new Intent(ctx, ServerActivity.class);
        PendingIntent pintent = PendingIntent.getActivity(ctx,0,activityIntent,0);
        nb.setContentIntent(pintent);
        //nb.setFullScreenIntent(pintent,true);
        nb.setSmallIcon(R.drawable.ic_launcher_foreground);
        nb.setContentTitle(title);
        nb.setOngoing(true);

        nb.setAutoCancel(false);
        nb.setContentText(msg);
        return nb.build();
    }

    public static void removeNotification(Context ctx){
        NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);
    }

    public static void OnReportCompleted(Context ctx){
        removeNotification(ctx);
    }

}
