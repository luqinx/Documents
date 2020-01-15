package chao.app.myapplication;

import android.app.Application;

import chao.android.tools.servicepool.AndroidServicePool;

/**
 * @author luqin
 * @since 2020-01-15
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AndroidServicePool.init(this);
    }
}
