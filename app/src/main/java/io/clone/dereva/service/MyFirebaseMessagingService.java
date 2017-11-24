package io.clone.dereva.service;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import io.clone.dereva.CustomerCallActivity;

/**
 * Created by walter on 11/22/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    public static final String TAG="NOTIFY";
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage!=null) {
            LatLng customer_location=new Gson().fromJson(remoteMessage.getNotification().getBody(), LatLng.class);
            Intent intent=new Intent(getBaseContext(), CustomerCallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("lat",customer_location.latitude);
            intent.putExtra("lng",customer_location.longitude);
            intent.putExtra("customer", remoteMessage.getNotification().getTitle());
            startActivity(intent);
        }else{
            Log.d(TAG, "onMessageReceived: Error receiving notification");
        }

    }
}
