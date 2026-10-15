package course.kehsukei.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 开机 / 时间被修改后重建整周闹钟。
 * 闹钟在关机期间无法保存，所以重启必须重新注册。
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmScheduler.scheduleAll(context);
    }
}
