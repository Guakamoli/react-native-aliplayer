package cn.whenpigsfly.rn.apsara;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;

import com.aliyun.downloader.AliDownloaderFactory;
import com.aliyun.downloader.AliMediaDownloader;
import com.aliyun.loader.MediaLoader;
import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
import com.aliyun.player.AliPlayerGlobalSettings;
import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.ErrorInfo;
import com.aliyun.player.bean.InfoBean;
import com.aliyun.player.bean.InfoCode;
import com.aliyun.player.nativeclass.CacheConfig;
import com.aliyun.player.nativeclass.MediaInfo;
import com.aliyun.player.nativeclass.PlayerConfig;
import com.aliyun.player.nativeclass.TrackInfo;
import com.aliyun.player.source.UrlSource;
import com.aliyun.player.source.VidAuth;
import com.aliyun.player.source.VidSts;
import com.cicada.player.utils.FrameInfo;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ApsaraPlayerView extends FrameLayout implements
        AliPlayer.OnInfoListener,
        AliPlayer.OnErrorListener,
        AliPlayer.OnCompletionListener,
        AliPlayer.OnPreparedListener,
        AliPlayer.OnRenderingStartListener,
        AliPlayer.OnStateChangedListener,
        AliPlayer.OnSeekCompleteListener {


    public enum Events {
        EVENT_END("onVideoEnd"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_SEEK("onVideoSeek"),
        EVENT_ERROR("onVideoError"),
        EVENT_FIRST_RENDERED_START("onVideoFirstRenderedStart"),
        EVENT_PROGRESS("onVideoProgress");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    private ThemedReactContext mContext;

    public ThemedReactContext getThemedReactContext() {
        return mContext;
    }

    private RCTEventEmitter mEventEmitter;
    private AliMediaDownloader mDownloader = null;
    private Promise mDownloaderPromise;

    private Map<String, Object> mSource;
    private AliPlayer mPlayer;
    private int mPlayerType;
    private LifecycleEventListener mLifecycleEventListener;
    private boolean mPrepared = false;

    private IPlayer.ScaleMode mScaleMode = IPlayer.ScaleMode.SCALE_ASPECT_FIT;
    private boolean isPaused;
    private boolean isSeek;
    private long mSeekTime;

    public ApsaraPlayerView(ThemedReactContext context, AliPlayer player) {
        super(context);
        mContext = context;
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        initLifecycle();
    }

    public void init(AliPlayer player, int type) {
        mPlayer = player;
        mPlayerType = type;
        if (mPlayer == null) {
            mPlayer = AliPlayerFactory.createAliPlayer(mContext.getApplicationContext());
        }
        mPlayer.setTraceId("DisableAnalytics");
        mPlayer.setScaleMode(mScaleMode);
        //配置缓存和延迟控制,先获取配置
        PlayerConfig config = mPlayer.getConfig();
        //最大延迟。注意：直播有效。当延时比较大时，播放器sdk内部会追帧等，保证播放器的延时在这个范围内。
        config.mMaxDelayTime = 5000;
        // 最大缓冲区时长。单位ms。播放器每次最多加载这么长时间的缓冲数据。
        config.mMaxBufferDuration = 50000;
        //高缓冲时长。单位ms。当网络不好导致加载数据时，如果加载的缓冲时长到达这个值，结束加载状态。
        config.mHighBufferDuration = 3000;
        // 起播缓冲区时长。单位ms。这个时间设置越短，起播越快。也可能会导致播放之后很快就会进入加载状态。
        config.mStartBufferDuration = 500;
        config.mPositionTimerIntervalMs = 500;
        //其他设置
        //往前缓存的最大时长。单位ms。默认为0。
        config.mMaxBackwardBufferDurationMs = 500;
        //设置配置给播放器
        mPlayer.setConfig(config);

//        AliPlayerGlobalSettings.setCacheFileClearConfig(24 * 60 * 3, 1024 * 20, 0);
//        String localCacheDir = ApsaraPlayerModule.getAliVideoPreloadDir(mContext);
//        AliPlayerGlobalSettings.enableLocalCache(true, 1024 * 10, localCacheDir);
    }

    private void initLifecycle() {
        mLifecycleEventListener = new LifecycleEventListener() {
            @Override
            public void onHostResume() {
                if (!isPaused && mPlayer != null) {
                    mPlayer.start();
                }
            }

            @Override
            public void onHostPause() {
                if (mPlayer != null) {
                    mPlayer.pause();
                }
            }

            @Override
            public void onHostDestroy() {

            }
        };
        mContext.addLifecycleEventListener(mLifecycleEventListener);
    }

    public void prepare() {
        if (mPlayer == null) {
            return;
        }
        VidSts sts = getStsSource(mSource.get("sts"));
        VidAuth auth = getAuthSource(mSource.get("auth"));
        mPlayer.clearScreen();
        if (sts != null) {
            Log.e("AAA", "prepare:" + sts);
            mPlayer.setDataSource(sts);
        } else if (auth != null) {
            Log.e("AAA", "prepare:" + auth);
            mPlayer.setDataSource(auth);
        } else if (mSource.get("uri") != null && !String.valueOf(mSource.get("uri")).isEmpty()) {
            UrlSource source = new UrlSource();
            source.setUri((String) mSource.get("uri"));
            mPlayer.setDataSource(source);
        }

        mPlayer.prepare();

        mPlayer.setAutoPlay(!isPaused);


        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnErrorListener(this);

        mPlayer.setOnRenderingStartListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnStateChangedListener(this);

        mPlayer.setOnSeekCompleteListener(this);

        mPrepared = true;
    }

    private TextureView mTextureView;

    public void setPaused(final boolean paused) {
        if (mPlayer != null) {
            if (paused) {
                mPlayer.pause();
            } else {
                mPlayer.start();
            }
        }
        this.isPaused = paused;
    }

    public void setRepeat(final boolean repeat) {
        if (mPlayer != null) {
            mPlayer.setLoop(repeat);
        }
    }

    public void setMuted(final boolean muted) {
        if (mPlayer != null) {
            mPlayer.setMute(muted);
        }
    }

    public void setVolume(final float volume) {
        if (mPlayer != null) {
            mPlayer.setVolume(volume);
        }
    }

    public void setVideoBackgroundColor(@ColorInt final int videoBackgroundColorInt) {
        if (mPlayer != null) {
            mPlayer.setVideoBackgroundColor(videoBackgroundColorInt);
        }
    }

    public void setSource(final Map source) {
        mSource = source;

//        if (mSource.get("uri") != null && !String.valueOf(mSource.get("uri")).isEmpty()) {
//            preLoadUrl((String) mSource.get("uri"));
//        }
        prepare();
    }

    public void setSeek(long position) {
        if (mPlayer != null) {
            mPlayer.seekTo(position, IPlayer.SeekMode.Accurate);
        }
        isSeek = true;
        mSeekTime = position;
    }


    public void setCacheEnable(boolean cacheEnable) {
        if (mPlayer != null && cacheEnable) {
            //本地缓存
            CacheConfig cacheConfig = new CacheConfig();
            //开启缓存功能
            cacheConfig.mEnable = true;
            //能够缓存的单个文件最大时长。超过此长度则不缓存
            cacheConfig.mMaxDurationS = 300;
            //缓存目录的位置
            String dirPath = getAliVideoCacheDir(mContext);
            cacheConfig.mDir = dirPath;
            //缓存目录的最大大小。超过此大小，将会删除最旧的缓存文件
            cacheConfig.mMaxSizeMB = 20 * 1024;
            //设置缓存配置给到播放器
            mPlayer.setCacheConfig(cacheConfig);
        }
    }

    public void setPositionTimerIntervalMs(final int positionTimerIntervalMs) {
        if (mPlayer != null) {
            PlayerConfig config = mPlayer.getConfig();
            config.mPositionTimerIntervalMs = positionTimerIntervalMs;
            mPlayer.setConfig(config);
        }
    }

    public void setResizeMode(final String resizeMode) {
        if ("contain".equals(resizeMode)) {
            mScaleMode = IPlayer.ScaleMode.SCALE_ASPECT_FIT;
            mPlayer.setScaleMode(IPlayer.ScaleMode.SCALE_ASPECT_FIT);
        } else if ("cover".equals(resizeMode)) {
            mScaleMode = IPlayer.ScaleMode.SCALE_ASPECT_FILL;
            mPlayer.setScaleMode(IPlayer.ScaleMode.SCALE_ASPECT_FILL);
        } else {
            mScaleMode = IPlayer.ScaleMode.SCALE_ASPECT_FIT;
            mPlayer.setScaleMode(IPlayer.ScaleMode.SCALE_ASPECT_FIT);
        }
    }

    private VidSts getStsSource(Object obj) {
        if (obj == null) {
            return null;
        }

        Map<String, String> opts = (Map<String, String>) obj;
        if (!opts.containsKey("vid")
                || !opts.containsKey("accessKeyId")
                || !opts.containsKey("accessKeySecret")
                || !opts.containsKey("securityToken")) {
            return null;
        }

        VidSts sts = new VidSts();
        sts.setVid(opts.get("vid"));
        sts.setAccessKeyId(opts.get("accessKeyId"));
        sts.setAccessKeySecret(opts.get("accessKeySecret"));
        sts.setSecurityToken(opts.get("securityToken"));
        sts.setRegion(opts.containsKey("region") ? opts.get("region") : "");
        return sts;
    }

    private VidAuth getAuthSource(Object obj) {
        if (obj == null) {
            return null;
        }

        Map<String, String> opts = (Map<String, String>) obj;
        if (!opts.containsKey("vid") || !opts.containsKey("playAuth")) {
            return null;
        }

        VidAuth auth = new VidAuth();
        auth.setVid(opts.get("vid"));
        auth.setPlayAuth(opts.get("playAuth"));
        auth.setRegion(opts.containsKey("region") ? opts.get("region") : "");
        return auth;
    }

    /**
     * 播放完成之后，就会回调到此接口。
     */
    @Override
    public void onCompletion() {
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), null);
    }


    /**
     * 播放器中的一些信息，包括：当前进度、缓存位置等等
     */
    @Override
    public void onInfo(InfoBean info) {
        if (info.getCode() == InfoCode.CurrentPosition) {
            WritableMap map = Arguments.createMap();
            map.putDouble("duration", mPlayer.getDuration());
            map.putDouble("currentTime", info.getExtraValue());
            mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), map);
        }
    }

    /**
     * 出错后需要停止掉播放器
     */
    @Override
    public void onError(ErrorInfo errorInfo) {
        WritableMap map = Arguments.createMap();
        map.putInt("code", errorInfo.getCode().getValue());
        map.putString("message", errorInfo.getMsg());
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), map);
        if (mPlayer != null) {
            mPlayer.release();
        }
    }

    /**
     * 播放器状态变更通知
     *
     * @param newState
     */
    @Override
    public void onStateChanged(int newState) {
        if (newState == IPlayer.prepared) {
            WritableMap map = Arguments.createMap();
            map.putDouble("duration", mPlayer.getDuration());
            mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), map);
        }
    }

    /**
     * 调用aliPlayer.prepare()方法后，播放器开始读取并解析数据。成功后，会回调此接口。
     */
    @Override
    public void onPrepared() {

    }

    /**
     * 渲染开始通知
     */
    @Override
    public void onRenderingStart() {
        WritableMap map = Arguments.createMap();
        map.putDouble("duration", mPlayer.getDuration());
        mEventEmitter.receiveEvent(getId(), Events.EVENT_FIRST_RENDERED_START.toString(), map);
    }


    /**
     * 拖动完成
     */
    @Override
    public void onSeekComplete() {
        if (isSeek) {
            isSeek = false;
            WritableMap map2 = Arguments.createMap();
            map2.putDouble("currentTime", mSeekTime);
            mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), map2);

            WritableMap map = Arguments.createMap();
            map.putDouble("currentTime", mSeekTime);
            mEventEmitter.receiveEvent(getId(), Events.EVENT_SEEK.toString(), map);
        } else {
            WritableMap map = Arguments.createMap();
            map.putDouble("currentTime", 0);
            mEventEmitter.receiveEvent(getId(), Events.EVENT_SEEK.toString(), map);
        }
    }

    public void save(ReadableMap options, Promise promise) {
        mDownloaderPromise = promise;
        mDownloader = AliDownloaderFactory.create(mContext.getApplicationContext());
        mDownloader.setSaveDir(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .getAbsolutePath()
        );

        Map opts = options == null ? mSource : options.toHashMap();
        VidSts sts = getStsSource(opts.get("sts"));
        VidAuth auth = getAuthSource(opts.get("auth"));

        if (sts != null) {
            mDownloader.prepare(sts);
        } else if (auth != null) {
            mDownloader.prepare(auth);
        } else {
            return;
        }

        mDownloader.setOnCompletionListener(new AliMediaDownloader.OnCompletionListener() {
            @Override
            public void onCompletion() {
                if (mDownloaderPromise == null) {
                    return;
                }

                WritableMap map = Arguments.createMap();
                map.putString("uri", mDownloader.getFilePath());
                mDownloaderPromise.resolve(map);
                releaseDownloader();
            }
        });

        mDownloader.setOnPreparedListener(new AliMediaDownloader.OnPreparedListener() {
            @Override
            public void onPrepared(MediaInfo mediaInfo) {
                List<TrackInfo> trackInfos = mediaInfo.getTrackInfos();
                mDownloader.selectItem(trackInfos.get(0).getIndex());
                mDownloader.start();
            }
        });

        mDownloader.setOnErrorListener(new AliMediaDownloader.OnErrorListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                if (mDownloaderPromise != null) {
                    mDownloaderPromise.reject(new Exception(errorInfo.getMsg()));
                }

                releaseDownloader();
            }
        });
    }

    public void releaseDownloader() {
        if (mDownloader == null) {
            return;
        }

        mDownloader.stop();
        mDownloader.release();
    }

    public void destroy() {
        releaseDownloader();
        if (mLifecycleEventListener != null) {
            mContext.removeLifecycleEventListener(mLifecycleEventListener);
            mLifecycleEventListener = null;
        }
        if (mPlayer != null) {
            mPlayer.pause();
            mPlayer.setSurface(null);
            mPlayer.setDisplay(null);
            if (mPlayerType != 0) {
                mPlayer.clearScreen();
                mPlayer.release();
                mPlayer = null;
            }
        }
    }

    public static String getAliVideoCacheDir(Context context) {
        return getDiskCachePath(context.getApplicationContext()) + File.separator + "aliplayer/playCache" + File.separator;
    }

    /**
     * 获取 APP 的 cache 路径
     */
    public static String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            File exFile = context.getExternalCacheDir();
            if (exFile != null) {
                return exFile.getPath();
            }
        }
        return context.getCacheDir().getPath();
    }

    public void preLoadUrl(final String url) {
        Log.e("AAA", "preLoadUrl:" + url);
        MediaLoader mediaLoader = MediaLoader.getInstance();
        mediaLoader.setOnLoadStatusListener(new MediaLoader.OnLoadStatusListener() {
            @Override
            public void onError(String url, int code, String msg) {
                //加载出错
                Log.e("AAA", "onError preLoadUrl:" + url);
                Log.e("AAA", "onError code:" + code + "; msg:" + msg);
            }

            @Override
            public void onCompleted(String s) {
                //加载完成
                Log.e("AAA", "onCompleted preLoadUrl:" + url);
            }

            @Override
            public void onCanceled(String s) {
                //加载取消
                Log.e("AAA", "onCanceled preLoadUrl:" + url);
            }
        });
        mediaLoader.load(url, 10 * 1000);
    }

}
