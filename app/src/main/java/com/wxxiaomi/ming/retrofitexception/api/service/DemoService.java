package com.wxxiaomi.ming.retrofitexception.api.service;

import com.wxxiaomi.ming.retrofitexception.bean.format.InitUserInfo;
import com.wxxiaomi.ming.retrofitexception.bean.format.Login;
import com.wxxiaomi.ming.retrofitexception.bean.format.common.Result;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by 12262 on 2016/5/31.
 */
public interface DemoService {
    @GET("ActionServlet?action=login")
    Observable<Result<Login>> readBaidu(@Query("username") String username, @Query("password") String password);

    @GET("ActionServlet?action=inituserinfo")
    Observable<Result<InitUserInfo>> initUserInfo(@Query("username") String username, @Query("password") String password);
}
