package com.nexuslink.alphrye.api;

import com.nexuslink.alphrye.net.bean.CommonNetBean;
import com.nexuslink.alphrye.net.bean.UserBean;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 *    author : alphrye
 *    time   : 2018/12/29
 *    desc   : 通用请求API
 */
public interface CommonApiService {
    @GET("/v1/user_profile")
    Call<CommonNetBean<UserBean>> requestUserProfile();
}
