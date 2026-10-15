package course.kehsukei.reminder;

/** 单节课的数据模型 */
public class Course {
    public final String name;      // 课程名，如"数学"
    public final String teacher;   // 老师，如"马俊强"
    public final String grade;     // 年级，如"八年级"
    public final String cls;       // 班级，如"81班"
    public final int weekday;      // 星期（1=周一 ... 7=周日）
    public final int period;      // 节次（第几节课）
    public final String time;      // 上课时间，如"09:25"
    public final String slot;      // 时段：上午/下午/灯下

    public Course(String name, String teacher, String grade, String cls,
                  int weekday, int period, String time, String slot) {
        this.name = name;
        this.teacher = teacher;
        this.grade = grade;
        this.cls = cls;
        this.weekday = weekday;
        this.period = period;
        this.time = time;
        this.slot = slot;
    }

    /** 显示用标题，如"数学（上午3）" */
    public String getTitle() {
        String slotName;
        switch (slot) {
            case "上午": slotName = "上午"; break;
            case "下午": slotName = "下午"; break;
            case "灯下": slotName = "晚自习"; break;
            default: slotName = "";
        }
        return name + "（" + slotName + period + "）";
    }

    /** 显示用副标题，如"八年级81班 · 授课老师：马俊强" */
    public String getSubtitle() {
        return grade + cls + "  ·  授课老师：" + teacher;
    }

    public int getHour() { return Integer.parseInt(time.split(":")[0]); }
    public int getMinute() { return Integer.parseInt(time.split(":")[1]); }
}
