package com.zyfdroid.epub.server;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zyfdroid.epub.R;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ServerActivity extends AppCompatActivity {

    TextView txtStatistic;

    @Override
    protected void onDestroy() {
        hWnd.removeCallbacks(timer);
        super.onDestroy();
    }


    ToggleButton tb;

    Handler hWnd = new Handler();

    long downbegin=0;
    long upbegin=0;

    float animation=0f;

    View curOuter,curInner;

    float rotateU=0f;
    float rotateD=0f;

    float rotateSmoothFactor = 0.03f;

    long lastsecond=0;

    float vd=0;
    float vu=0;

    double lsd;
    double lsu;

    float dvd,dvu;

    long lastU,lastD;

    Runnable timer = new Runnable() {
        @Override
        public void run() {

            if(isStarted()){
                long down = TrafficStats.getUidRxBytes(getApplicationInfo().uid) - downbegin;
                long up = TrafficStats.getUidTxBytes(getApplicationInfo().uid) - upbegin;


                if(lastsecond != System.currentTimeMillis()/1000){
                    lastsecond = System.currentTimeMillis()/1000;
                    txtStatistic.setText("↓ "+ Formatter.formatFileSize(ServerActivity.this, down  -lastD)+"/s  ↑ " + Formatter.formatFileSize(ServerActivity.this,up - lastU)+"/s");
                    lastU = up;
                    lastD = down;
                }

                if(animation<1){
                    animation+=(1-animation)*0.1f;
                    if(animation>1){
                        animation=1;
                    }
                }



                double d = (down) / 1048576d * 360 - lsd;
                double u = (-up) / 1048576d * 360 - lsu;
                lsd = (down) / 1048576d * 360;
                lsu = (-up) / 1048576d * 360;

                vd =(float) Math.atan(d);
                vu =(float) Math.atan(u);

                dvd += (vd - dvd) * 0.1f;
                dvu += (vu - dvu) * 0.1f;

                rotateD -= dvd; //(((float)d) - rotateD) * rotateSmoothFactor;
                rotateU -= dvu; //(((float)u) - rotateU) * rotateSmoothFactor;

                if(rotateD < 0){rotateD+=360;}
                if(rotateU > 360){rotateD-=360;}

                curInner.setRotation(rotateD);
                curOuter.setRotation(rotateU);
            }
            else{
                if(animation>0){
                    animation-=(animation) * 0.1f;
                    if(animation<0){
                        animation=0;
                    }
                }
                txtStatistic.setText("");
                ((TextView)findViewById(R.id.txtIps)).setElevation(0);
            }
            curInner.setScaleX(animation);
            curInner.setScaleY(animation);
            curOuter.setScaleX(animation);
            curOuter.setScaleY(animation);
            txtIp.setElevation(5 * getResources().getDisplayMetrics().density * animation);
            hWnd.postDelayed(timer,16);
        }
    };
    TextView txtIp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        curInner = findViewById(R.id.cirInner);
        curOuter = findViewById(R.id.cirOuter);
        txtIp = findViewById(R.id.txtIps);
        //BasicConfigurator.configure();
        txtStatistic = findViewById(R.id.txtStatistic);

        tb = findViewById(R.id.btnToggle);
        tb.setChecked(isStarted());

        tb.setChecked(isStarted());
        animation=isStarted() ? 1 : 0;
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    start();
                }
                else{
                    stop();
                }
                ((TextView)findViewById(R.id.txtIps)).setText(getLocalIPAddress());
            }
        });
        hWnd.postDelayed(timer,500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((TextView)findViewById(R.id.txtIps)).setText(getLocalIPAddress());
    }

    long t = 0;

    public boolean isStarted() {
        return ServerService.httpServer!=null;
    }
    public void start(){
        if(!isStarted()) {
            startService(new Intent(this,ServerService.class));
            downbegin = TrafficStats.getUidRxBytes(getApplicationInfo().uid);
            upbegin = TrafficStats.getUidTxBytes(getApplicationInfo().uid);
            lastD = 0;
            lastU = 0;
            dvd=30;
            dvu=-30;
            rotateD = -90f;
            rotateU = 90f;
        }
    }

    public void stop(){
        if(isStarted()){
            stopService(new Intent(this,ServerService.class));
        }
    }

    public String getLocalIPAddress()
    {
        String result="";
        try
        {
            for (Enumeration<NetworkInterface> mEnumeration = NetworkInterface.getNetworkInterfaces(); mEnumeration.hasMoreElements();)               {
                NetworkInterface intf = mEnumeration.nextElement();
                for (Enumeration<InetAddress> enumIPAddr = intf.getInetAddresses(); enumIPAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIPAddr.nextElement();
                    //如果不是回环地址
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.getHostAddress().contains(":"))
                    {
                        result+= "http://"+inetAddress.getHostAddress()+":2480/\n";
                    }
                }
            }
        }
        catch (SocketException ex)
        {
            android.util.Log.e("Error", ex.toString());
        }
        if(result.trim().isEmpty()){
            return getString(R.string.no_internet);
        }
        return result.trim();
    }
}