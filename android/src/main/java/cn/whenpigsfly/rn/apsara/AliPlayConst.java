package cn.whenpigsfly.rn.apsara;

import android.content.Context;
import android.util.Log;

import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;

import java.util.HashMap;
import java.util.Map;

public class AliPlayConst {

    /**
     * 播放池播放器实例数量
     */
    public static final int PLAYER_COUNT = 5;

    /**
     * 单个播放器最大复用次数
     */
    public static final int MAX_REUSE_COUNT = 5;

    /**
     * 播放器池
     */
    public static final AliPlayer[] ALI_PLAYER_ARRAY = new AliPlayer[PLAYER_COUNT];

    /**
     * 记录播放器复用次数
     */
    public static final Map<Integer, Integer> ALI_PLAYER_MAP = new HashMap();

    /**
     * 播放次数
     */
    public static int PLAYER_POSITION = 0;

    public static AliPlayer getAliPlayer(Context context) {
        AliPlayer staticPlayer = AliPlayConst.ALI_PLAYER_ARRAY[(AliPlayConst.PLAYER_POSITION % AliPlayConst.PLAYER_COUNT)];
        //0~4
        int countPosition = AliPlayConst.PLAYER_POSITION % AliPlayConst.PLAYER_COUNT;

        if (staticPlayer == null) {
            staticPlayer = AliPlayerFactory.createAliPlayer(context);
            AliPlayConst.ALI_PLAYER_ARRAY[countPosition] = staticPlayer;
            AliPlayConst.ALI_PLAYER_MAP.put(countPosition, 1);
        } else {
            Integer reuseCount = AliPlayConst.ALI_PLAYER_MAP.get(countPosition);
            if (reuseCount != null) {
                //复用超过50次,释放并且重新创建播放器
                if (reuseCount >= MAX_REUSE_COUNT) {
                    staticPlayer.clearScreen();
                    staticPlayer.release();
                    staticPlayer = AliPlayerFactory.createAliPlayer(context);
                    AliPlayConst.ALI_PLAYER_ARRAY[countPosition] = staticPlayer;
                    AliPlayConst.ALI_PLAYER_MAP.put(countPosition, 1);
                } else {
                    //复用次数+1
                    AliPlayConst.ALI_PLAYER_MAP.put(countPosition, reuseCount + 1);
                }
            }
        }
        Log.e("AliPlayer", "播放器位置：" + countPosition + "；复用次数：" + AliPlayConst.ALI_PLAYER_MAP.get(countPosition));
        AliPlayConst.PLAYER_POSITION++;
        return staticPlayer;
    }

}
