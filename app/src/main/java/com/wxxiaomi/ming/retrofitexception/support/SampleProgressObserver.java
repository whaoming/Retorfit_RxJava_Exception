package com.wxxiaomi.ming.retrofitexception.support;

import com.wxxiaomi.ming.retrofitexception.api.exception.ApiException;

/**
 * Created by 12262 on 2016/6/5.
 */
public abstract class SampleProgressObserver<T> extends MyObserver<T>{
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onError(ApiException ex) {

    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onNext(T t) {

    }
}
