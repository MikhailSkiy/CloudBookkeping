package com.google.android.gms.drive.sample.quickstart.activity;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.ChangeListener;
import com.google.android.gms.drive.sample.quickstart.R;

import static com.google.android.gms.drive.DriveApi.*;
import static com.google.android.gms.drive.DriveResource.*;

public class ReceiveActivity extends Activity {

    private static final int REQUEST_CODE_OPENER = 5;
    private static final int REQUEST_CODE_OPEN_BANK_QUERY = 6;

    private static final String REPORTS_FOLDER = "reports_folder_id";
    private static final String BANK_QUERY_FILE = "bank_query_id_file";

    private static final String TAG = "ReceiveActivity";
    private static GoogleApiClient mGoogleApiClient;
    private DriveId folderId_;
    private String link_;

    private Button bankQueryBtn_;
    private Button reminderBtn_;
    private Button viewReportsBtn_;
    private View view_;

    ImageView bankView_;
    ImageView reportsView_;

    /**
     * Represents the file picked by the user.
     */
    private DriveId selectedFolderId_;
    private DriveId  bankQueryFileId_;

    /**
     * Keeps the status whether change events are being listened to or not.
     */
    private boolean isSubscribed = false;
    final private Object mSubscriptionStatusLock = new Object();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        Bundle args = getIntent().getExtras();
        if (args != null) {
            String id = args.getString(getString(R.string.id_key));
            if (id != null) {
                if ((selectedFolderId_ != null)||( getId(REPORTS_FOLDER) !=null)) {
                    String testId = getId(REPORTS_FOLDER);
                    if (id.equals(getId(REPORTS_FOLDER))) {
                        reportsView_ = (ImageView)findViewById(R.id.reports_view);
                        reportsView_.setVisibility(View.VISIBLE);
                    }
                }
                if ((bankQueryFileId_ != null)||( getId(BANK_QUERY_FILE) !=null)) {
                    if (id.equals(getId(BANK_QUERY_FILE))) {
                        bankView_ = (ImageView)findViewById(R.id.bank_view);
                        bankView_.setVisibility(View.VISIBLE);
                    }
                }
            }
        }


        view_ = findViewById(android.R.id.content);
        mGoogleApiClient = MainActivity.getGoogleApiClient();

        bankQueryBtn_ = (Button) findViewById(R.id.bank_query_btn);
        reminderBtn_ = (Button) findViewById(R.id.reminder_btn);
        viewReportsBtn_ = (Button) findViewById(R.id.reports_btn);

        bankQueryBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBankQueryFile();
            }
        });

        viewReportsBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openReports();

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!(mGoogleApiClient == null)) {
            mGoogleApiClient.connect();
        }
    }



    private String getId(String key){
        SharedPreferences prefs = this.getSharedPreferences(
                "com.example.app", Context.MODE_PRIVATE);

        String value = prefs.getString(key,null);
        return value;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OPENER:
                if (resultCode == RESULT_OK) {
                    selectedFolderId_ = (DriveId) data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    DriveFile selectedPdfReport = Drive.DriveApi.getFile(mGoogleApiClient, selectedFolderId_);
                    selectedPdfReport.getMetadata(mGoogleApiClient).setResultCallback(metadataRetrievedCallback);
                    reportsView_.setVisibility(View.INVISIBLE);

                }
                break;

            case REQUEST_CODE_OPEN_BANK_QUERY:
                if (resultCode == RESULT_OK) {
                   bankQueryFileId_ = (DriveId) data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    DriveFile file = bankQueryFileId_.asDriveFile();
                    file.getMetadata(mGoogleApiClient)
                            .setResultCallback(metadataCallback);
                    bankView_.setVisibility(View.INVISIBLE);
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    final private ResultCallback<DriveResource.MetadataResult> metadataCallback = new
            ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {
//                        showMessage("Problem while trying to fetch metadata");
                        return;
                    }
                    Metadata metadata = result.getMetadata();
                    Log.v("Link", metadata.getEmbedLink());
                    openGoogleForm(metadata.getEmbedLink());
                }
            };

    // Opens google form in the web browser
    private void openGoogleForm(String link) {
        Uri url = Uri.parse(link);
        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d("MainActivity", "Couldn't call because no receiving apps installed!");
        }
    }


    ResultCallback<DriveResource.MetadataResult> metadataRetrievedCallback = new
            ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i(TAG, "Failed to get metadata.");
                        return;
                    }
                    Metadata metadata = result.getMetadata();
                    Log.i(TAG, "Metadata was retrieved successfully.");
                    openPdf(metadata.getWebContentLink());
                }
            };

    private void openPdf(String link) {
        Uri url = Uri.parse(link);
        // Uri url = Uri.parse("http://goo.gl/forms/Z9FPs1TsVC");
        Intent intent = new Intent(Intent.ACTION_VIEW, url);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d("MainActivity", "Couldn't call because no receiving apps installed!");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_receive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void openReports() {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }

                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newOpenFileActivityBuilder()
                                .setMimeType(new String[]{"application/pdf"})
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

    // Opens Google Drive file chooser where user can open "To Do" Folder and select Google Form with to do tasks
    private void openBankQueryFile() {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }

                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newOpenFileActivityBuilder()
                                .setMimeType(new String[]{"application/vnd.google-apps.spreadsheet"})
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_OPEN_BANK_QUERY, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

    /**
     * This method opens Drive activity with list of folders and files
     * and helps user to choose folder for listening changes
     */
    private void selectFolderForChangeListening() {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }

                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newOpenFileActivityBuilder()
                                .setMimeType(new String[]{"application/vnd.google-apps.folder"})
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

    private void getFolder() {
        DriveFolder folder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        folder.listChildren(mGoogleApiClient).setResultCallback(childrenRetrievedCallback);

    }

    ResultCallback<DriveApi.MetadataBufferResult> childrenRetrievedCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Toast.makeText(ReceiveActivity.this, "Problem while retrieving folders", Toast.LENGTH_LONG);
                        Log.v(TAG, "Problem while retrieving folders");
                        return;
                    }

                    for (int i = 0; i < result.getMetadataBuffer().getCount(); i++) {
                        String originalFileName = result.getMetadataBuffer().get(i).getOriginalFilename();
                        Log.v(TAG, originalFileName);
                        if (originalFileName.equals("Reports")) {
                            folderId_ = result.getMetadataBuffer().get(i).getDriveId();
                            DriveFolder reportsFolder = Drive.DriveApi.getFolder(mGoogleApiClient, folderId_);
                            reportsFolder.listChildren(mGoogleApiClient).setResultCallback(reportsResultCallback);
                            link_ = result.getMetadataBuffer().get(i).getWebViewLink();
                            break;
                        }
                    }

                }
            };

    ResultCallback<DriveApi.MetadataBufferResult> reportsResultCallback = new ResultCallback<MetadataBufferResult>() {
        @Override
        public void onResult(MetadataBufferResult metadataBufferResult) {
            if (!metadataBufferResult.getStatus().isSuccess()) {
                Toast.makeText(ReceiveActivity.this, "Problem while retrieving files", Toast.LENGTH_LONG);
                Log.v(TAG, "Problem while retrieving files");
                return;
            }
            String filesCount = Integer.toString(metadataBufferResult.getMetadataBuffer().getCount());
            Log.v(TAG, filesCount);

            // for (int i=0;i<metadataBufferResult.getMetadataBuffer().getCount();i++){
            String reportFileName = metadataBufferResult.getMetadataBuffer().get(0).getOriginalFilename();
            Log.v("File name", reportFileName);

            String reportFileTitle = metadataBufferResult.getMetadataBuffer().get(0).getTitle();
            Log.v("File title", reportFileTitle);

            String fileLink = metadataBufferResult.getMetadataBuffer().get(0).getWebViewLink();
            Log.v("File link", fileLink);
            // }

            openGoogleSheets(fileLink);
        }
    };

    private void openGoogleSheets(String link) {
        Uri url = Uri.parse(link);
        Intent intent = new Intent(Intent.ACTION_VIEW, url);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d("MainActivity", "Couldn't call because no receiving apps installed!");
        }
    }

    /**
     * A listener to handle file change events.
     */
    final private ChangeListener changeListener = new ChangeListener() {
        @Override
        public void onChange(ChangeEvent event) {
            sendNotification(view_);
        }
    };

    private void sendNotification(View view) {
        Intent intent = new Intent(ReceiveActivity.this, ReceiveActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ReceiveActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ReceiveActivity.this);
        builder.setSmallIcon(R.drawable.ic_launcher);
        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(pendingIntent);
        // Set the notification to auto-cancel. This means that the notification will disappear
        // after the user taps it, rather than remaining until it's explicitly dismissed.
        builder.setAutoCancel(true);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        builder.setContentTitle("BasicNotifications Sample");
        builder.setContentText("Time to learn about notifications!");
        builder.setSubText("Tap to view documentation about notifications.");

        NotificationManager notificationManager = (NotificationManager) ReceiveActivity.this.getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

    /**
     * Toggles the subscription status. If there is no selected file, returns
     * immediately.
     */
    private void toggle() {
        if (selectedFolderId_ == null) {
            return;
        }
        synchronized (mSubscriptionStatusLock) {
            DriveFile file = selectedFolderId_.asDriveFile();
            if (!isSubscribed) {
                Log.d(TAG, "Starting to listen to the file changes.");
                file.addChangeListener(mGoogleApiClient, changeListener);
                isSubscribed = true;
            } else {
                Log.d(TAG, "Stopping to listen to the file changes.");
                file.removeChangeListener(mGoogleApiClient, changeListener);
                isSubscribed = false;
            }
        }
        //refresh();
    }
}
