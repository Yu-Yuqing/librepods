# LibrePods 1.0.10-RC1

## 下载与安装

**只需下载并安装 [LibrePods-FOSS-v1.0.10-RC1-release.apk](https://github.com/Yu-Yuqing/librepods/releases/download/v1.0.10-RC1/LibrePods-FOSS-v1.0.10-RC1-release.apk)。**

本页只提供一个安装包。下方 GitHub 自动生成的 **Source code** 是源码，不用安装；本次电量同步不需要额外刷 ZIP。

## 本次更新

- 集成 AirPods 系统电量同步，供系统蓝牙设置和桌面电量小组件读取。
- 增加语音助手的后台弹出界面、锁屏显示权限引导。
- 新安装默认关闭 Use Material 3 Expressive，保留用户已保存的选择。

## 使用说明

1. 系统电量同步需要 **Android 14+ 和 LSPosed/Vector**。在框架中启用 LibrePods，勾选“蓝牙（com.android.bluetooth）”，重启并解锁、连接耳机。
2. 本社区版签名与上游不同，**不能直接覆盖原版**。先备份设置，再卸载旧版后安装；使用旧 Root 模块的用户需先停用旧模块，避免签名冲突。
3. 长按设置选择“语音助手”后，按页面提示开启后台弹出界面、锁屏显示等权限。

这是基于 [beidaoc v1.0.10](https://github.com/beidaoc/librepods/releases/tag/v1.0.10) 修改的社区候选版，保留 GPL-3.0 许可。[147 项自动化测试已通过](https://github.com/Yu-Yuqing/librepods/actions/runs/42463618468)，集成 APK 尚未进行安装后的手机实机验证。

<details>
<summary>APK SHA-256 校验值</summary>

`4c18fce69c1100688b88bf0b0b87b3236d8ab2f36d5aa1415ba7639c5c608dd4`

</details>