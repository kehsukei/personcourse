package course.kehsukei.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * 闹钟触发入口：被 AlarmManager 在精确的提醒时刻唤醒（即使 APP 关闭、锁屏、休眠）。
 *
 * 职责：
 *   1. 启动全屏弹窗 Activity（盖在锁屏之上）
 *   2. 启动循环铃声服务（直到点"我知道了"才停）
 *   3. 为本节课注册下一周的同日提醒，实现"每周自动重复"
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 取出发送时塞进去的字段（兼容旧方式）
        String name = intent.getStringExtra("name");
        String cls  = intent.getStringExtra("cls");
        String time = intent.getStringExtra("time");
        String slot = intent.getStringExtra("slot");
        String id   = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_ID);

        if (name == null) {
            // 理论上不该发生：没有课程信息就不弹
            return;
        }

        // 1) 启动循环铃声
        RingtonePlayer.get(context).play();

        // 2) 全屏弹窗
        Intent alarm = new Intent(context, AlarmActivity.class);
        alarm.putExtra("name", name);
        alarm.putExtra("cls", cls);
        alarm.putExtra("time", time);
        alarm.putExtra("slot", slot);
        alarm.putExtra(AlarmScheduler.EXTRA_COURSE_ID, id);
        alarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        // 从 BroadcastReceiver 启动 Activity 必须 NEW_TASK
        try {
            context.startActivity(alarm);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3) 顺延注册下一周的同日提醒（offset = 7 天）
        Course course = findCourse(context, id);
        if (course != null) {
            int daysUntil = daysUntilNextWeekday(course.weekday);
            AlarmScheduler.scheduleNextWeek(context, course, daysUntil);
        }
    }

    /** 计算「下一个匹配 weekday」距离今天的天数（1~7） */
    private int daysUntilNextWeekday(int weekday) {
        int target = AlarmScheduler.toCalendarWd(weekday);
        int today  = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        int diff = target - today;
        if (diff <= 0) diff += 7;     // 确保是"下一周"而不是本周已过的日子
        return diff;
    }

    /** 用 id 从课表里找回 Course 对象 */
    private Course findCourse(Context ctx, String id) {
        if (id == null) return null;
        for (Course c : CourseLoader.load(ctx)) {
            if (AlarmScheduler.courseId(c).equals(id)) return c;
        }
        return null;
    }
}
