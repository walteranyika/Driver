package io.clone.dereva.retrofit;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by walter on 11/13/17.
 */

public interface IGoogleApi {
    @GET
    Call<String> getPath(@Url String url);
}
