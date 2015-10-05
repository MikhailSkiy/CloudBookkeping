package com.google.android.gms.drive.sample.quickstart;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.google.android.gms.drive.DriveApi.*;

public class ReceiveActivity extends Activity {

    private static final String TAG = "ReceiveActivity";
    private static GoogleApiClient mGoogleApiClient;
    private DriveId folderId_;
    private String link_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);
        mGoogleApiClient = MainActivity.getGoogleApiClient();
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

    private void getFolder(){
        DriveFolder folder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        folder.listChildren(mGoogleApiClient).setResultCallback(childrenRetrievedCallback);

    }

    ResultCallback<DriveApi.MetadataBufferResult> childrenRetrievedCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Toast.makeText(ReceiveActivity.this, "Problem while retrieving folders", Toast.LENGTH_LONG);
                        Log.v(TAG,"Problem while retrieving folders");
                        return;
                    }

                    for (int i=0;i<result.getMetadataBuffer().getCount();i++){
                        String originalFileName = result.getMetadataBuffer().get(i).getOriginalFilename();
                        Log.v(TAG, originalFileName);
                        if (originalFileName.equals("Reports")) {
                            folderId_ = result.getMetadataBuffer().get(i).getDriveId();
                            DriveFolder reportsFolder = Drive.DriveApi.getFolder(mGoogleApiClient,folderId_);
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
            Log.v(TAG,filesCount);

           // for (int i=0;i<metadataBufferResult.getMetadataBuffer().getCount();i++){
                String reportFileName = metadataBufferResult.getMetadataBuffer().get(0).getOriginalFilename();
                Log.v("File name", reportFileName );

                String reportFileTitle = metadataBufferResult.getMetadataBuffer().get(0).getTitle();
                Log.v("File title", reportFileTitle );

                String fileLink = metadataBufferResult.getMetadataBuffer().get(0).getWebViewLink();
                Log.v("File link", fileLink );
           // }

            openGoogleSheets(fileLink);
        }
    };

    private void openGoogleSheets(String link){
        Uri url = Uri.parse(link);
        Intent intent = new Intent(Intent.ACTION_VIEW, url);

        if (intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        } else {
            Log.d("MainActivity", "Couldn't call because no receiving apps installed!");
        }
    }
}
