package course.kehsukei.reminder;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * 循环铃声播放器：用 Alarm 音频流反复播放系统闹钟音，
 * 每次响完 → 停 0.25 秒 → 再响，直到调用 stop() 才停下。
 *
 * Alarm 流会无视静音与大多数勿扰设置，保证"必须被听到"。
 */
public class RingtonePlayer {

    private static final long SILENT_BETWEEN_MS = 250;

    private static volatile RingtonePlayer sInstance;

    private final Context appContext;
    private MediaPlayer current;
    private boolean playing;
    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    public static synchronized RingtonePlayer get(Context ctx) {
        if (sInstance == null) {
            sInstance = new RingtonePlayer(ctx.getApplicationContext());
        }
        return sInstance;
    }

    private RingtonePlayer(Context appContext) {
        this.appContext = appContext;
    }

    /** 开始循环播放，已在播放则先停再重启 */
    public synchronized void play() {
        stop();
        playing = true;
        startOne();
    }

    /** 立即停止（点"我知道了"时调用） */
    public synchronized void stop() {
        playing = false;
        handler.removeCallbacks(restartRunnable);
        if (current != null) {
            try {
                if (current.isPlaying()) current.stop();
                current.reset();
                current.release();
            } catch (Exception ignored) {}
            current = null;
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    /** 播放一段，结束后再排一次自己，形成循环 */
    private void startOne() {
        if (!playing) return;
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            final MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(appContext, uri);
            mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            mp.setLooping(false);
            mp.setOnCompletionListener(mp1 -> {
                try { mp1.release(); } catch (Exception ignored) {}
                if (playing) {
                    handler.postDelayed(restartRunnable, SILENT_BETWEEN_MS);
                }
            });
            mp.setOnErrorListener((mp1, what, extra) -> {
                try { mp1.release(); } catch (Exception ignored) {}
                if (playing) {
                    handler.postDelayed(restartRunnable, SILENT_BETWEEN_MS);
                }
                return true;
            });
            mp.prepare();
            mp.start();
            current = mp;
        } catch (Exception e) {
            e.printStackTrace();
            if (playing) {
                handler.postDelayed(restartRunnable, SILENT_BETWEEN_MS);
            }
        }
    }

    private final Runnable restartRunnable = new Runnable() {
        @Override
        public void run() {
            startOne();
        }
    };
}
