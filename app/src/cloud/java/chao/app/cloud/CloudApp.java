package chao.app.cloud;

import chao.app.myapplication.App;

/**
 * @author luqin
 * @since 2020-01-15
 */
public class CloudApp extends App {

    @Override
    public void onCreate() {
        super.onCreate();
        LoginManager.login();
    }
}
