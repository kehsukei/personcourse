package course.kehsukei.reminder;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 提醒到达时的全屏弹窗：
 *   - 点亮屏幕、盖在锁屏之上、禁止被系统自动销毁
 *   - 循环响铃，直到点"我知道了"才停止
 */
public class AlarmActivity extends AppCompatActivity {

    private RingtonePlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 锁屏/休眠时也能显示
        Window w = getWindow();
        w.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_alarm);

        String name = getIntent().getStringExtra("name");
        String cls  = getIntent().getStringExtra("cls");
        String time = getIntent().getStringExtra("time");
        String slot = getIntent().getStringExtra("slot");

        ((TextView) findViewById(R.id.tvTitle)).setText(name != null ? name : "上课提醒");
        ((TextView) findViewById(R.id.tvClass)).setText(cls != null ? cls : "");
        ((TextView) findViewById(R.id.tvDetail)).setText(
                (slot != null ? slot : "") + " · " + (time != null ? time : "") + " 上课");

        player = RingtonePlayer.get(this);
        player.play();

        findViewById(R.id.btnDismiss).setOnClickListener(v -> {
            if (player != null) player.stop();
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键关闭，必须点"我知道了"
    }

    @Override
    protected void onDestroy() {
        if (player != null) player.stop();
        super.onDestroy();
    }
}
