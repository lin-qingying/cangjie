# Cangjie Enhancer — DevEco Studio Plugin

> 基于 **华为 DevEco Studio** 内置仓颉（Cangjie）插件的增强扩展插件。

---

## 简介

本插件是一个 **DevEco Studio** 扩展插件，以华为官方内置的**仓颉语言（Cangjie）插件**为基础，
在其之上提供额外的语言支持、代码分析、效率工具等增强能力，帮助开发者在鸿蒙（HarmonyOS）
生态中更高效地使用仓颉语言进行开发。

---

## 功能特性

- 🔍 **增强代码检查** — 针对仓颉语言的自定义 Inspection 规则
- ⚡ **快捷意图（Intention）** — 常用代码模式的一键重构与生成
- 📝 **代码模板** — 仓颉常用代码片段（Live Templates）
- 🛠️ **工具窗口** — 仓颉项目结构视图增强
- 🔗 **深度集成** — 依赖并扩展官方 Cangjie 插件的语言服务

---

## 环境要求

| 依赖项 | 版本要求 |
|--------|----------|
| DevEco Studio | 4.x 及以上（含内置 Cangjie 插件） |
| JDK | 17（DevEco Studio 内置 JBR 17） |
| Gradle | 见 `gradle/wrapper/gradle-wrapper.properties` |

---

## 开发与构建

### 1. 克隆项目

```bash
git clone https://github.com/your-org/cangjie-enhancer.git
cd cangjie-enhancer
```

### 2. 配置 DevEco Studio 路径

本插件使用**本地 DevEco Studio 安装目录**作为编译 SDK，而非 JetBrains 官方 Maven 仓库。
请通过以下任一方式指定路径：

**方式一：环境变量（推荐）**

```bash
# macOS
export DEVECO_HOME="/Applications/DevEco-Studio.app/Contents"

# Windows (PowerShell)
$env:DEVECO_HOME = "C:\Program Files\Huawei\DevEco Studio"

# Linux
export DEVECO_HOME="/opt/DevEco-Studio"
```

**方式二：`gradle.properties`**

```properties
devEcoHome=/Applications/DevEco-Studio.app/Contents
```

### 3. 构建插件

```bash
./gradlew buildPlugin
```

构建产物位于 `build/distributions/cangjie-enhancer-*.zip`。

### 4. 本地调试运行

```bash
./gradlew runIde
```

此命令会以插件沙箱模式启动本地 DevEco Studio，可实时调试。

---

## 安装

### 手动安装（推荐用于开发/测试）

1. 执行 `./gradlew buildPlugin` 生成 zip 包
2. 打开 DevEco Studio
3. `Settings / Preferences → Plugins → ⚙️ → Install Plugin from Disk…`
4. 选择 `build/distributions/cangjie-enhancer-*.zip`
5. 重启 IDE

### 发布安装

如通过华为 AppGallery Connect 或内部插件仓库分发，请参考华为官方插件发布文档。

---

## 项目结构

```
cangjie-enhancer/
├── src/
│   └── main/
│       ├── kotlin/               # 插件主要逻辑（Kotlin）
│       └── resources/
│           └── META-INF/
│               └── plugin.xml    # 插件声明与扩展点注册
├── build.gradle.kts              # 构建脚本（localPath 指向 DevEco）
├── gradle.properties             # 版本与路径配置
├── settings.gradle.kts
└── README.md
```

---

## 与标准 IDEA 插件的关键差异

| 项目 | 标准 IDEA 插件 | 本插件（DevEco） |
|------|---------------|----------------|
| SDK 来源 | JetBrains Maven | 本地 DevEco 安装目录 |
| 依赖声明 | `intellijIdea("2024.x")` | `local(file(devEcoHome))` |
| 额外依赖 | 无 | `bundledPlugin("com.huawei.deveco.language.cangjie")` |
| 分发渠道 | JetBrains Marketplace | Huawei AppGallery Connect / 内部 |
| 目标语言 | Java/Kotlin/其他 | 仓颉（Cangjie）/ ArkTS |

---

## 贡献指南

1. Fork 本仓库
2. 新建特性分支：`git checkout -b feature/my-feature`
3. 提交改动：`git commit -m 'feat: add my feature'`
4. 推送分支：`git push origin feature/my-feature`
5. 发起 Pull Request

---

## 许可证

[Apache License 2.0](LICENSE)

---

## 相关资源

- [华为开发者文档 — DevEco Studio](https://developer.huawei.com/consumer/cn/deveco-studio/)
- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/)
- [仓颉编程语言官网](https://cangjie-lang.cn/)