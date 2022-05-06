package cn.whenpigsfly.rn.apsara;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.FrameLayout;

import com.aliyun.downloader.AliDownloaderFactory;
import com.aliyun.downloader.AliMediaDownloader;
import com.aliyun.loader.MediaLoader;
import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
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
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ApsaraPlayerView extends FrameLayout implements
        AliPlayer.OnInfoListener,
        AliPlayer.OnErrorListener,
        AliPlayer.OnCompletionListener,
        AliPlayer.OnPreparedListener,
        AliPlayer.OnSeekCompleteListener {


    public enum Events {
        EVENT_END("onVideoEnd"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_SEEK("onVideoSeek"),
        EVENT_ERROR("onVideoError"),
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
    private LifecycleEventListener mLifecycleEventListener;
    private boolean mPrepared = false;
    private boolean mRepeat;

    private boolean isPaused;

    public ApsaraPlayerView(ThemedReactContext context, AliPlayer player) {
        super(context);

        mContext = context;
        mPlayer = player;
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        init();
        initLifecycle();
    }

    public void init() {
        if (mPlayer != null) {
            return;
        }

        mPlayer = AliPlayerFactory.createAliPlayer(mContext);
        mPlayer.setTraceId("DisableAnalytics");
        mPlayer.setScaleMode(IPlayer.ScaleMode.SCALE_ASPECT_FIT);
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
        //其他设置
        //往前缓存的最大时长。单位ms。默认为0。
        config.mMaxBackwardBufferDurationMs = 500;
        //设置配置给播放器
        mPlayer.setConfig(config);


        //本地缓存
        CacheConfig cacheConfig = new CacheConfig();
        //开启缓存功能
        cacheConfig.mEnable = true;
        //能够缓存的单个文件最大时长。超过此长度则不缓存
        cacheConfig.mMaxDurationS = 300;
        //缓存目录的位置
        String dirPath = getDiskCachePath(mContext.getApplicationContext()) + File.separator + "aliplayer/video" + File.separator;
        cacheConfig.mDir = dirPath;
        //缓存目录的最大大小。超过此大小，将会删除最旧的缓存文件
        cacheConfig.mMaxSizeMB = 20 * 1024;
        //设置缓存配置给到播放器
        mPlayer.setCacheConfig(cacheConfig);
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
                if (!isPaused && mPlayer != null) {
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
            mPlayer.setDataSource(sts);
        } else if (auth != null) {
            mPlayer.setDataSource(auth);
        } else if (mSource.get("uri") != null && !String.valueOf(mSource.get("uri")).isEmpty()) {
            UrlSource source = new UrlSource();
            source.setUri((String) mSource.get("uri"));
            mPlayer.setDataSource(source);
        }

        if (!isPaused) {
            mPlayer.prepare();
        }

        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnSeekCompleteListener(this);

        if (mRepeat) {
            mPlayer.setAutoPlay(true);
        }

        mPrepared = true;
    }

    public void setPaused(final boolean paused) {
        if (mPlayer == null) {
            return;
        }
        if (paused) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        isPaused = paused;
    }

    public void setRepeat(final boolean repeat) {
        mRepeat = repeat;
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

    public void setSource(final Map source) {
        mSource = source;
        prepare();
    }

    public void setSeek(long position) {
        if (mPlayer != null) {
            mPlayer.seekTo(position);
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
        Log.e("AAA", "onCompletion:");
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), null);
    }


    /**
     * 播放器中的一些信息，包括：当前进度、缓存位置等等
     */
    @Override
    public void onInfo(InfoBean info) {
        Log.e("AAA", "onInfo:" + info.getExtraValue());
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
        Log.e("AAA", "onError");
        WritableMap map = Arguments.createMap();
        map.putInt("code", errorInfo.getCode().getValue());
        map.putString("message", errorInfo.getMsg());
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), map);
        if (mPlayer != null) {
            mPlayer.release();
        }
    }

    /**
     * 调用aliPlayer.prepare()方法后，播放器开始读取并解析数据。成功后，会回调此接口。
     */
    @Override
    public void onPrepared() {
        Log.e("AAA", "onPrepared");
        WritableMap map = Arguments.createMap();
        map.putDouble("duration", mPlayer.getDuration());
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), map);
    }

    /**
     * 拖动完成
     */
    @Override
    public void onSeekComplete() {
        Log.e("AAA", "onSeekComplete");
        WritableMap map = Arguments.createMap();
        map.putDouble("currentTime", mPlayer.getDuration());
        mEventEmitter.receiveEvent(getId(), Events.EVENT_SEEK.toString(), null);
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
            mPlayer.clearScreen();
            mPlayer.release();
            mPlayer = null;
        }
    }


    /**
     * 获取 APP 的 cache 路径
     *
     * @param context
     * @return /storage/emulated/0/Android/data/包名/cache
     */
    public static String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            // /storage/emulated/0/Android/data/packageName/cache   不会在空间少时被自动清除
            File exFile = context.getExternalCacheDir();
            if (exFile != null) {
                return exFile.getPath();
            }
            return context.getCacheDir().getPath();
        } else {
            // /data/data/<应用包名>/cache   用来存储临时数据。因此在系统空间较少时有可能会被自动清除。
            return context.getCacheDir().getPath();
        }
    }
}
