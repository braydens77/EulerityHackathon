package com.braydensorenson.eulerityhackathon;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String IMAGES_URL = "https://eulerity-hackathon.appspot.com/image";
    private static final String TAG = MainActivity.class.getSimpleName();
    RequestQueue requestQueue;
    LinearLayout linearLayout;
    public static List<Bitmap> bitmaps = new ArrayList<>();
    public static List<String> imgUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        getImageList();
    }

    private void getImageList(){
        requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest imgListRequest = new JsonArrayRequest(Request.Method.GET, IMAGES_URL, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, response.toString());
                        getImages(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error with JsonArrayRequest: " + error.getMessage());
                        Toast.makeText(getApplicationContext(), "Error obtaining images", Toast.LENGTH_LONG).show();
                    }
        });
        imgListRequest.setTag(TAG);
        requestQueue.add(imgListRequest);
    }

    private void getImages(JSONArray jsonArr){
        for(int i=0; i<jsonArr.length(); i++){
            try{
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                String url = jsonObj.getString("url");
                downloadImage(url);
            }catch(JSONException e){
                Log.d(TAG, "Error getting JSON object: " + e.getMessage());
            }
        }
    }

    private void downloadImage(String url){
        ImageDownloadListener imgDlListener = new ImageDownloadListener(url);
        ImageRequest imgReq = new ImageRequest(url,
                imgDlListener, 1000, 1000,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.ARGB_8888,
                imgDlListener
        );
        imgReq.setTag(TAG);
        requestQueue.add(imgReq);
    }

    class ImageDownloadListener implements Response.Listener<Bitmap>, Response.ErrorListener {
        private String url;

        ImageDownloadListener(String url){
            this.url = url;
        }

        @Override
        public void onResponse(final Bitmap bitmap) {
            bitmaps.add(bitmap);
            imgUrls.add(url);
            final int imgId = bitmaps.size()-1;
            ImageView imgView = new ImageView(getApplicationContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int)pxFromDp(getApplicationContext(), 200)
            );
            /*imgView.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
            imgView.getLayoutParams().height = (int)dpFromPx(getApplicationContext(), 100);*/
            imgView.setLayoutParams(layoutParams);
            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgView.setImageBitmap(bitmap);
            imgView.setTag(imgId);
            imgView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent edit = new Intent(getApplicationContext(), EditImageActivity.class);
                    edit.putExtra("imgId", imgId);
                    edit.putExtra("imgUrl", url);
                    startActivity(edit);
                }
            });
            linearLayout.addView(imgView);
        }

        public float pxFromDp(final Context context, final float dp) {
            return dp * context.getResources().getDisplayMetrics().density;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "Error downloading image: " + error.getMessage());
        }
    }

    @Override
    protected void onStop () {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}
