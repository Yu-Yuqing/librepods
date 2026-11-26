# LibrePods 1.0.10-RC5

统计页向左滑动可从日切到周、从周切到月。向右滑动可返回，也可以点顶部标签直接选择时段。点选柱子仍在图中查看对应时长。

在「统计」右上角点「⋮」，可导出全部佩戴记录到一个 JSON 文件。请将文件保存到下载目录或自己的云盘，重装后在同一菜单选择「导入佩戴记录」。备份包括各次佩戴的开始与结束时间。进行中的记录导出到当时为止。导入时保留本机记录，同一起点的较长备份可补全历史记录，其他重叠区间会跳过。文件无效时不修改数据库。

RC5 与 RC4 完整版沿用同一签名。已安装 RC3 或 RC4-Upgrade 的用户仍使用早期调试签名，请在保留记录前提下安排迁移。

## RC2 发布签名

GitHub 上 v1.0.10-RC2 的发布 APK 证书 SHA-256 为 `e8509e9b460eec156ee9bed8eaccf19e84d69662da06092a2a559d29cf5681a9`。本次提供的 RC5 APK 与 RC4 完整版使用 `cb4923032d2aa13e437af1ce543966e66321004c3e1f31d726dd470ed8264e80`。

如需给 RC2 用户覆盖升级，请在保有 RC2 签名密钥的环境构建此源码。若 GitHub Actions 仍保有 RC2 的四项 RELEASE_* secrets，可在本版源码分支运行 Android CI。下载该次运行的 release APK，用 Android SDK Build Tools 的 `apksigner verify --print-certs 文件名.apk` 核对证书，再发布。
