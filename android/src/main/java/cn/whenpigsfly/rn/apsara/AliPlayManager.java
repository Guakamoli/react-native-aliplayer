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
        private static AliPlayManager instance = new AliPlayManager();
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
    private static final int mMaxReuseCount = 50;

    public static class AliPlayerInfo {
        //播放器实例
        public AliPlayer aliPlayer;

        //缓存池中的下标
        public int playerPosition;

        //当前播放器复用次数
        public int reuseCount;

        //是否被View展示中
        public boolean isShowing = false;
    }

    private ArrayList<AliPlayerInfo> mPlayHomeList = new ArrayList<>();


    public AliPlayerInfo getAliPlayer(Context context) {
        for (AliPlayerInfo info : mPlayHomeList) {
            //没有显示中的View
            if (!info.isShowing) {
                if (info.reuseCount < mMaxReuseCount) {
                    //直接复用
                    info.isShowing = true;
                    info.reuseCount++;
                } else {
                    info.aliPlayer.setSurface(null);
                    info.aliPlayer.setDisplay(null);
                    info.aliPlayer.clearScreen();
                    info.aliPlayer.release();
                    AliPlayer player = AliPlayerFactory.createAliPlayer(context.getApplicationContext());
                    info.aliPlayer = player;
                    info.isShowing = true;
                    info.reuseCount = 1;
                }
                Log.e("AliPlayer", "缓存池大小：" + mPlayHomeList.size() + "；缓存池位置：" + info.playerPosition + "；复用次数：" + info.reuseCount + "；View数量：" + mPlayViewCount);
                return info;
            }
        }

        AliPlayerInfo info = new AliPlayerInfo();
        AliPlayer player = AliPlayerFactory.createAliPlayer(context.getApplicationContext());
        info.aliPlayer = player;
        info.isShowing = true;
        info.reuseCount = 1;
        info.playerPosition = mPlayHomeList.size();
        mPlayHomeList.add(info);
//        mAliPlayerInfo = info;
        Log.e("AliPlayer", "缓存池大小：" + mPlayHomeList.size() + "；缓存池位置：" + info.playerPosition + "；复用次数：" + info.reuseCount + "；View数量：" + mPlayViewCount);
        return info;
    }

}
