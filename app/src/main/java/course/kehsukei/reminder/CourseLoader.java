package course.kehsukei.reminder;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 从 assets/courses.json 加载课表 */
public class CourseLoader {
    public static List<Course> load(Context ctx) {
        List<Course> list = new ArrayList<>();
        try {
            InputStream is = ctx.getAssets().open("courses.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            JSONArray arr = new JSONArray(new String(buf, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Course(
                        o.getString("name"),
                        o.optString("teacher", ""),
                        o.optString("grade", ""),
                        o.optString("cls", ""),
                        o.getInt("weekday"),
                        o.getInt("period"),
                        o.getString("time"),
                        o.optString("slot", "上午")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
