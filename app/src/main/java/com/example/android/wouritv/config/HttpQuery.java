package com.example.android.wouritv.config;

import android.os.Build;

import java.io.IOException;

import androidx.annotation.RequiresApi;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Sumbang on 31/08/2017.
 */

public class HttpQuery {

    private OkHttpClient client = new OkHttpClient();

    private String ENDPOINT = "https://www.wouri.tv/webservice.php";
    //private String ENDPOINT = "http://192.168.1.13/wouritv/api/service.php";

    public HttpQuery(){

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String run(String url) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String film(String url,String idvideo) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("idfilm",idvideo)
                //.addFormDataPart("user",CurrentUser.getInstance().getId())
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String serie(String url,String idvideo) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("serie",idvideo)
                //.addFormDataPart("user",CurrentUser.getInstance().getId())
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String accesfilm(String url,String idvideo,String code) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("film",idvideo)
                .addFormDataPart("code",code)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String allfilm(String url,String categorie) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("categorie",categorie)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String allserie(String url) throws IOException {

        // if(CurrentUser.getInstance() == null) CurrentUser.getInstance().setId("0");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("user","0")
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String recherche(String url) throws IOException {

        // if(CurrentUser.getInstance() == null) CurrentUser.getInstance().setId("0");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("user","0")
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getcode(String url,String code,String details) throws IOException {

        if(CurrentUser.getInstance() == null) CurrentUser.getInstance().setId("0");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("code",code)
                .addFormDataPart("details",details)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getcode(String url,String user) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("user", user)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }



    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String haveaccess(String url,String user,String film) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("user", user)
                .addFormDataPart("film", film)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String haveaccess2(String url,String user,String serie) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("user", user)
                .addFormDataPart("serie", serie)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getcode2(String url,String iduser) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("user",iduser)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getcode3(String url,String code) throws IOException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", url)
                .addFormDataPart("code",code)
                .build();

        okhttp3.Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Autorisation","a0adki7do0wgo8ockgscww8gs8o0kk4")
                .header("Ressource",url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            return response.body().string();
        }

    }
}
