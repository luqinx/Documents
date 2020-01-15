package chao.app.debug;

import chao.java.tools.servicepool.IInitService;
import chao.java.tools.servicepool.annotation.Init;
import chao.java.tools.servicepool.annotation.Service;

/**
 * @author luqin
 * @since 2020-01-15
 */
@Init(lazy = false)
@Service
public class DebugInit implements IInitService {

    @Override
    public void onInit() {
        //do something
    }
}
