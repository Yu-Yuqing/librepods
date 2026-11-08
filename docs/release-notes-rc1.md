# LibrePods 1.0.10-RC1

**下载安装：[LibrePods-FOSS-v1.0.10-RC1-release.apk](https://github.com/Yu-Yuqing/librepods/releases/download/v1.0.10-RC1/LibrePods-FOSS-v1.0.10-RC1-release.apk)**

本页只提供一个安装 APK。GitHub 自动生成的 Source code 是源码，不用安装。

## 本次调整

- 后台弹出界面的说明缩短为“允许后台唤醒语音助手”，锁屏显示缩短为“允许锁屏唤醒语音助手”。
- 删除权限列表下方的长段说明，权限状态保持单行显示。
- 修正非 Material 风格的长文字布局：空间音频、通知使用权和语音引擎的说明放在标题下方，并给右侧状态或箭头留出空间。

保留系统电量同步、语音助手权限引导，以及默认关闭 Material 3 Expressive 的设置。未修改字号配置。

## 安装说明

- 已安装本社区 RC1 的用户可直接覆盖更新，签名保持一致。
- 系统电量同步需要 Android 14+ 和 LSPosed/Vector，在框架中启用 LibrePods 并勾选“蓝牙（com.android.bluetooth）”。
- 上游原版使用不同签名，切换时需先备份设置、卸载旧版；使用旧 Root 模块的用户需先停用旧模块，避免签名冲突。

基于 [beidaoc v1.0.10](https://github.com/beidaoc/librepods/releases/tag/v1.0.10) 修改，保留 GPL-3.0 许可。本版完成 Release 编译和签名校验，尚未进行安装后的手机实机验证。