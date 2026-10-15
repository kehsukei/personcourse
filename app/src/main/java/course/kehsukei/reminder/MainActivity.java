package course.kehsukei.reminder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 主界面：
 * - 实时时钟 + 下一节课倒计时
 * - 按星期分组展示本周课表（今天高亮）
 * - 设置提前提醒分钟数
 * - 启动时向系统注册整周闹钟
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 1001;

    private TextView tvClock, tvNext, tvSummary;
    private LinearLayout container;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private List<Course> courses;
    private int advanceMin = AlarmScheduler.DEFAULT_ADVANCE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvClock = findViewById(R.id.tvClock);
        tvNext = findViewById(R.id.tvNext);
        tvSummary = findViewById(R.id.tvSummary);
        container = findViewById(R.id.container);
        Spinner spinner = findViewById(R.id.spinnerAdvance);
        Button btnApply = findViewById(R.id.btnApply);

        courses = CourseLoader.load(this);
        advanceMin = AlarmScheduler.getAdvanceMinutes(this);

        String[] opts = {"1 分钟", "2 分钟", "3 分钟", "4 分钟", "5 分钟", "10 分钟"};
        final int[] vals = {1, 2, 3, 4, 5, 10};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opts));
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == advanceMin) spinner.setSelection(i);
        }
        btnApply.setOnClickListener(v -> {
            int idx = spinner.getSelectedItemPosition();
            AlarmScheduler.setAdvanceMinutes(this, vals[idx]);
            AlarmScheduler.cancelAll(this);
            AlarmScheduler.scheduleAll(this);
            advanceMin = vals[idx];
            Toast.makeText(this, "已保存，全部提醒已重新注册", Toast.LENGTH_SHORT).show();
        });

        // Android 13+ 申请通知权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
            }
        }

        // Android 12+ 精确闹钟需用户授权：跳转设置页
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.AlarmManager am = ContextCompat.getSystemService(this, android.app.AlarmManager.class);
            if (am != null && !am.canScheduleExactAlarms()) {
                Toast.makeText(this, "请在系统设置中允许本应用使用「精确闹钟」", Toast.LENGTH_LONG).show();
                try {
                    startActivity(new android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            android.net.Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {}
            }
        }

        // 注册整周闹钟
        AlarmScheduler.scheduleAll(this);

        renderSummary();
        renderTable();
        tickClock();
        handler.postDelayed(tickRunnable, 1000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tickRunnable);
        super.onDestroy();
    }

    private final Runnable tickRunnable = new Runnable() {
        @Override public void run() {
            tickClock();
            handler.postDelayed(this, 1000);
        }
    };

    private void tickClock() {
        Calendar now = Calendar.getInstance();
        tvClock.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND)));
        Course next = findNextCourse(now);
        if (next != null) {
            Calendar t = nextTriggerOf(next, advanceMin);
            long diff = t.getTimeInMillis() - now.getTimeInMillis();
            long min = diff / 60000;
            long sec = (diff % 60000) / 1000;
            String slotName = slotLabel(next);
            tvNext.setText(Html.fromHtml(
                    "下一节：<b>" + next.name + "</b>（" + slotName + next.period + "）" + next.grade + next.cls
                            + "，还有 <b>" + min + "分" + sec + "秒</b>"));
        } else {
            tvNext.setText("本周课程已全部结束，下周一开始 🎉");
        }
    }

    private void renderSummary() {
        int c81 = 0, c82 = 0;
        for (Course c : courses) {
            if ("81班".equals(c.cls)) c81++;
            else if ("82班".equals(c.cls)) c82++;
        }
        tvSummary.setText(Html.fromHtml(
                "<b>" + courses.size() + "</b> 节课 / 每周　|　"
                        + "81班 <b>" + c81 + "</b> 节　|　82班 <b>" + c82 + "</b> 节"));
    }

    private void renderTable() {
        container.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        String[] wdNames = {"", "周一", "周二", "周三", "周四", "周五", "", "周日"};

        for (int wd = 1; wd <= 7; wd++) {
            List<Course> day = new ArrayList<>();
            for (Course c : courses) if (c.weekday == wd) day.add(c);
            if (day.isEmpty()) continue;

            View card = inf.inflate(R.layout.item_day_card, container, false);
            TextView tvDay = card.findViewById(R.id.tvDay);
            TextView tvDayCount = card.findViewById(R.id.tvDayCount);
            LinearLayout list = card.findViewById(R.id.listCourses);

            int calWd = AlarmScheduler.toCalendarWd(wd);
            int todayWd = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            tvDay.setText(wdNames[wd]);
            if (calWd == todayWd) {
                tvDay.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                tvDay.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_dot_today, 0);
            }
            tvDayCount.setText(day.size() + " 节");

            for (Course c : day) {
                View row = inf.inflate(R.layout.item_course_row, list, false);
                TextView tvSlot = row.findViewById(R.id.tvSlot);
                TextView tvName = row.findViewById(R.id.tvName);
                TextView tvTime = row.findViewById(R.id.tvTime);
                TextView tvClass = row.findViewById(R.id.tvClass);
                String slot = slotLabel(c);
                tvSlot.setText(slot);
                tvSlot.setTextColor(slotColor(slot));
                tvName.setText(c.name);
                tvTime.setText(c.time);
                tvClass.setText(c.grade + c.cls);
                list.addView(row);
            }
            container.addView(card);
        }
    }

    private String slotLabel(Course c) {
        if ("上午".equals(c.slot)) return "上午";
        if ("下午".equals(c.slot)) return "下午";
        return "晚自习";
    }

    private int slotColor(String slot) {
        if ("上午".equals(slot)) return ContextCompat.getColor(this, R.color.slot_morning);
        if ("下午".equals(slot)) return ContextCompat.getColor(this, R.color.slot_afternoon);
        return ContextCompat.getColor(this, R.color.slot_night);
    }

    /** 计算某节课「最近一次」触发时刻，仅用于界面倒计时展示 */
    private Calendar nextTriggerOf(Course c, int advanceMin) {
        Calendar now = Calendar.getInstance();
        int todayWd = now.get(Calendar.DAY_OF_WEEK);
        int targetWd = AlarmScheduler.toCalendarWd(c.weekday);
        int diff = targetWd - todayWd;
        if (diff < 0) diff += 7;
        if (diff == 0) {
            Calendar tryToday = buildTriggerAt(now, c, advanceMin);
            if (!tryToday.after(now)) diff = 7;
        }
        Calendar base = (Calendar) now.clone();
        base.add(Calendar.DAY_OF_MONTH, diff);
        return buildTriggerAt(base, c, advanceMin);
    }

    private Calendar buildTriggerAt(Calendar base, Course c, int advanceMin) {
        Calendar x = (Calendar) base.clone();
        x.set(Calendar.HOUR_OF_DAY, c.getHour());
        x.set(Calendar.MINUTE, c.getMinute());
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);
        x.add(Calendar.MINUTE, -advanceMin);
        return x;
    }

    private Course findNextCourse(Calendar now) {
        Course best = null;
        long bestDiff = Long.MAX_VALUE;
        for (Course c : courses) {
            Calendar t = nextTriggerOf(c, advanceMin);
            long diff = t.getTimeInMillis() - now.getTimeInMillis();
            if (diff > 0 && diff < bestDiff) {
                bestDiff = diff;
                best = c;
            }
        }
        return best;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
