# 课程提醒小助手（原生安卓）

马俊强老师八年级 81/82 班数学课表，系统级闹钟提醒，**关掉 APP、锁屏、休眠都会响**。

## 功能
- 系统闹钟 `AlarmManager.setExactAndAllowWhileIdle`：休眠/后台清理/锁屏都能唤醒
- 锁屏盖屏全屏弹窗 + 循环响铃，直到点「我知道了」才停
- 铃声用 Alarm 音频流，**无视静音**
- 触发后自动注册下一周同日，实现每周自动重复
- 开机、时间变更后自动重建闹钟
- 可设置提前提醒分钟数（默认 10 分钟）
- 课表数据在 `app/src/main/assets/courses.json`，改课表重新上传即可

## 上传到 GitHub 后自动编译 APK
1. 新建仓库（Public，不用勾选 README）
2. 把本目录所有文件**原样拖拽**上传到仓库根目录
3. 进 Actions 标签，等 3~8 分钟变绿
4. 进绿色记录，底部 Artifacts 下载 `app-debug-apk`，解压得到 APK

> ⚠️ 不要新建子文件夹，所有文件必须直接放在仓库根目录；`.github` 是隐藏文件夹，网页拖拽会上传但请确保它存在。

## 星期约定
`courses.json` 里 `weekday`：1=周一，2=周二 …… 6=周五，7=周日。
`time` 是上课时间，提醒时刻 = 上课时间 − 提前分钟数。

## 修改课表
编辑 `app/src/main/assets/courses.json`，每条一课，字段：`name / teacher / grade / cls / weekday / period / time / slot`。
改完 push，Actions 自动重编。
