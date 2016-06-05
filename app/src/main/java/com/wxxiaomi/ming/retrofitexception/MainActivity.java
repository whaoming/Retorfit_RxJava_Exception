package com.wxxiaomi.ming.retrofitexception;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.wxxiaomi.ming.retrofitexception.bean.format.Login;
import com.wxxiaomi.ming.retrofitexception.api.exception.ApiException;
import com.wxxiaomi.ming.retrofitexception.api.HttpMethods;
import com.wxxiaomi.ming.retrofitexception.support.MyObserver;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpMethods.getInstance().login("122627018","9879879877")
                .subscribe(new MyObserver<Login>() {
                    @Override
                    protected void onError(ApiException ex) {
//                        ex.printStackTrace();
                        Log.i("wang", ex.getDisplayMessage());

                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onNext(Login login) {
                        Log.i("wang",login.userInfo.toString());
                    }
                });
    }
}
