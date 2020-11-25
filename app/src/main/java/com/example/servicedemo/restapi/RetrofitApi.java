package com.example.servicedemo.restapi;
import com.example.servicedemo.utils.URLS;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;

import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface RetrofitApi {

    @GET(URLS.IMAGE_DETAILS_LIST_URL)
    Call<JsonArray> getImageDetailsList(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET()
    @Streaming
    Call<ResponseBody> downloadImage(@Url String fileUrl);
}
