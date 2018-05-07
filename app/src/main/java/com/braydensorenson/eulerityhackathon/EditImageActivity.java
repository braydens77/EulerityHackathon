package com.braydensorenson.eulerityhackathon;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubfilter;
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubfilter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class EditImageActivity extends AppCompatActivity implements FiltersFragment.ThumbnailInteractionListener, EditFragment.ValueChangeListener {
    Bitmap originalBitmap;
    Bitmap filteredImg;
    Bitmap mostRecentBitmap;
    ImageView fullImgView;
    RequestQueue requestQueue;
    EditFragment editFragment;
    FiltersFragment filtersFragment;
    String lineEnd = "\r\n";
    String twoHyphens = "--";
    String boundary = "*****MyBoundary";
    String mimeType = "multipart/form-data; boundary=" + boundary;
    private static final String TAG = EditImageActivity.class.getSimpleName();
    private static final String UPLOAD_URL = "https://eulerity-hackathon.appspot.com/upload";
    private static final int REQUEST_WRITE_EXTERNAL = 1;
    private int imgId;
    boolean saveAllowed = false;

    static
    {
        System.loadLibrary("NativeImageProcessor");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.editImageToolbar);
        setSupportActionBar(toolbar);
        fullImgView = (ImageView) findViewById(R.id.fullImage);
        imgId = getIntent().getIntExtra("imgId", 0);
        originalBitmap = MainActivity.bitmaps.get(imgId);
        fullImgView.setImageBitmap(originalBitmap);
        requestQueue = Volley.newRequestQueue(this);
        setUpTabs();
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);
        if (fragment == null) {
            filtersFragment = FiltersFragment.newInstance(imgId);
            fm.beginTransaction().add(R.id.fragmentContainer, filtersFragment).commit();
        }
    }

    private void setUpTabs(){
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getText().equals("Edit")){
                    //Toast.makeText(getApplicationContext(), "Edit selected", Toast.LENGTH_LONG).show();
                    if (editFragment == null) {
                        editFragment = EditFragment.newInstance();
                    }
                    fm.beginTransaction().replace(R.id.fragmentContainer, editFragment).commit();
                }else if(tab.getText().equals("Filter")){
                    //Toast.makeText(getApplicationContext(), "Filter selected", Toast.LENGTH_LONG).show();
                    if (filtersFragment == null) {
                        filtersFragment = FiltersFragment.newInstance(imgId);
                    }
                    fm.beginTransaction().replace(R.id.fragmentContainer, filtersFragment).commit();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onThumbnailClick(Filter f) {
        // Convert a copy of the full size image with selected filter
        Bitmap copy = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        filteredImg = f.processFilter(copy);
        fullImgView.setImageBitmap(filteredImg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_image_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.save:
                saveImage();
                break;
            case R.id.upload:
                uploadImage();
                break;
        }
        return true;
    }

    private void uploadImage(){
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, UPLOAD_URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String url = response.getString("url");
                            Log.d(TAG, "url for upload: " + url);
                            postImage(url);
                        } catch (JSONException e) {
                            Log.d(TAG, "Could not parse JSONObject: " + e.getMessage());
                            Toast.makeText(getApplicationContext(),"Could not obtain upload url", Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error with JsonObjectRequest: " + error.getMessage());
                        Toast.makeText(getApplicationContext(), "Error obtaining upload URL", Toast.LENGTH_LONG).show();
                    }
        });
        request.setTag(TAG);
        requestQueue.add(request);
    }

    private void saveImage(){
        checkWritePermission();
        if(!saveAllowed){
            Toast.makeText(this, "Saving is not enabled", Toast.LENGTH_LONG).show();
            return;
        }
        String fileName = createPNGFileName();
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), fileName);
        try {
            OutputStream output = new FileOutputStream(file);
            Bitmap imgToSave;
            if(mostRecentBitmap != null)
                imgToSave = mostRecentBitmap;
            else if(filteredImg != null)
                imgToSave = filteredImg;
            else imgToSave = originalBitmap;
            boolean saved = imgToSave.compress(Bitmap.CompressFormat.PNG, 100, output);
            if(!saved){
                Log.d(TAG, "compress failed");
                Toast.makeText(this, "Compress failed", Toast.LENGTH_LONG).show();
            }
            output.flush();
            output.close();
            Toast.makeText(this, fileName + " saved", Toast.LENGTH_LONG).show();
        }catch (IOException e) {
            Log.d(TAG, e.getMessage());
            Toast.makeText(this, "Could not save image", Toast.LENGTH_SHORT).show();
        }
    }

    private String createPNGFileName(){
        String prefix = "image";
        Long millis = System.currentTimeMillis();
        String timeStamp = millis.toString();
        return prefix + timeStamp + ".png";
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void checkWritePermission(){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL);
            }
        }else{
            saveAllowed = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveAllowed = true;
                }
            }
        }
    }

    private void postImage(final String url) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Bitmap imgToUpload;
            if(mostRecentBitmap != null)
                imgToUpload = mostRecentBitmap;
            else if(filteredImg != null)
                imgToUpload = filteredImg;
            else {
                Toast.makeText(this, "No changes to upload", Toast.LENGTH_LONG).show();
                return;
            }
            imgToUpload.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] fileByteArray = byteArrayOutputStream.toByteArray();
            byte[] multipartBody = null;
            ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream2);
            try {
                addPayload(dataOutputStream, fileByteArray, createPNGFileName());
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                multipartBody = byteArrayOutputStream2.toByteArray();
                Log.d(TAG, "Payload for request: " + new String(multipartBody));
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*Map<String, String> headers = new HashMap<>();
            headers.put("content-type", mimeType);*/
            MultipartRequest multipartRequest = new MultipartRequest(url, null, mimeType, multipartBody, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    Log.d(TAG, "POST image upload response: " + response.toString());
                    Toast.makeText(getApplicationContext(), "Image uploaded", Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "Could not POST image to url " + url + " : " + error.getMessage());
                    Toast.makeText(getApplicationContext(), "Could not POST image to url", Toast.LENGTH_SHORT).show();
                }
            });
            Log.d(TAG, "Headers: " + multipartRequest.getHeaders().toString());
            requestQueue.add(multipartRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addPayload(DataOutputStream dataOutputStream, byte[] fileData, String fileName) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        // Add appid
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"appid\""
                + lineEnd +  lineEnd + "braydens77@gmail.com" + lineEnd);

        // Add original
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"original\""
                + lineEnd + lineEnd + getIntent().getStringExtra("imgUrl"));
        dataOutputStream.writeBytes(lineEnd);

        // Add file
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
                + fileName + "\"" + lineEnd
                + "Content-Type: image/png" + lineEnd + lineEnd);
        // Add image file content
        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileData);
        int bytesAvailable = fileInputStream.available();
        int maxBufferSize = 1024 * 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }
        dataOutputStream.writeBytes(lineEnd);
    }

    @Override
    public void onBrightnessChange(int progress) {
        // Brightness values from -100 to 100
        progress -= 100;
        Bitmap bm = getCopyOfCurrentBitmap();
        Filter f = new Filter();
        f.addSubFilter(new BrightnessSubfilter(progress));
        mostRecentBitmap = f.processFilter(bm);
        fullImgView.setImageBitmap(bm);
    }

    @Override
    public void onContrastChange(int progress) {
        // Contrast values from 1.0 to 3.0
        float value = (float) (0.1 * progress + 1);
        Bitmap bm = getCopyOfCurrentBitmap();
        Filter f = new Filter();
        f.addSubFilter(new ContrastSubfilter(value));
        mostRecentBitmap = f.processFilter(bm);
        fullImgView.setImageBitmap(bm);
    }

    private Bitmap getCopyOfCurrentBitmap(){
        Bitmap bm = null;
        if(filteredImg != null) {
            return filteredImg.copy(Bitmap.Config.ARGB_8888, true);
        }
        return originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
    }
}
