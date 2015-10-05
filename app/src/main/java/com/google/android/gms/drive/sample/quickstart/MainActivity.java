/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.drive.sample.quickstart;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.ChangeListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Android Drive Quickstart activity. This activity takes a photo and saves it
 * in Google Drive. The user is prompted with a pre-made dialog which allows
 * them to choose the file location.
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String TAG = "drive-quickstart";
    private DriveId folderId_;

    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final int REQUEST_CODE_MANAGE = 4;


    private static GoogleApiClient mGoogleApiClient;
    private Bitmap mBitmapToSave;
    private Button sendBtn_;
    private Button receiveBtn_;

    private Button contactBtn_;
    private Button websiteBtn_;
    private Button manageDriveBtn_;
    private Button callBtn_;

    private View view_;

    /**
     * Create a new file and save it to Drive.
     */
    private void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = mBitmapToSave;
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveContentsResult>() {

                    @Override
                    public void onResult(DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");

                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        // Write the bitmap data from it.
                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                        try {
                            outputStream.write(bitmapStream.toByteArray());
                        } catch (IOException e1) {
                            Log.i(TAG, "Unable to write file contents.");
                        }

                        // Create the initial metadata - MIME type and title.
                        // Note that the user will be able to change the title later.
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("image/jpeg").setTitle("Android Photo.png").build();
                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view_ = findViewById(android.R.id.content);

        sendBtn_ = (Button) findViewById(R.id.sendBtn);
        receiveBtn_ = (Button)findViewById(R.id.receiveBtn);

        contactBtn_ = (Button) findViewById(R.id.contactBtn);
        websiteBtn_ = (Button)findViewById(R.id.siteBtn);
        manageDriveBtn_ = (Button)findViewById(R.id.manageBtn);


        sendBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SendActivity.class);
                startActivity(intent);
            }
        });

        receiveBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ReceiveActivity.class);
                startActivity(intent);
            }
        });

        websiteBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBrowser();
            }
        });


        contactBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmailApp();
            }
        });

        manageDriveBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrive();
            }
        });

    }



    @Override
    protected void onResume() {
    	super.onResume();
    	if (mGoogleApiClient == null) {
    		// Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
    	}
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    public static GoogleApiClient getGoogleApiClient(){
        return mGoogleApiClient;
    }



    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");
        if (mBitmapToSave == null) {
        	// This activity has no UI of its own. Just start the camera.

            return;
        }
       // saveFileToDrive();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    private void setListenerToReportsFolder(){
        DriveFolder folder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        folder.listChildren(mGoogleApiClient).setResultCallback(childrenRetrievedCallback);
    }

    ResultCallback<DriveApi.MetadataBufferResult> childrenRetrievedCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Toast.makeText(MainActivity.this, "Problem while retrieving folders", Toast.LENGTH_LONG);
                        Log.v(TAG,"Problem while retrieving folders");
                        return;
                    }

                    for (int i=0;i<result.getMetadataBuffer().getCount();i++){
                        String originalFileName = result.getMetadataBuffer().get(i).getOriginalFilename();
                        Log.v(TAG, originalFileName);
                        if (originalFileName.equals("Reports")) {
                            folderId_ = result.getMetadataBuffer().get(i).getDriveId();
                            DriveFolder reportsFolder = Drive.DriveApi.getFolder(mGoogleApiClient,folderId_);
                           reportsFolder.addChangeListener(mGoogleApiClient,changeListener);
                            break;
                        }
                    }

                }
            };

    /**
     * A listener to handle file change events.
     */
    final private ChangeListener changeListener = new ChangeListener() {
        @Override
        public void onChange(ChangeEvent event) {
            sendNotification(view_);
        }
    };

    public void sendNotification(View view) {

        // BEGIN_INCLUDE(build_action)
        /** Create an intent that will be fired when the user clicks the notification.
         * The intent needs to be packaged into a {@link android.app.PendingIntent} so that the
         * notification service can fire it on our behalf.
         */

        Intent intent = new Intent(MainActivity.this,  ReceiveActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // END_INCLUDE(build_action)

        // BEGIN_INCLUDE (build_notification)
        /**
         * Use NotificationCompat.Builder to set up our notification.
         */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this);

        /** Set the icon that will appear in the notification bar. This icon also appears
         * in the lower right hand corner of the notification itself.
         *
         * Important note: although you can use any drawable as the small icon, Android
         * design guidelines state that the icon should be simple and monochrome. Full-color
         * bitmaps or busy images don't render well on smaller screens and can end up
         * confusing the user.
         */
        builder.setSmallIcon(R.drawable.ic_launcher);

        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(pendingIntent);

        // Set the notification to auto-cancel. This means that the notification will disappear
        // after the user taps it, rather than remaining until it's explicitly dismissed.
        builder.setAutoCancel(true);

        /**
         *Build the notification's appearance.
         * Set the large icon, which appears on the left of the notification. In this
         * sample we'll set the large icon to be the same as our app icon. The app icon is a
         * reasonable default if you don't have anything more compelling to use as an icon.
         */
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        /**
         * Set the text of the notification. This sample sets the three most commononly used
         * text areas:
         * 1. The content title, which appears in large type at the top of the notification
         * 2. The content text, which appears in smaller text below the title
         * 3. The subtext, which appears under the text on newer devices. Devices running
         *    versions of Android prior to 4.2 will ignore this field, so don't use it for
         *    anything vital!
         */
        builder.setContentTitle("BasicNotifications Sample");
        builder.setContentText("Time to learn about notifications!");
        builder.setSubText("Tap to view documentation about notifications.");

        // END_INCLUDE (build_notification)

        // BEGIN_INCLUDE(send_notification)
        /**
         * Send the notification. This will immediately display the notification icon in the
         * notification bar.
         */
        NotificationManager notificationManager = (NotificationManager) MainActivity.this.getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
        // END_INCLUDE(send_notification)
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.visit_web_site_action:
                openBrowser();
                return true;

            case R.id.manage_drive_action:
                openDrive();
                return true;

            case R.id.qbo_login_action:
                openQboLoginForm();
                return true;

            case R.id.call_action:
                call();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    private void openDrive(){
//        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder().build();
//        // Create an intent for the file chooser, and start it.
//        IntentSender intentSender = Drive.DriveApi
//                .newCreateFileActivityBuilder()
//                .setInitialMetadata(metadataChangeSet)
//                .build(mGoogleApiClient);
//
//        try {
//            startIntentSenderForResult(
//                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
//        } catch (SendIntentException e) {
//            Log.i(TAG, "Failed to launch file chooser.");
//        }
//    }

    private void openEmailApp(){
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.setType("vnd.android.cursor.item/email");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {"will@cloudbookkeep.com"});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My Email Subject");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "My email content");
        startActivity(Intent.createChooser(emailIntent, "Send mail using..."));
    }

    private void openBrowser(){
        Uri url = Uri.parse("http://www.cloudbookkeep.com/");
        Intent intent = new Intent(Intent.ACTION_VIEW, url);

        if (intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        } else {
            Log.d("MainActivity", "Couldn't call because no receiving apps installed!");
        }
    }

    private void openDrive(){
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { "text/plain", "text/html","image/jpeg" })
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_MANAGE, null, 0, 0, 0);
        } catch (SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    private void openQboLoginForm(){
        Uri url = Uri.parse("https://qbo.intuit.com/qbo30/login?webredir/");
        Intent intent = new Intent(Intent.ACTION_VIEW, url);

        if (intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        } else {
            Log.d("MainActivity", "Couldn't call because no receiving apps installed!");
        }
    }

    private void call(){
        String phone = "+34666777888";
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
        startActivity(intent);
    }
}
