package cn.whenpigsfly.rn.apsara;

import android.content.Context;
import android.util.Log;

import com.aliyun.loader.MediaLoader;
import com.aliyun.player.AliPlayerGlobalSettings;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.IllegalViewOperationException;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.File;

public class ApsaraPlayerModule extends ReactContextBaseJavaModule {
    private ReactApplicationContext mReactContext;

    @Override
    public String getName() {
        return "ApsaraPlayerModule";
    }

    public ApsaraPlayerModule(ReactApplicationContext context) {
        super(context);
        mReactContext = context;
    }

    @ReactMethod
    public void save(final ReadableMap options, final int reactTag, final Promise promise) {
        try {
            UIManagerModule uiManager = mReactContext.getNativeModule(UIManagerModule.class);
            uiManager.addUIBlock(new UIBlock() {
                public void execute(NativeViewHierarchyManager nvhm) {
                    ApsaraPlayerView view = (ApsaraPlayerView) nvhm.resolveView(reactTag);
                    view.save(options, promise);
                }
            });
        } catch (IllegalViewOperationException e) {
            promise.reject("ERROR", e);
        }
    }

    @ReactMethod
    public void destroy(final int reactTag) {
        try {
            UIManagerModule uiManager = mReactContext.getNativeModule(UIManagerModule.class);
            uiManager.addUIBlock(new UIBlock() {
                public void execute(NativeViewHierarchyManager nvhm) {
                    ApsaraPlayerView view = (ApsaraPlayerView) nvhm.resolveView(reactTag);
                    view.destroy();
                }
            });
        } catch (IllegalViewOperationException e) {
            // ignore
        }
    }

    @ReactMethod
    public void setGlobalSettings(final Promise promise) {
        //expireMin - 缓存多久过期：单位分钟，默认值30天，过期的缓存不管容量如何，都会在清理时淘汰掉；
        //maxCapacityMB - 最大缓存容量：单位兆，默认值20GB，在清理时，如果缓存总大小超过此大小，会以cacheItem为粒度，按缓存的最后时间排序，一个一个淘汰掉一些缓存，直到小于等于最大缓存容量；
        //freeStorageMB - 磁盘最小空余容量：单位兆，默认值0，在清理时，同最大缓存容量，如果当前磁盘容量小于该值，也会按规则一个一个淘汰掉一些缓存，直到freeStorage大于等于该值或者所有缓存都被干掉；
        AliPlayerGlobalSettings.enableLocalCache(true, 1024 * 10, localCacheDir);

        AliPlayerGlobalSettings.setCacheFileClearConfig(24 * 60 * 2, 1024 * 2, 1024 * 2);

        //enable - true：开启本地缓存。false：关闭。默认关闭。
        //maxBufferMemoryKB - 设置单个源的最大内存占用大小。单位KB
        //localCacheDir - 本地缓存的文件目录，绝对路径
        String localCacheDir = getAliVideoPreloadDir(mReactContext);
    }

    public static String getAliVideoPreloadDir(Context context) {
        return ApsaraPlayerView.getDiskCachePath(context.getApplicationContext()) + File.separator + "aliplayer/preloadCache" + File.separator;
    }

    /**
     * @param url     预加载 url
     * @param promise 回调
     */
    @ReactMethod
    public void preLoadUrl(final String url, final Promise promise) {

//        Log.e("AAA", "preLoadUrl:" + url);
        MediaLoader mediaLoader = MediaLoader.getInstance();

        DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter = mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

        /**
         * 设置加载状态回调
         */
        mediaLoader.setOnLoadStatusListener(new MediaLoader.OnLoadStatusListener() {
            @Override
            public void onError(String url, int code, String msg) {
                //加载出错
                WritableMap params = Arguments.createMap();
                params.putString("url", url);
                params.putInt("code", code);
                params.putString("msg", msg);
                eventEmitter.emit("onError", params);

//                Log.e("AAA", "onError preLoadUrl:" + url);
            }

            @Override
            public void onCompleted(String s) {
                //加载完成
                WritableMap params = Arguments.createMap();
                params.putString("url", url);
                eventEmitter.emit("onCompleted", params);

//                Log.e("AAA", "onCompleted preLoadUrl:" + url);
            }

            @Override
            public void onCanceled(String s) {
                //加载取消
                WritableMap params = Arguments.createMap();
                params.putString("url", url);
                eventEmitter.emit("onCanceled", params);

//                Log.e("AAA", "onCanceled preLoadUrl:" + url);
            }
        });

        /**
         * 开始加载文件。异步加载。可以同时加载多个视频文件。
         * @param url - 视频文件地址。
         * @param duration - 加载的时长大小，单位：毫秒。
         */
        mediaLoader.load(url, 10 * 1000);
    }

}
