package io.clone.dereva.common;

import android.location.Location;

import io.clone.dereva.retrofit.FCMClient;
import io.clone.dereva.retrofit.IFCMService;
import io.clone.dereva.retrofit.IGoogleApi;
import io.clone.dereva.retrofit.RetrofitClient;

/**
 * Created by walter on 11/13/17.
 */

public class Common {
   //TokensTable
   public static final String TOKENS_TABLE="Tokens";

    public static Location mLastLocation=null;
    //Table to store all available drivers
    public static final String DRIVERS_TABLE="Drivers";
    //Table to store information about Pickup Request
    public static final String PICKUP_REQUEST_TABLE="PickupRequest";


    //Table to store Info about registered taxi customers
    public static final String USER_RIDERS_TABLE="RidersInformation";
    //Table to store Info about registered taxi drivers
    public static final String USER_DRIVERS_TABLE="DriversInformation";

    public static  final  String baseURL="https://maps.googleapis.com";
    public static  final  String fcmURL="https://fcm.googleapis.com/";
    public  static IGoogleApi getGoogleApi(){
        return RetrofitClient.getClient(baseURL).create(IGoogleApi.class);
    }
    public  static IFCMService getFCMService(){
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }


}
