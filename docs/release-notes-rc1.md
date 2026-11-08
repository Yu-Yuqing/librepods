# LibrePods 1.0.10-RC1 社区版

基于 [beidaoc/librepods v1.0.10](https://github.com/beidaoc/librepods/releases/tag/v1.0.10) 修改，保留上游功能与 GPL-3.0 许可。这是第三方社区构建，非上游官方版本。

## 本次修改

- **系统蓝牙电量同步**：把 LibrePods 已获取的 AirPods 左右耳电量写入系统正常使用的蓝牙电量缓存，并由系统通知设置和桌面小组件刷新。左右耳都有效时取较低值；单耳时使用该耳电量；未知电量不伪造成 100%。补充左右耳与充电盒的电量元数据。
- **无需独立电量补丁**：同步逻辑集成在 App 和已有的蓝牙 LSPosed 作用域内，不创建额外的耳机连接，不需要为此安装新的全局挂载组件。
- **语音助手权限引导**：选择长按“语音助手”后显示后台弹出界面、锁屏显示等所需权限，点击项目可前往系统设置；App 设置内也保留入口。返回 App 后重新检查状态。系统限制读取状态时显示“请手动确认”。
- 修正原版电量元数据写入时反向的蓝牙特权判断。
- Fork 的 Android CI 在没有签名 Secrets 时可运行单元测试和生成调试 APK，不会尝试使用空签名或向上游 Discord 发送通知。发布签名版需要仓库拥有者配置自己的签名。

- **外观默认值**：新安装默认关闭 Use Material 3 Expressive，保留开关及已有用户的设置。

## 使用条件

系统电量同步需要 **Android 14 或以上**，以及支持当前 API 的 **LSPosed/Vector**。在框架中启用 LibrePods，并勾选“蓝牙（com.android.bluetooth）”，重启、解锁并连接 AirPods。保留你原有的其他 LibrePods 作用域。

此同步路径不会重新配对或争抢耳机连接。它依赖 Android 蓝牙内部接口，其他 ROM 或后续系统版本仍需验证。原独立电量同步补丁已在 Android 17 / HyperOS OS4.0 上验证系统设置和桌面小组件可显示电量；集成版还需要对应的安装后验证。

若此前安装了“LibrePods 电量同步补丁”（local.librepods.batterysync），切换到集成版后请在 LSPosed 停用该独立补丁，避免重复同步。

## 安装与签名

本社区版使用独立签名，**不能直接覆盖上游 APK**。先备份设置，再卸载旧版并安装本版；不要直接清除数据尝试解决签名冲突。使用旧版 Root 模块的用户还需先停用旧模块，避免系统内保留不同签名的 APK。

- `release.apk`：日常使用版本。
- `debug.apk`：调试和导出日志用；本次发布与 release 使用同一套社区签名。
- 对应 `release.zip` / `debug.zip`：可选的系统特权 Root 模块，必须搭配相同签名的 APK，且 Root 环境本身应支持挂载。**仅使用本次电量同步不要求安装这个 ZIP。**
- `source.zip`：对应修改源码，依照 GPL-3.0 提供。

## 小米语音助手设置

在 LibrePods 的长按设置中选择“语音助手”，按照页面提示开启：

1. **后台弹出界面**：返回桌面或其他 App 后仍能唤起助手。
2. **锁屏显示**：锁屏时允许启动助手界面，不会解除手机锁屏。
3. 按当前系统需要允许悬浮窗。语音助手本身的锁屏设置也可能影响使用。

这些是厂商特殊权限，无法通过普通 Android 权限弹窗自动授权，需要用户在系统页面开启。

## 从源码构建

需要 JDK 21、Android SDK 37.0、NDK 30.0.14904198 和 CMake 3.22.1：

```sh
cd android
./gradlew testFossDebugUnitTest
./gradlew assembleFossDebug
```

签名发布时，在本机 `android/local.properties` 或仓库 Secrets 配置 `RELEASE_STORE_FILE`（CI 中为 Base64 的 `RELEASE_KEYSTORE_FILE`）、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`，执行：

```sh
./gradlew packageReleaseArtifacts
```

保持后续版本使用同一签名。不要把密钥、密码、local.properties 或个人手机备份提交到 GitHub。
