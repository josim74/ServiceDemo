package com.example.servicedemo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.servicedemo.model.Image;
import com.example.servicedemo.restapi.ApiClient;
import com.example.servicedemo.restapi.RetrofitApi;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyService extends Service {
    public MyService() {
    }

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int itemCount = 0;
    private int imageListSize = 0;
    RetrofitApi retrofitApi;
    Call<JsonArray> callImageList;
    Call<ResponseBody> callImageDownload;
    boolean isCanceled = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();
        if(intent.getAction() == ("STOP_SERVICE")){
            if(callImageDownload != null){
                callImageDownload.cancel();
                isCanceled = true;
            }
        }
        return START_STICKY;
    }

    private void createNotification() {
        int requestID = (int) System.currentTimeMillis();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("id", "an", NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription("no sound");
            notificationChannel.setSound(null, null);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, requestID,notificationIntent, 0);


        notificationBuilder = new NotificationCompat.Builder(this, "id")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Download")
                .setContentText("Downloading Image")
                .setDefaults(0)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        notificationManager.notify(0, notificationBuilder.build());

        initRetrofit();
    }

    private void initRetrofit() {
        Log.d("TRACKING", "startImageDownload: 3");
        retrofitApi = ApiClient.getClient().create(RetrofitApi.class);

        callImageList = retrofitApi.getImageDetailsList(1, 30);
        callImageList.enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> callImageList, Response<JsonArray> response) {
                ArrayList<Image> images = new Gson().fromJson(response.body(), new TypeToken<ArrayList<Image>>() {}.getType());
                imageListSize = images.size();
                downloadImage(images);
//                for(final Image img : images){
//                    itemCount++;
//                    Call<ResponseBody> callImageDownload = retrofitApi.downloadImage(img.getDownload_url());
//                    callImageDownload.enqueue(new Callback<ResponseBody>() {
//                        @Override
//                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> rs) {
//                            try {
//                                    downloadImage(rs.body(), img.getId());
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void onFailure(Call<ResponseBody> call, Throwable t) {
//
//                        }
//                    });
//
//                    // if you want to cancel download............
////                    if(itemCount==5){
////                        callImageList.cancel();
////                        callImageDownload.cancel();
////                        break;
////                    }
//
//                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                t.getMessage();
            }
        });
    }

    private void downloadImage(final ArrayList<Image> images) {

        callImageDownload = retrofitApi.downloadImage(images.get(itemCount).getDownload_url());
        callImageDownload.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> rs) {
                try {
                    createImage(rs.body(), images.get(itemCount).getId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                itemCount++;
                if(isCanceled){
                    return;
                }
                downloadImage(images);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });

    }

    private void createImage(ResponseBody body, int id) throws IOException {

        int count;
        byte data[] = new byte[1024 * 4];
        long fileSize = body.contentLength();
        InputStream inputStream = new BufferedInputStream(body.byteStream(), 1024 * 8);
        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "downloaded_image_"+id+".jpg");
        OutputStream outputStream = new FileOutputStream(outputFile);
        long total = 0;
        String downloadComplete = null;
        //int totalFileSize = (int) (fileSize / (Math.pow(1024, 2)));

        while ((count = inputStream.read(data)) != -1) {

            total += count;
            int progress = (int) ((double) (total * 100) / (double) fileSize);


            updateNotification(progress);
            outputStream.write(data, 0, count);
            downloadComplete = "downloaded_image_"+id+".jpg";
        }
        onDownloadComplete(downloadComplete);
        outputStream.flush();
        outputStream.close();
        inputStream.close();

    }

    private void updateNotification(int currentProgress) {


        notificationBuilder.setProgress(100, currentProgress, false);
        notificationBuilder.setContentText("Downloaded: " + currentProgress + "%");
        notificationManager.notify(0, notificationBuilder.build());
    }


    private void sendProgressUpdate(String downloadComplete) {

        Intent intent = new Intent(MainActivity.PROGRESS_UPDATE);
        intent.putExtra("downloadComplete", downloadComplete);
        LocalBroadcastManager.getInstance(MyService.this).sendBroadcast(intent);
    }

    private void onDownloadComplete(String downloadComplete) {
        sendProgressUpdate(downloadComplete);

        if(itemCount == imageListSize){
            notificationManager.cancel(0);
        }
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.setContentText("Image Download Complete");
        notificationManager.notify(0, notificationBuilder.build());

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        notificationManager.cancel(0);
    }
}
