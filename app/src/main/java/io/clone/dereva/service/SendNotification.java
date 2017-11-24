package io.clone.dereva.service;

import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by walter on 11/24/17.
 */

public class SendNotification {
    public static final String TAG="SEND_NOTIF";
    public static final MediaType JSON= MediaType.parse("application/json; charset=utf-8");

    public void sendNotification(final String reg_token_receiver,final String bodyContents,final String titleContent,final String extraOne,final String extraTwo) {
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json=new JSONObject();
                    JSONObject dataJson=new JSONObject();
                    dataJson.put("body",bodyContents);
                    dataJson.put("title", titleContent);
                    json.put("notification",dataJson);

                    JSONObject objExtra=new JSONObject();
                    objExtra.put("extraOne",extraOne);
                    objExtra.put("extraTwo",extraTwo);

                    json.put("data", objExtra);

                    json.put("to",reg_token_receiver);
                    RequestBody body = RequestBody.create(JSON, json.toString());
                    Log.d(TAG, "doInBackground: REQUEST_BODY : "+json.toString());
                    Request request = new Request.Builder()
                            .header("Authorization","key=AAAA6Y596Js:APA91bHIifVrIaLi6eJNAVuedO8hS4ekHH3eXjDZGkp3nNC-bbgQ-RP84W98zEiEdUOj6LPFba7v9hntwHBoCQLcAyRsg1V0m5Rl3QzJORfoORq6YN9384gYi_yqIDO4C_ML25KBcnFo")
                            .url("https://fcm.googleapis.com/fcm/send")
                            .post(body)
                            .build();
                    Response response = client.newCall(request).execute();
                    String finalResponse = response.body().string();
                    Log.d(TAG, "doInBackground: "+finalResponse);
                }catch (Exception e){
                    //Log.d(TAG,e+"");
                }
                return null;
            }
        }.execute();

    }
}
