package com.google.android.gms.drive.sample.quickstart.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.DriveEventService;
import com.google.android.gms.drive.sample.quickstart.R;
import com.google.android.gms.drive.sample.quickstart.activity.ReceiveActivity;

/**
 * Created by Mikhail Valuyskiy on 06.10.2015.
 */
public class MyDriveEventService extends DriveEventService {
    @Override
    public void onChange(ChangeEvent event){
        Log.d("MyDriveEventService", "Change event: " + event);
        sendNotification();
    }

    private void sendNotification() {
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

        builder.setContentTitle("Reports notification");
        builder.setContentText("The reports in Reports folder were updated!");
        builder.setSubText("Tap to view changes.");

        NotificationManager notificationManager = (NotificationManager)this.getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }
}
