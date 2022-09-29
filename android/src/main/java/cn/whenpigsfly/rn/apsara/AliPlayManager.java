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

        //缓存池中的下标
        public int playerPosition;

        //当前播放器复用次数
        public int reuseCount;

        public int viewId = -1;

        //是否被View展示中
        public boolean isShowing = false;
    }

    private final ArrayList<AliPlayerInfo> mPlayHomeList = new ArrayList<>();

    /**
     * 记录每个View创建的 viewId,及对应的播放池下标
     */
    private final Map<Integer, Integer> mViewMap = new HashMap<>();

    public void setViewDestroy(int viewId) {
        Integer playListPosition = mViewMap.get(viewId);
        if (playListPosition != null) {
            //把播放池中的播放器实例和View解绑
            if (!mPlayHomeList.isEmpty() && mPlayHomeList.size() > playListPosition) {
                AliPlayerInfo info = mPlayHomeList.get(playListPosition);
                if (info != null && info.viewId == viewId) {
                    //缓存池过大
                    if (mPlayHomeList.size() > mMaxPlayerCount) {
                        mPlayHomeList.remove((int) playListPosition);
                    } else {
                        info.viewId = -1;
                        info.isShowing = false;
                    }
                    mViewMap.remove(viewId);
                }
            }
        }
//        Log.e("AliPlayer", "setViewDestroy 缓存池大小：" + mPlayHomeList.size() + "；View数量：" + mPlayViewCount);
    }

    public AliPlayerInfo getAliPlayer(Context context, int viewId) {
        for (AliPlayerInfo info : mPlayHomeList) {
            //没有显示中的View
            if (!info.isShowing) {
                if (info.reuseCount < mMaxReuseCount) {
                    //直接复用
                    info.isShowing = true;
                    info.viewId = viewId;
                    info.reuseCount++;
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
//                Log.e("AliPlayer", "缓存池大小：" + mPlayHomeList.size() + "；缓存池位置：" + info.playerPosition + "；复用次数：" + info.reuseCount + "；View数量：" + mPlayViewCount);
                mViewMap.put(viewId, info.playerPosition);
                return info;
            }
        }
        AliPlayerInfo info = new AliPlayerInfo();
        AliPlayer player = AliPlayerFactory.createAliPlayer(context.getApplicationContext());
        info.aliPlayer = player;
        info.isShowing = true;
        info.viewId = viewId;
        info.reuseCount = 1;
        info.playerPosition = mPlayHomeList.size();
        mPlayHomeList.add(info);
        mViewMap.put(viewId, info.playerPosition);

//        Log.e("AliPlayer", "缓存池大小：" + mPlayHomeList.size() + "；缓存池位置：" + info.playerPosition + "；复用次数：" + info.reuseCount + "；View数量：" + mPlayViewCount);
        return info;
    }

}
