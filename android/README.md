# 懒得找了 · Android App

原生 Kotlin + Jetpack Compose 复刻 landezhaole 网站（v1.0.1）。

- 包名：`com.landezhaole.app`，versionCode 2 / versionName 1.0.1，minSdk 26 / targetSdk 34
- 数据：`android/app/src/main/assets/data.json`（19 分类 / 2320 站点快照，与网站同源；网站数据更新时同步替换该文件再发版）
- 功能：19 分类区 + 子 Tab、顶部 10 分类快捷入口、站内搜索（标题/简介/标签匹配，NEW 置顶）、8 个搜索引擎外跳、卡片七彩角标（HOT/NEW/推荐/精选）、今日点击（本机当日，点卡片记一次）、夜间模式、回到顶部
- 图标：卡片用彩色底 + 前 2 字（不拉第三方 favicon，避免地球占位与加载失败问题）
- 打包：推到 main 即自动跑 GitHub Actions，产物在 Actions → Artifacts → landezhaole-debug-apk；本地：`cd android && ./gradlew :app:assembleDebug`

网站版文件（index.html / data.json / version.json 等）仍在仓库根目录，与安卓工程共存。

- 更新弹窗：手绘涂鸦风（胶带 + 粗黑手绘边框 + 蜡笔涂抹进度条，下载红条 → 安装蓝条脉动 → 完成绿勾），顶部“检查更新”按钮触发；四态：下载中“不更了”→ 取消“继续画完”→ 安装中 → 完成“好耶，去找站”，另有“再来一遍”重播。注意：进度目前是演示模拟（与原 HTML 演示一致），真正的 APK 下载安装需接 Release 直链后再替换进度来源。
