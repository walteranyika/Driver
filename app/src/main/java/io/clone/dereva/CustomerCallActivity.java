package io.clone.dereva;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.clone.dereva.common.Common;
import io.clone.dereva.models.FCMResponse;
import io.clone.dereva.models.Notification;
import io.clone.dereva.models.Sender;
import io.clone.dereva.models.Token;
import io.clone.dereva.retrofit.IFCMService;
import io.clone.dereva.retrofit.IGoogleApi;
import io.clone.dereva.service.SendNotification;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerCallActivity extends AppCompatActivity {

    private static final String TAG = "CUSTOMER_CALL";
    TextView txtTime, txtDistance, txtAddress;
    MediaPlayer mMediaPlayer;
    IGoogleApi mService;

    Button btnAccept, btnCancel;

    String customerId="";

    IFCMService mFCMService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_call);
        txtAddress=(TextView) findViewById(R.id.txtAddress);
        txtTime=(TextView) findViewById(R.id.txtTime);
        txtDistance=(TextView) findViewById(R.id.txtDistance);
        btnAccept=(Button) findViewById(R.id.accept_btn);
        btnCancel=(Button) findViewById(R.id.cancel_btn);

        mService=Common.getGoogleApi();
        mFCMService=Common.getFCMService();

        mMediaPlayer=MediaPlayer.create(this,R.raw.ringtone);

        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();
        if (getIntent()!=null){
            double lat=getIntent().getDoubleExtra("lat",-1.0);
            double lng=getIntent().getDoubleExtra("lng",-1.0);
            customerId=getIntent().getStringExtra("customer");
            getDirection(lat,lng);
        }
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!customerId.isEmpty() ){
                    cancelBooking(customerId);
                }
            }
        });


    }

    private void cancelBooking(String customerRegToken) {
        SendNotification notification=new SendNotification();
        notification.sendNotification(customerRegToken,"Notice","Driver has cancelled your request","Ride Cancelled","Try Again");
     /* Token token=new Token(customerId);
        Notification notification=new Notification("Notice","Driver has cancelled your request");
        Sender sender=new Sender(token.getToken(),notification);

        mFCMService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success==1){
                    Log.d(TAG, "onResponse: Cancelled Request");
                    Toast.makeText(CustomerCallActivity.this, "Request Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });*/

    }

    private void getDirection(double lat,double lng) {

        String requestApi=null;
        try{
            requestApi="https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+ Common.mLastLocation.getLatitude()+","+Common.mLastLocation.getLongitude()  +"&"+
                    "destination="+lat+","+lng+"&"+
                    "key="+getResources().getString(R.string.google_maps_key);

            Log.d(TAG, "getDirection: "+requestApi);

            mService.getPath(requestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject=new JSONObject(response.body().toString());
                        JSONArray routesArray=jsonObject.getJSONArray("routes");
                        JSONObject object=routesArray.getJSONObject(0);
                        JSONArray legs=object.getJSONArray("legs");

                        JSONObject legsObject=legs.getJSONObject(0);

                        JSONObject distance=legsObject.getJSONObject("distance");

                        txtDistance.setText(distance.getString("text"));

                        JSONObject time=legsObject.getJSONObject("duration");

                        txtTime.setText(time.getString("text"));

                        String address=legsObject.getString("end_address");

                        txtAddress.setText(address);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(CustomerCallActivity.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        mMediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onPause() {
        mMediaPlayer.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaPlayer.start();
    }
}
