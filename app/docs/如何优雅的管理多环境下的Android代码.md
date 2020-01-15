日常开发过程中除了主工程代码，肯定也少不了调试代码,如日志、调试开关、调试工具、功能Mock等等，此时就需要一个开发阶段使用的开发环境。现在很多项目都使用云测试，三方云测试平台(如testin,
阿里云等等)测试时往往无法跳过登录，所以要在云测的安装包中内置登录token,还要指定云测接口等等一些云测场景下的一些特殊功能，此时就需要一个专为三方云测平台使用的云测环境。项目上线当然需要有线上环境
此外AS/Gradle还内置了debug环境和release环境。那么多不同的环境,代码如何去做管理?
又如何把不同环境的代码完全隔离开来?我们必须要保证调试、Mock等等这些业务无关的代码完全不能影响到线上。


**首先, 我们使用AS新建一个项目, 我们看看AS/Gradle有什么？**

<div align="center">
<img src="https://user-gold-cdn.xitu.io/2020/1/15/16fa7deb5a6f04af?w=444&h=422&f=png&s=25887" width="300" height="300"/>
</div>


如上图所示, AS已经在src目录下创建三个SourceSet目录，分别是androidTest, main和test,其中main是主工程目录，其他两个是用于单元测试目录。

**AS/Gradle只有这三个SourceSet目录吗？**

当然不是！我们知道,build.gradle配置文件中，我们可以配置buildTypes.默认已经有debug和release两个buildType. 当然我们也可以自定义一个buildType名字叫custom.


```
android {
    ...
    
    buildTypes {
        debug {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        custom {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

执行同步操作后，我们点开AS左侧的"build Variants",我们可以选择具体使用哪个buildType

<div align="center">
<img src="https://user-gold-cdn.xitu.io/2020/1/15/16fa7f39f505e394?w=732&h=600&f=png&s=50325" width="330" height="300"/>
</div>


**很多人可能会问, 这有什么用?**

这真的很有用！

上面说过AS已经在src目录下创建了名为"main"的SourceSet目录和另外两个test目录。此时我们可以在src目录下再创建一个debug/release/custom目录,目录下的内容和main目录下内容一致。main目录一直处于激活状态,但同一时刻只会有一个buildType的SourceSet目录会被激活，如下图所示。编译时,Gradle会自动合并多个SourceSet目录。
<div align="center">
<img src="https://user-gold-cdn.xitu.io/2020/1/15/16fa80b159da332c?w=692&h=1336&f=png&s=138322" width="300" height="560"/>
</div>


**AS/Gradle还支持根据buildType引入其他module或者三方仓库**

```
    dependencies {
        ...
        debugImplementation project(":debugLib1")
        debugApi 'com.android.support:appcompat-v7:28.0.0'
        
        customImplementation project(":customLib")
        customApi 'com.google.code.gson:gson:2.8.5'
        ...
    }
```

除了buildType,AS/Gradle还支持productFlavors,内容和buildTypes类似,可自行百度这里不再赘述。需要强调的是Gradle在编译时可以同时有一个buildType，一个productFlavor和main三个SourceSet共存，编译过程中会自动合并三个SourceSet目录.


现在，如何使用多环境已经显而易见了，我们只要按需自定义buildType/productFlavor,然后将调试或者Mock的代码放到对应的buildType/productFlavor的SourceSet下就可以了。

**这样真的就一切顺利了吗？**

实际操作过程中会发现，buildType/productFlavor是可以直接引用到main下面的代码,相反却是不可以的,一旦引用会导致buildType/productFlavor切换的时候,找不到向对应的类。

问题来了,既然main无法直接引用buildType/productFlavor中的代码，那么buildType/productFlavor中的代码如何初始化？main又如何调用这些代码？

就以上面说的云测环境做例子,假设我为云测创建了一个productFlavor名字叫cloud，我想要在云测环境app执行初始化的时候调用一次登录接口，这样就可以内置token而不需要在云测环境手动登录了,

```

public class LoginManager {

    public static void login() {
        //do login
    }
}
```


如何触发这个登录操作是一个问题。通常我们会在自定义Application的onCreate()方法中执行初始化。

```
public class App extends Application {
    
    @Override
    public void onCreate() {
        LoginManager.login(); //这里不应该直接引用LoginManager, 因为LoginManager在productFlavor为cloud的SourceSet中, 一旦切到其他productFlavor,LoginManager类就找不到了
    }
}
```

**有一个办法**
在cloud中新建一个CloudApp继承自main中的App,然后在could的AndroidManifest中引用

```
<application
        android:name="chao.app.cloud.CloudApp" 
        tools:replace="android:name" //必要, 使用CloudApp替换App
        ...
        />
```

```
public class CloudApp extends App {

    @Override
    public void onCreate() {
        super.onCreate();
        LoginManager.login();  //这样就不用担心buildType/productFlavor切换导致类找不到的问题了
    }
}
```

**简单的场景里这个方法是很有效的,场景再复杂一点呢**

如果BuildVariants选择cloudDebug,即productFlavor是cloud,buildType是debug,debug也需要有自己的初始化,但是AndroidManifest中Application最终只能有一个。所以稍微复杂点的场景问题会变得很棘手。
另外由于存在多环境切换，buildType/productFlavor中的类是不能直接被调用的


**如何解决**

[ServicePool](https://juejin.im/post/5e1e76c6518825267f699a3d)是一个用于组件化通信的神器。可以很有效的解决上述问题。

使用ServicePool初始化Service初始化代替Application初始化，cloud中定义一个CloudInit,debug中定义一个DebugInit.

```
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AndroidServicePool.init(this); //初始化ServicePool
    }
}
```

```
@Init(lazy = false) //ServicePool默认所有组件使用懒加载，这里取消掉懒加载模式，让ServicePool初始化后理解加载这个Service
@Service
public class CloudInit implements IInitService {

    @Override
    public void onInit() {
        LoginManager.login();

    }
}
```

```
@Init(lazy = false) //ServicePool默认所有组件使用懒加载，这里取消掉懒加载模式，让ServicePool初始化后理解加载这个Service
@Service
public class DebugInit implements IInitService {
    
    @Override
    public void onInit() {
        //do something
    }
}
```




