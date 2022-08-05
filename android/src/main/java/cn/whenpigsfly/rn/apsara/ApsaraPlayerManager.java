package cn.whenpigsfly.rn.apsara;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.aliyun.player.AliPlayer;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import javax.annotation.Nullable;

import cn.whenpigsfly.rn.apsara.ApsaraPlayerView.Events;

public class ApsaraPlayerManager extends SimpleViewManager<ApsaraPlayerView> {

    public static final String REACT_CLASS = "ApsaraPlayer";

    public void onDropViewInstance(@NonNull ApsaraPlayerView view) {
        super.onDropViewInstance(view);
        AliPlayManager.getInstance().mPlayViewCount--;
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.destroy();
            }
        });
        AliPlayManager.getInstance().setViewDestroy(view.getId());
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public ApsaraPlayerView createViewInstance(ThemedReactContext c) {
        AliPlayManager.getInstance().mPlayViewCount++;
        ApsaraPlayerView view = new ApsaraPlayerView(c);
        return view;
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }


    @ReactProp(name = "initPlayer", defaultBoolean = true)
    public void setInitPlayer(final ApsaraPlayerView view, final boolean initPlayer) {
        final AliPlayer player;
        final AliPlayManager.AliPlayerInfo mPlayerInfo;
        mPlayerInfo = AliPlayManager.getInstance().getAliPlayer(view.getContext(), view.getId());
        player = mPlayerInfo.aliPlayer;
        view.init(player);
        TextureView textureView = new TextureView(view.getContext());
        view.addView(textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                player.setSurface(new Surface(surface));
                player.surfaceChanged();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                player.setSurface(new Surface(surface));
                player.surfaceChanged();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                player.setSurface(null);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }


    @ReactProp(name = "paused", defaultBoolean = true)
    public void setPaused(final ApsaraPlayerView view, final boolean paused) {
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setPaused(paused);
            }
        });
    }

    @ReactProp(name = "repeat", defaultBoolean = true)
    public void setRepeat(final ApsaraPlayerView view, final boolean repeat) {
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setRepeat(repeat);
            }
        });
    }

    @ReactProp(name = "muted", defaultBoolean = false)
    public void setMuted(final ApsaraPlayerView view, final boolean muted) {
        Log.e("AliPlayer", "setMuted：" + view.getId());
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setMuted(muted);
            }
        });
    }

    @ReactProp(name = "volume", defaultFloat = 1.0f)
    public void setVolume(final ApsaraPlayerView view, final float volume) {
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setVolume(volume);
            }
        });
    }

    @ReactProp(name = "seek", defaultFloat = 0.0f)
    public void setSeek(final ApsaraPlayerView view, final float seek) {
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setSeek((long) seek);
            }
        });
    }

    @ReactProp(name = "resizeMode")
    public void setResizeMode(final ApsaraPlayerView view, final String resizeMode) {
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setResizeMode(resizeMode);
            }
        });
    }

    @ReactProp(name = "cacheEnable", defaultBoolean = true)
    public void setCacheEnable(final ApsaraPlayerView view, final boolean cacheEnable) {
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setCacheEnable(cacheEnable);
            }
        });
    }

    @ReactProp(name = "positionTimerIntervalMs", defaultInt = 100)
    public void setPositionTimerIntervalMs(final ApsaraPlayerView view, final int positionTimerIntervalMs) {
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setPositionTimerIntervalMs(positionTimerIntervalMs);
            }
        });
    }

    @ReactProp(name = "videoBackgroundColor", customType = "#000000")
    public void setVideoBackgroundColor(final ApsaraPlayerView view, final String videoBackgroundColorString) {
        int colorInts = Color.parseColor(videoBackgroundColorString);
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setVideoBackgroundColor(colorInts);
            }
        });
    }

    @ReactProp(name = "source")
    public void setSource(final ApsaraPlayerView view, @Nullable ReadableMap source) {
        if (source == null) {
            return;
        }
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.setSource(source.toHashMap());
            }
        });
    }
}
