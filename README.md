# SMS Forwarder to DingTalk (短信转发钉钉工具)

> 一个极客风格的 Android 短信转发工具，支持将手机短信实时转发到钉钉群机器人。

## 📸 界面预览
<img width="500" height="1000" alt="image" src="https://github.com/user-attachments/assets/0d6f624a-faf2-439b-8371-e6074d0ab732" />

## ✨ 主要功能
*   **短信监听**：实时捕获系统短信消息。
*   **钉钉转发**：支持自定义钉钉机器人 Webhook，支持 Markdown 格式推送。
*   **极客 UI**：采用 Jetpack Compose 构建的黑客终端风格界面 (Hacker Green)。
*   **后台保活**：内置前台服务 (Foreground Service) 与开机自启功能，降低被杀后台概率。
*   **配置管理**：支持“打开 APP 自动开启”及“手机开机自启”配置。
*   **隐私安全**：所有配置仅保存在手机本地，无第三方服务器中转。

## 🛠️ 技术栈
*   **语言**: Kotlin
*   **UI**: Jetpack Compose (Material3)
*   **网络**: OkHttp
*   **架构**: MVVM (简易版) + StateFlow
*   **其他**: DataStore / SharedPreferences, BroadcastReceiver

## 🚀 使用指南
1.  **下载安装**: 下载最新 Release APK 安装到手机。
2.  **权限授予**: 首次运行需授予“短信读取”及“通知”权限。
3.  **配置 Webhook**:
    *   在钉钉群设置中添加“自定义机器人”。
    *   安全设置选择“关键词”，填入 `短信`。
    *   复制 Webhook 地址填入 APP 并点击保存。
4.  **保活设置 (重要)**:
    *   请务必在手机设置中将本应用设置为“允许自启动”和“电池优化无限制”，否则无法在后台长时间运行。

## ⚠️ 免责声明
本项目仅供学习交流使用。请勿用于非法用途。开发者不对使用本软件产生的任何后果负责。

---
Created by [Zayika7]
