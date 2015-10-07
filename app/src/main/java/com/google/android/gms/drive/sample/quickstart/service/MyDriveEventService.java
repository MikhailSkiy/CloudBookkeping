package com.google.android.gms.drive.sample.quickstart.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.DriveEventService;
import com.google.android.gms.drive.sample.quickstart.R;
import com.google.android.gms.drive.sample.quickstart.activity.ReceiveActivity;
import com.google.android.gms.drive.sample.quickstart.activity.SettingsActivity;

/**
 * Created by Mikhail Valuyskiy on 06.10.2015.
 */
public class MyDriveEventService extends DriveEventService {

    private static final int REPORTS_NOTIFICATION = 1;
    private static final int QUERIES_NOTIFICATION = 2;

    @Override
    public void onChange(ChangeEvent event){

        Log.d("MyDriveEventService", "Change event: " + event);
        DriveId resourseId = event.getDriveId();
        if (resourseId.equals(SettingsActivity.mSelectedFileId)){
            sendNotification(REPORTS_NOTIFICATION);
        } else if (resourseId.equals(SettingsActivity.mQueryFileId)){
            sendNotification(QUERIES_NOTIFICATION);
        }

    }

    private void sendNotification(int notificationType) {
        Intent intent = new Intent(this,  ReceiveActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher);
        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(pendingIntent);
        // Set the notification to auto-cancel. This means that the notification will disappear
        // after the user taps it, rather than remaining until it's explicitly dismissed.
        builder.setAutoCancel(true);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        if (notificationType==1) {
            builder.setContentTitle("Reports notification");
            builder.setContentText("The reports in Reports folder were updated!");
            builder.setSubText("Tap to view changes.");
        } else if (notificationType ==2){
            if (notificationType==2) {
                builder.setContentTitle("Queries notification");
                builder.setContentText("The queries in Bank queries folder were updated!");
                builder.setSubText("Tap to view changes.");
            }
        }

        NotificationManager notificationManager = (NotificationManager)this.getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }
}
