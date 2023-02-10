package cn.whenpigsfly.rn.apsara;

import android.content.Context;
import android.util.Log;

import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AliPlayManager {

    private AliPlayManager() {
    }

    private static class Instance {
        private static final AliPlayManager instance = new AliPlayManager();
    }

    public static AliPlayManager getInstance() {
        return Instance.instance;
    }
    /**
     * 播放器中 View 的数量
     */
    public int mPlayViewCount = 0;

    /**
     * 单个播放器最大复用次数
     */
    private static final int mMaxReuseCount = 5;

    /**
     * 播放器缓存池最大数量
     */
    private static final int mMaxPlayerCount = 5;

    public static class AliPlayerInfo {
        //播放器实例
        public AliPlayer aliPlayer;

        //当前播放器复用次数
        public int reuseCount;

        public int viewId = -1;

        //是否被View展示中
        public boolean isShowing = false;
    }

    /**
     * 记录每个View创建的 viewId,及对应的播放池下标
     */
    private final Map<Integer, AliPlayerInfo> mViewMap = new HashMap<>();

    public void setViewDestroy(int viewId) {
        AliPlayerInfo info = mViewMap.get(viewId);
            //把播放池中的播放器实例和View解绑
        if (info != null) {
            //缓存池过大
            if (mViewMap.size() > mMaxPlayerCount) {
                if (info.aliPlayer != null) {
                    info.aliPlayer.stop();
                    info.aliPlayer.release();
                    info.aliPlayer = null;
                }
                mViewMap.remove(viewId);
            } else {
                if (info.aliPlayer != null) {
                    info.aliPlayer.stop();
                    info.aliPlayer.clearScreen();
                }
                info.viewId = -1;
                info.isShowing = false;
            }
        }
    }

    public AliPlayerInfo getAliPlayer(Context context, int viewId) {
        for (Map.Entry<Integer, AliPlayerInfo> entry : mViewMap.entrySet()) {
            AliPlayerInfo info = entry.getValue();
            if (!info.isShowing) {
                if (info.reuseCount < mMaxReuseCount) {
                    //直接复用
                    info.isShowing = true;
                    info.viewId = viewId;
                    info.reuseCount++;
                    info.aliPlayer.clearScreen();
                    info.aliPlayer.pause();
                } else {
                    info.aliPlayer.setSurface(null);
                    info.aliPlayer.setDisplay(null);
                    info.aliPlayer.clearScreen();
                    info.aliPlayer.release();
                    AliPlayer player = AliPlayerFactory.createAliPlayer(context.getApplicationContext());
                    info.aliPlayer = player;
                    info.isShowing = true;
                    info.viewId = viewId;
                    info.reuseCount = 1;
                }
                return info;
            }
        }
        AliPlayerInfo info = new AliPlayerInfo();
        AliPlayer player = AliPlayerFactory.createAliPlayer(context.getApplicationContext());
        info.aliPlayer = player;
        info.isShowing = true;
        info.viewId = viewId;
        info.reuseCount = 1;
        mViewMap.put(viewId, info);

        return info;
    }

}
