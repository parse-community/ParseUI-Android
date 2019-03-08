package com.parse.ui.widget.sample;

import android.app.Application;

import com.parse.Parse;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.Configuration configuration = new Parse.Configuration.Builder(this)
                .server("http://localhost")
                .build();
        Parse.initialize(configuration);
    }
}
