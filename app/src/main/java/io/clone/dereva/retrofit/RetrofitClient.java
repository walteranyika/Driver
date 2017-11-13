package io.clone.dereva.retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by walter on 11/13/17.
 */

public class RetrofitClient {
    private static Retrofit retrofit=null;
    public  static  Retrofit getClient(String baseURL){
        if (retrofit==null){
            retrofit=new Retrofit.Builder()
                    .baseUrl(baseURL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
