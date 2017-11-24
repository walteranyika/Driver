package io.clone.dereva.retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by walter on 11/23/17.
 */

public class FCMClient {
    private static Retrofit retrofit=null;
    public  static  Retrofit getClient(String baseURL){
        if (retrofit==null){
            retrofit=new Retrofit.Builder()
                    .baseUrl(baseURL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
