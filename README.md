# 今日任务小组件 App

一个安卓桌面小组件应用：前一天在 App 里配置好当天的时间段任务，桌面小组件就会实时显示「当前时间段该做什么」，时间过了自动跳到下一个时段，全程不用点进软件。

## 功能特性

- ✅ 桌面小组件实时显示当前时段任务
- ✅ 时间到了自动切换到下一个时段（每分钟自动刷新）
- ✅ 空闲时段显示「接下来要做什么」
- ✅ 当天任务完成后显示「可以休息啦」
- ✅ 纯本地存储，不联网、不注册、隐私安全

## 如何编译安装

### 方式一：用 Android Studio（推荐）

1. 安装 [Android Studio](https://developer.android.com/studio)（自带 JDK 和 SDK）
2. 打开 Android Studio → `File` → `Open` → 选择本项目文件夹
3. 等待 Gradle 同步完成（第一次会自动下载依赖，需要联网）
4. 手机开启「开发者选项」→「USB 调试」，用数据线连接电脑
5. 点击顶部绿色运行按钮 ▶，选择你的手机，即可安装

### 方式二：命令行编译

在项目根目录执行（需已安装 JDK 17 + Android SDK）：

```bash
./gradlew assembleDebug
```

生成的 APK 在 `app/build/outputs/apk/debug/app-debug.apk`，传到手机安装即可。

## 如何使用

### 1. 配置今日任务

1. 打开「今日任务」App
2. 点击底部「＋ 添加时间段」
3. 选择开始时间、结束时间，填写任务内容（如「写周报」）
4. 点击「保存」
5. 重复添加，直到排满一天

### 2. 添加桌面小组件

1. 长按手机桌面空白处
2. 点击「小组件 / Widgets」
3. 找到「今日任务小组件」
4. 拖到桌面

### 3. 效果

- 小组件会显示当前时段的「现在 · 09:00 - 10:00」和任务内容
- 时间过了会自动跳到下一个时段
- 没任务时显示「接下来」的下一场，或「可以休息啦」
- 点击小组件可直接打开 App 修改任务

## 目录结构

```
今日任务小组件/
├── app/
│   ├── build.gradle                 # App 构建配置
│   └── src/main/
│       ├── AndroidManifest.xml      # 清单（声明 Activity 和小组件）
│       ├── java/com/example/dailytaskwidget/
│       │   ├── MainActivity.java            # 主界面：配置任务
│       │   ├── DailyTaskWidgetProvider.java # 小组件：实时显示+自动切换
│       │   ├── Task.java                    # 任务数据模型
│       │   ├── TaskStore.java               # 本地存储
│       │   └── TaskAdapter.java             # 列表适配器
│       └── res/
│           ├── layout/              # 界面布局
│           ├── values/              # 颜色、主题、字符串
│           ├── drawable/            # 图标、背景
│           └── xml/                 # 小组件配置
├── build.gradle
└── settings.gradle
```

## 技术说明

- **语言**：Java + Android SDK
- **最低系统**：Android 7.0（API 24）
- **存储**：SharedPreferences（JSON 格式，按日期分键）
- **小组件刷新**：`AlarmManager.setExactAndAllowWhileIdle` 每分钟精准触发，实现到点自动切换

## 常见问题

**Q：小组件不自动刷新？**
部分国产手机（小米、华为等）会限制后台，需要在系统设置里给 App 开启「自启动」和「后台运行」权限。

**Q：能配置未来某一天的任务吗？**
当前版本默认编辑「今天」。数据按日期存储，未来版本可加日期切换功能。

**Q：数据会丢吗？**
数据存在手机本地，卸载 App 会清除。换手机需手动备份（后续可加导出功能）。
