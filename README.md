<div align="center">

# BHYG Android
本仓库将不提供发行版

### 哔哩哔哩会员购抢票助手 Android 版

基于 Android + Jetpack Compose 实现的 BHYG 移动端移植项目，聚焦于会员购项目解析、扫码登录、购票人选择、自动重试下单与支付二维码拉起。

<p>
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B-ff6b9d?style=for-the-badge" alt="platform" />
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-2b2d42?style=for-the-badge" alt="kotlin" />
  <img src="https://img.shields.io/badge/Jetpack-Compose-ff89b5?style=for-the-badge" alt="compose" />
  <img src="https://img.shields.io/badge/Status-Experimental-3d405b?style=for-the-badge" alt="status" />
</p>

</div>

---

## 项目简介

这是一个面向 Android 设备的 `BHYG` 客户端实现，目标是把原本偏桌面端/脚本端的抢票流程整理成更直观的移动端操作体验。

当前版本已经实现了以下核心流程：

- 哔哩哔哩账号扫码登录与登录态检测
- 会员购项目解析，自动读取场次与票档信息
- 根据项目限制读取常用购票人并进行勾选
- 支持票数、重试间隔、热票模式等参数配置
- 自动循环请求 `prepare` / `createOrder`
- 抢单成功后自动拉取支付二维码
- 内置控制台日志，便于观察执行状态

---

## 界面预览

<table>
  <tr>
    <td align="center"><img src="./docs/images/login.png" width="260" alt="登录页" /></td>
    <td align="center"><img src="./docs/images/config.png" width="260" alt="项目配置页" /></td>
    <td align="center"><img src="./docs/images/console.png" width="260" alt="控制台页" /></td>
  </tr>
  <tr>
    <td align="center"><strong>扫码登录</strong></td>
    <td align="center"><strong>项目配置</strong></td>
    <td align="center"><strong>控制台执行</strong></td>
  </tr>
</table>

---

## 致谢

本项目的核心思路、接口流程认知与方向参考，来自原项目 [ZianTT/BHYG](https://github.com/ZianTT/BHYG)。

感谢原作者与相关贡献者所做的探索与沉淀。这个 Android 版本本质上是基于原有思路做的一次移动端实现与界面重构，很多流程理解都受益于原项目。

如果你正在使用本项目，也建议同时关注原仓库：

- 原项目地址：[ZianTT/BHYG](https://github.com/ZianTT/BHYG)
- 当前 Android 实现：[wy3057/bhyg-Android](https://github.com/wy3057/bhyg-Android)

---

## 功能说明

### 1. 账号登录

- 使用哔哩哔哩 Web 二维码登录接口生成登录码
- 轮询扫码状态，支持未扫码、待确认、成功、过期等状态反馈
- 登录成功后自动读取账号昵称、MID、头像
- Cookie 会持久化保存在本地，便于后续复用登录态

### 2. 项目解析

- 输入会员购 `ProjectId`
- 自动请求项目信息并解析项目名称、场次列表、票档列表
- 支持切换不同场次与票档

### 3. 购票人配置

- 自动读取当前账号下常用购票人信息
- 根据项目的实名绑定规则进行勾选
- 当票数与实名购票人数不匹配时，会在启动前阻止下单

### 4. 抢票策略

- 可配置购买张数
- 可配置重试间隔，当前范围为 `100ms - 2000ms`
- 可切换热票模式，用于热点项目场景下的参数适配

### 5. 自动下单

- 自动请求 `prepare token`
- 自动调用 `createOrder`
- 成功后轮询订单状态并尝试获取支付二维码
- 在控制台中输出完整运行日志

---

## 使用流程

1. 打开 App，进入 `账号登录` 页面。
2. 点击获取二维码，使用哔哩哔哩 App 扫码并确认登录。
3. 切换到 `项目配置`，输入目标会员购项目的 `ProjectId`。
4. 选择场次、票档、购票人，并根据需要调整张数与重试间隔。
5. 进入 `控制台`，点击启动按钮开始循环下单。
6. 若成功锁单，界面会弹出支付二维码，请尽快完成支付。

---

## 开发环境

- Android Studio 最新稳定版
- JDK 11
- Android SDK 36
- 最低支持 Android 7.0 `API 24`

项目主要技术栈：

- Kotlin
- Jetpack Compose
- OkHttp
- Moshi / Retrofit
- Kotlin Coroutines
- ZXing

---

## 本地运行

```bash
git clone https://github.com/wy3057/bhyg-Android.git
cd bhyg-Android
```

然后使用 Android Studio 打开项目目录并等待 Gradle 同步完成。

如果你只需要本地调试：

1. 使用 Android Studio 打开项目。
2. 确认本地已安装 JDK 11 与对应 Android SDK。
3. 连接真机或启动模拟器。
4. 直接运行 `app` 模块即可。

说明：

- 当前仓库中的 `.env.example` 属于初始模板遗留内容，现阶段运行本项目并不依赖 `GEMINI_API_KEY`。
- `debug` 构建已配置调试签名。
- `release` 构建默认读取环境变量中的签名信息。

---

## 项目结构

```text
app/src/main/java/com/example/
├── bilibili/
│   ├── BilibiliSession.kt     # 登录、项目解析、下单、订单状态查询
│   ├── QrCodeGenerator.kt     # 二维码生成
│   └── TokenGenerator.kt      # 请求参数相关生成逻辑
├── ui/
│   ├── GrabberScreen.kt       # 主界面与三个功能页
│   ├── GrabberViewModel.kt    # 页面状态与抢票任务调度
│   └── theme/                 # 主题配色
└── MainActivity.kt            # 入口 Activity
```

---

## 注意事项

- 本项目仅供学习 Android 网络请求、状态管理与界面实现思路使用。
- 会员购接口、风控逻辑、参数要求可能随时间变化而失效。
- 抢票、下单、支付相关操作具有真实资金风险，请自行判断后果并谨慎使用。
- 使用任何第三方工具造成的账号、订单、风控、法律与经济后果，请自行承担。

---


