package io.clone.dereva.common;

import io.clone.dereva.retrofit.IGoogleApi;
import io.clone.dereva.retrofit.RetrofitClient;

/**
 * Created by walter on 11/13/17.
 */

public class Common {
    public static  final  String baseURL="https://maps.googleapis.com";
    public  static IGoogleApi getGoogleApi(){
        return RetrofitClient.getClient(baseURL).create(IGoogleApi.class);
    }
}
