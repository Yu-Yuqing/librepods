# LibrePods 1.0.10-RC1：AirPods 系统电量同步与语音助手权限引导

这是基于 [beidaoc/librepods v1.0.10](https://github.com/beidaoc/librepods/releases/tag/v1.0.10) 修改的社区候选版，保留原项目功能和 GPL-3.0 许可。

这次主要处理了两个使用问题：

- App 内能看到 AirPods 电量，但系统蓝牙设置和读取系统电量的桌面小组件不显示。现在将 App 获取的电量同步到系统正常使用的蓝牙电量缓存，不额外连接耳机。左右耳都连接时取有效电量的较低值，单耳时使用该耳电量。
- 耳机长按能在 App 内唤起助手，返回桌面或锁屏后却失效。现在选择“语音助手”时会显示后台弹出界面、锁屏显示等权限的检查结果，并提供系统设置入口。App 设置中也能重新检查。

此外，新安装时 **Use Material 3 Expressive 默认关闭**，仍可手动开启；已有用户保存过的选择继续生效。

## 下载

[GitHub Release：v1.0.10-RC1](https://github.com/Yu-Yuqing/librepods/releases/tag/v1.0.10-RC1)

- 日常使用下载 `release.apk`。
- 需要调试或导出日志时使用 `debug.apk`。
- 对应 Root 模块 ZIP 为可选文件，仅使用本次系统电量同步不要求安装它。
- 同时提供对应源码和 SHA-256 校验值。

## 使用前请看

系统电量同步需要 **Android 14+ 和 LSPosed/Vector**。请在框架中启用 LibrePods，勾选“蓝牙（com.android.bluetooth）”，重启并解锁、连接耳机。原来的其他作用域按需保留。

此社区版使用与上游不同的签名，不能直接覆盖原版。请先备份原设置、保留旧安装包，再卸载旧版后安装。使用旧 Root 模块的用户还需先停用旧模块，避免签名冲突。

小米用户选择长按语音助手后，按页面提示允许“后台弹出界面”和“锁屏显示”；这些是厂商特殊权限，需要在系统设置中手动开启。

这是 RC 候选版，已进行源码构建和自动化测试；**尚未进行该集成 APK 的手机安装实测**。蓝牙内部接口也可能因 ROM 或系统更新而不同，欢迎反馈设备、系统版本和实际表现。

[源码仓库](https://github.com/Yu-Yuqing/librepods) · [详细修改和构建说明](https://github.com/Yu-Yuqing/librepods/blob/main/docs/release-notes-rc1.md)
