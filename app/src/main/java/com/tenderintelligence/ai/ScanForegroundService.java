package com.tenderintelligence.ai;

import android.app.*;
import android.content.*;
import android.os.*;
import android.content.pm.ServiceInfo;

/** Foreground process lifetime support while the activity-based GeM WebView scans. */
public class ScanForegroundService extends Service {
 public static final String ACTION_STOP="com.tenderintelligence.ai.STOP_SCAN_SERVICE";
 private static final String CHANNEL="tender_scan";
 private static final int NOTIFICATION_ID=2107;
 @Override public void onCreate(){
  super.onCreate();
  NotificationManager manager=getSystemService(NotificationManager.class);
  NotificationChannel channel=new NotificationChannel(CHANNEL,"Tender Radar scanning",NotificationManager.IMPORTANCE_LOW);
  channel.setDescription("Shows when a GeM tender scan is running");
  manager.createNotificationChannel(channel);
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&ACTION_STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  Intent open=new Intent(this,GemBrowserActivity.class);
  open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent pending=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification notification=new Notification.Builder(this,CHANNEL)
   .setSmallIcon(android.R.drawable.stat_notify_sync)
   .setContentTitle("Tender Radar scanning")
   .setContentText("GeM scan active • tap to view progress")
   .setContentIntent(pending).setOngoing(true).setOnlyAlertOnce(true).build();
  if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
  else startForeground(NOTIFICATION_ID,notification);
  return START_NOT_STICKY;
 }
 @Override public android.os.IBinder onBind(Intent intent){return null;}
}
