package cn.whenpigsfly.rn.apsara;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
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

    private SurfaceView mSurfaceView;

    public void onDropViewInstance(@NonNull ApsaraPlayerView view) {
        super.onDropViewInstance(view);
        view.getThemedReactContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                view.destroy();
            }
        });
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public ApsaraPlayerView createViewInstance(ThemedReactContext c) {
        final AliPlayer player = AliPlayerFactory.createAliPlayer(c);

        ApsaraPlayerView playerView = new ApsaraPlayerView(c, player);
//        mSurfaceView = new SurfaceView(c);
//        playerView.addView(mSurfaceView);
//        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                player.setSurface(holder.getSurface());
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                player.surfaceChanged();
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                player.setSurface(null);
//            }
//        });

        TextureView textureView = new TextureView(c);
        playerView.addView(textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                player.setSurface(new Surface(surface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                player.surfaceChanged();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                player.setSurface(null);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        return playerView;
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

    @ReactProp(name = "paused", defaultBoolean = true)
    public void setPaused(final ApsaraPlayerView view, final boolean paused) {
        view.setPaused(paused);
    }

    @ReactProp(name = "repeat", defaultBoolean = true)
    public void setRepeat(final ApsaraPlayerView view, final boolean repeat) {
        view.setRepeat(repeat);
    }

    @ReactProp(name = "muted", defaultBoolean = false)
    public void setMuted(final ApsaraPlayerView view, final boolean muted) {
        view.setMuted(muted);
    }

    @ReactProp(name = "volume", defaultFloat = 1.0f)
    public void setVolume(final ApsaraPlayerView view, final float volume) {
        view.setVolume(volume);
    }

    @ReactProp(name = "seek", defaultFloat = 0.0f)
    public void setSeek(final ApsaraPlayerView view, final float seek) {
        view.setSeek((long) seek);
    }

    @ReactProp(name = "cacheEnable", defaultBoolean = true)
    public void setCacheEnable(final ApsaraPlayerView view, final boolean cacheEnable) {
        view.setCacheEnable(cacheEnable);
    }

    @ReactProp(name = "source")
    public void setSource(final ApsaraPlayerView view, @Nullable ReadableMap source) {
        if (source == null) {
            return;
        }

        view.setSource(source.toHashMap());
    }
}
