package course.kehsukei.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;

/**
 * 闹钟核心：把每节课注册到系统 AlarmManager。
 *
 * 保证「关掉 APP 也能后台提醒」的关键：
 *   - setExactAndAllowWhileIdle：休眠 / 后台清理 / 锁屏 都会唤醒
 *   - 按 weekday 批量注册本周七天，而不是只注册「下一次」（否则一周只响一次）
 *   - 每节课触发时会重新注册下一周，实现无限循环
 *
 * 星期约定：Course.weekday 1=周一 ... 7=周日
 *          Calendar.DAY_OF_WEEK 1=周日 ... 7=周六
 *          转换：weekday==7 -> Calendar.SUNDAY(1)；否则 -> weekday+1
 */
public class AlarmScheduler {

    public static final String EXTRA_COURSE_ID = "course_id";
    private static final String PREFS = "reminder_prefs";
    private static final String KEY_ADVANCE = "advance_minutes";
    public static final int DEFAULT_ADVANCE = 10;

    public static int getAdvanceMinutes(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_ADVANCE, DEFAULT_ADVANCE);
    }

    public static void setAdvanceMinutes(Context ctx, int minutes) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_ADVANCE, minutes).apply();
    }

    /**
     * 注册所有课程：对每节课计算本周内所有匹配的 weekday，全部 setExact。
     * 已过去的触发点会被跳过，只保留「今天剩余 + 本周未来」的提醒。
     */
    public static void scheduleAll(Context ctx) {
        AlarmManager am = ContextCompat.getSystemService(ctx, AlarmManager.class);
        if (am == null) return;

        List<Course> courses = CourseLoader.load(ctx);
        int advance = getAdvanceMinutes(ctx);
        Calendar now = Calendar.getInstance();

        for (Course c : courses) {
            for (int offset = 0; offset < 7; offset++) {
                Calendar thatDay = (Calendar) now.clone();
                thatDay.add(Calendar.DAY_OF_MONTH, offset);
                if (thatDay.get(Calendar.DAY_OF_WEEK) != toCalendarWd(c.weekday)) continue;

                Calendar trigger = buildTrigger(thatDay, c, advance);
                if (!trigger.after(now)) continue; // 已过，跳过

                Intent intent = new Intent(ctx, AlarmReceiver.class);
                intent.putExtra(EXTRA_COURSE_ID, courseId(c));
                intent.putExtra("name", c.name);
                intent.putExtra("cls", c.grade + c.cls);
                intent.putExtra("time", c.time);
                intent.putExtra("slot", c.slot);

                PendingIntent pi = PendingIntent.getBroadcast(
                        ctx,
                        requestCode(c, offset),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                setExactAlarm(am, trigger.getTimeInMillis(), pi);
            }
        }
    }

    /**
     * 只注册某一节课在「下一周 offset 那天」的提醒（用于本次触发后顺延）
     */
    static void scheduleNextWeek(Context ctx, Course c, int offsetFromToday) {
        AlarmManager am = ContextCompat.getSystemService(ctx, AlarmManager.class);
        if (am == null) return;
        int advance = getAdvanceMinutes(ctx);
        Calendar thatDay = (Calendar) Calendar.getInstance().clone();
        thatDay.add(Calendar.DAY_OF_MONTH, offsetFromToday);

        Calendar trigger = buildTrigger(thatDay, c, advance);
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.putExtra(EXTRA_COURSE_ID, courseId(c));
        intent.putExtra("name", c.name);
        intent.putExtra("cls", c.grade + c.cls);
        intent.putExtra("time", c.time);
        intent.putExtra("slot", c.slot);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, requestCode(c, offsetFromToday), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        setExactAlarm(am, trigger.getTimeInMillis(), pi);
    }

    /** 取消全部：按同样的 requestCode 规则反查并 cancel */
    public static void cancelAll(Context ctx) {
        AlarmManager am = ContextCompat.getSystemService(ctx, AlarmManager.class);
        if (am == null) return;
        List<Course> courses = CourseLoader.load(ctx);
        for (Course c : courses) {
            for (int offset = 0; offset < 7; offset++) {
                Intent intent = new Intent(ctx, AlarmReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(
                        ctx, requestCode(c, offset), intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.cancel(pi);
            }
        }
    }

    /** 把周一(1)~周日(7) 转成 Calendar.DAY_OF_WEEK 常量 */
    static int toCalendarWd(int weekday) {
        if (weekday == 7) return Calendar.SUNDAY;   // 7 -> 周日=1
        return weekday + 1;                         // 1->2(周一) ... 6->7(周六)
    }

    /** 保证唯一且稳定的 requestCode，避免不同课程互相覆盖 */
    static int requestCode(Course c, int offset) {
        return ((courseId(c).hashCode() & 0x7fffffff) % 100000) * 7 + (offset % 7);
    }

    private static Calendar buildTrigger(Calendar base, Course course, int advanceMin) {
        Calendar c = (Calendar) base.clone();
        c.set(Calendar.HOUR_OF_DAY, course.getHour());
        c.set(Calendar.MINUTE, course.getMinute());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MINUTE, -advanceMin);
        return c;
    }

    private static void setExactAlarm(AlarmManager am, long time, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, time, pi);
        }
    }

    static String courseId(Course c) {
        return c.grade + c.cls + "_wd" + c.weekday + "_p" + c.period;
    }
}
