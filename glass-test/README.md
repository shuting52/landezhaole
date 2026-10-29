# 懒得找了 · 液态玻璃测试版

测试用 App，不是正式版，不会覆盖已有的懒得找了 App。

- 包名：`com.landezhaole.glasstest`，version 1.0.0（code 1），可与主 App（`com.landezhaole.app`）和落地 App（`com.landezhaole.landing`）同时装
- 液态玻璃：Kyant0/AndroidLiquidGlass Backdrop 2.0.1，按上传的安卓完整代码写法（`rememberLayerBackdrop` + `layerBackdrop` + `drawBackdrop`，vibrancy + blur 4dp + lens 12dp/24dp）
- 数据：CF Pages 项目 landezhaole 的 `data.json` 原样内置，21 分类 / 2603 站点 / 174 NEW
- 主题：landezhaole.pages.dev 当前的 Libraries 浅深两套，数值翻译成 Kotlin 颜色：浅底 #fdfdfd / 强调 #0073e5，深底 #121212 / 强调 #55cfff
- 原生 Kotlin + Jetpack Compose，没有 WebView，没有 HTML

## 功能（测试版，刻意做小）

- 首页玻璃卡片：分类 / 站点 / NEW 总数，Libraries 浅深切换
- 搜索：搜站点名、介绍、链接
- 分类横向切换 + 只看 NEW + 底部玻璃 Dock
- 站点卡片：点卡片用系统浏览器打开对应网站
- 全部列表最多显示 300 条，搜一下更快；真折射需要 Android 13+，低版本会退化显示

## 编译

本地 Android Studio 打开本工程，或用系统 Gradle（仓库里没放 wrapper jar，CI 同样用系统 Gradle 9.3.2）：

```bash
gradle :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

说明：测试版放在 CF 项目仓库 `shuting52/landezhaole` 的 `glass-test/` 子目录里，网站根目录文件一点没动。推到 main 后 `.github/workflows/build-glass-test.yml` 自动编包，并把包提交回 `glass-test/app-debug.apk`，raw 下载链接：

```
https://raw.githubusercontent.com/shuting52/landezhaole/main/glass-test/app-debug.apk
```

注意：这个 raw 链接只有在 CI 第一次成功并提交 APK 后才有效；提交信息带 `[skip ci]`，不会无限循环触发构建。
