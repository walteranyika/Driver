package io.clone.dereva.retrofit;

import io.clone.dereva.models.FCMResponse;
import io.clone.dereva.models.Sender;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by walter on 11/22/17.
 */

public interface IFCMService {
    @Headers({"Content-Type:application/json",
    "Authorization:key=AAAA6Y596Js:APA91bHIifVrIaLi6eJNAVuedO8hS4ekHH3eXjDZGkp3nNC-bbgQ-RP84W98zEiEdUOj6LPFba7v9hntwHBoCQLcAyRsg1V0m5Rl3QzJORfoORq6YN9384gYi_yqIDO4C_ML25KBcnFo"})

    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body Sender body);

}
