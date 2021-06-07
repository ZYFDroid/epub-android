package com.zyfdroid.epub.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class ServerService extends Service {

    public static EPUBiumServer httpServer;
    public ServerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        httpServer=null;
        EPUBiumServer ss = new EPUBiumServer(this);
        try {
            ss.start();
            Toast.makeText(this, "服务器已开启", Toast.LENGTH_SHORT).show();
            httpServer = ss;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "服务器开启失败："+e.getMessage(), Toast.LENGTH_SHORT).show();
            ss.stop();
        }
        startForeground(NotificationUtils.notificationId,NotificationUtils.postNoification(this,"EPUBium服务器正在运行... \r\n点击打开界面","EPUBium服务器"));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        EPUBiumServer server = httpServer;
        httpServer = null;
        if(server!=null) {
            server.stop();
        }
        stopForeground(true);
        NotificationUtils.removeNotification(this);
        Toast.makeText(getApplicationContext(), "服务器已关闭", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }
}