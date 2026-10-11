# 14.5 可复查记录：最终分发制品 × JetBrains Plugin Verifier（2026-10-11）

本文件是 tasks.md 14.5「保存可复查记录」那半的落点，记的是**可机械化的那一半**——最终分发制品在真实平台上的
**加载/结构面**核验。**触发 / 排序 / 插入 / 撤销 / 错误态**那半需要真人 GUI 会话（`runIde`），本文件只记录它的前置与未闭环状态。

## 1. 被测对象与命令

- 制品：宿主 `product/idea-plugin/build/distributions/intellij-cangjie-2.1.4.zip`（`buildPlugin` 产物，42223071 字节，2026-10-10 21:33）。
  制品内插件描述符：`intellij-cangjie/lib/idea-plugin-2.1.4.jar!/META-INF/plugin.xml`，
  `<idea-version since-build="253" until-build="253.*"/>`（与 12.7 的 `PlatformMatrixDeclarationTest` 声明一致）。
- 被测平台：**IU-253.29346.50**（与宿主 `gradle.properties` 的 `baseIDE=IU` / `ideRunVersion=253.29346.50` 同源，
  是产品实际构建与测试所用的那一个）。
- 配置改动：宿主 `build-logic/src/main/kotlin/cangjie.intellij-product.gradle.kts` 新增

  ```kotlin
  pluginVerification {
      ides {
          ide(IntelliJPlatformType.fromCode(ideToRunType.get()), ideRunVersion.get())
      }
  }
  ```

- 复跑命令：

  ```bash
  cd "D:/code/intellij/cangjie/intellij-ide/.claude/worktrees/musing-bun-5ae793" && ./gradlew.bat -p . :product:idea-plugin:verifyPlugin -Pcangjie.upstream.root=D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun
  ```

- 本次运行：IntelliJ Plugin Verifier **1.410**，20 m 50 s（另下载依赖插件 1.17 GB / 4 m 01 s），**BUILD SUCCESSFUL**。
- 报告：`product/idea-plugin/build/reports/pluginVerifier/IU-253.29346.50/report.html`（412545 字节）。

## 2. 结论（verifier 原文）

```
Plugin cn.cangnova.cangjie:2.1.4 against IU-253.29346.50: Compatible.
14 usages of scheduled for removal API and 168 usages of deprecated API.
265 usages of experimental API. 104 usages of internal API.
2 non-extendable API usage violations. 3 plugin configuration defects
```

`Dynamic Plugin Eligibility: Plugin can probably be enabled or disabled without IDE restart`。

即：**制品在被测平台上判定为 Compatible**（无「不兼容 API 调用」级问题），逐条计数如上。

### 2.1 待跟进的三个结构告警（原文）

```
Plugin structure warnings (3):
    There is a duplicated dependency on 'org.toml.lang'. Remove this dependency by updating the plugin.xml file.
    There is a duplicated dependency on 'com.redhat.devtools.lsp4ij'. Remove this dependency by updating the plugin.xml file.
    There is a duplicated dependency on 'com.intellij.modules.json'. Remove this dependency by updating the plugin.xml file.
```

判定与处置建议（**本轮不改**，理由如下）：

- 制品 `plugin.xml` 的 `<dependencies>` 声明五条：`com.intellij.modules.platform`、`com.intellij.modules.json`、
  `org.toml.lang`、`com.intellij.copyright`、`com.redhat.devtools.lsp4ij`；其中前三条 + lsp4ij 又被宿主
  `cangjie.intellij-product.gradle.kts` 的 `bundledPlugins(...)` / `plugins("com.redhat.devtools.lsp4ij:0.19.2")` 声明，
  并被 IPGP 一并交给 verifier 作为「依赖插件」。verifier 因此把「描述符声明 + 依赖插件集里同样有」读成重复依赖。
- **不能照字面删**：`com.redhat.devtools.lsp4ij` 是运行时必需且不随 IDE 分发的外部插件，删掉 `<plugin id="..."/>`
  会让制品在真实 IDE 里解析不到依赖（加载面直接变红）；另两条同理要先确认 IPGP 会不会补进描述符。
- 因此这三条按**「需要一次 runIde 冒烟 + 重跑 verifier 才能改」的宿主打包决策**处理，不在本变更内改动描述符。
- **交叉核对（2026-10-11）**：IPGP 自带的配置校验 `:product:idea-plugin:verifyPluginProjectConfiguration` 只报一条——
  「`until-build` 在 243+ 平台上应移除以便前向兼容」；**没有**报这三条重复依赖。故「重复依赖」判读为
  **verifier 侧看到的「描述符声明 × 依赖插件集」重叠**，不是描述符本身的缺陷。
  而 `until-build=253.*` 是宿主**刻意**的声明口径（只声明已测平台，见 12.7 的 `PlatformMatrixDeclarationTest`），
  属产品决策而非缺陷；要放开前向兼容得先有该平台的实测证据。
- verifier 另有一条**下载告警**（非制品问题）：`org.jetbrains.plugins.clion.radler:253.29346.240` 下载 11 m 19 s 后
  以 `closed` 失败，verifier 记 `misses/failures` 后继续，不影响本次结论。

### 2.2 其余计数说明

- `265 usages of experimental API` / `104 usages of internal API`：属平台注解面提示（`@ApiStatus.Experimental`/`Internal`），
  不构成不兼容；宿主仓已有既存使用面（如 `CangJieBuildAdapter` 的 `StartBuildEventImpl`、`CangJieCodeStyleSettingsFactory`
  的 `CodeStyleSettingsServiceListener` 等），与本变更无关。
- `2 non-extendable API usage violations`：`GlobalSearchScope.getProject()` 被
  `CaBaseResolutionScope` / `CaIdeModuleContentScope` 覆写——平台标 `@ApiStatus.NonExtendable`。
  属既存实现面（分析层解析作用域），本变更未触碰；如实记录，不在此轮改动。
- `14 usages of scheduled for removal API`：verifier 的计数行给出总数，逐条清单在 `report.html` 内
  （`Scheduled for removal` 段）。给后续升级平台时当输入。

## 3. 未闭环面：真人 GUI 会话那一半

以下六项**没有**在本记录内被验收，仍需一次真人会话（`runIde` 或 `runIdeForUiTests`）：

| 项 | 现状 |
| --- | --- |
| 加载 | verifier 覆盖了描述符/类引用/平台兼容；**沙箱内是否真的加载起来**仍需 runIde 目视 |
| 触发 | light fixture 已验 BASIC/SMART 触发（14.2），GUI 自动触发未验 |
| 排序 | 候选顺序未在真实 IDE 里核对 |
| 插入 | fixture 已验（14.2），GUI 侧未验 |
| 撤销 | **见下节：fixture 无法验收，已定位为夹具限制；真机是唯一出口** |
| 错误态 | 未验（LSP 未启动一条已在 14.3 机械覆盖） |

另：14.2 里被判为「夹具限制」的第二条（库侧候选按前缀发现）也等这次真机复验。

## 4. 撤销项的独立复验（2026-10-11，物理文件 + 真编辑器）

14.2① 的「撤销不回退」原先只在**内存 light 文档**上观测到。本轮用**磁盘真实文件**（`tempDirFixture.createFile`
+ `configureFromExistingVirtualFile`）重做了这件事，并加了**对照组**（同夹具、同载体上的一次普通 `WriteCommandAction`
文档编辑），触发方式各给两种（编辑器动作 `ACTION_UNDO` 与 `UndoManager.undo(editor)`）。

实测（用例在宿主 `product/idea-plugin/src/test/kotlin/org/cangnova/cangjie/ide/completion/CangJieCompletionUndoOnFileTest.kt`，四个变体一次跑完，**四条全不过**）：

| 载体 | 编辑 | 触发 | 结果 |
| --- | --- | --- | --- |
| 磁盘真实文件（`tempDirFixture.createFile` + `configureFromExistingVirtualFile`） | `WriteCommandAction` 里 `document.insertString` + `commitDocument`（对照组） | `myFixture.performEditorAction(IdeActions.ACTION_UNDO)` | 不回退（`let value = 1let other = 2` 仍在） |
| 同上 | 对照组 | `UndoManager.getInstance(project).undo(fileEditor)`（`FileEditor` 取自 `FileEditorManager.getAllEditors`） | 不回退 |
| 同上 | `completeBasic()` + `finishLookup('\n')` 接受候选 | `ACTION_UNDO` | 不回退（`return limit` 仍在） |
| 同上 | 补全接受候选 | `UndoManager.undo(fileEditor)` | 不回退 |

即「撤销在这个宿主 light fixture 里对任何命令内文档改动都不生效」——**载体**是内存还是磁盘、**改动**是普通编辑还是补全插入、**触发**经编辑器动作还是经 `UndoManager`，结论一致。
随后把这条**夹具事实**钉成了两条通过用例：撤销触发后文本必须仍停在编辑后（将来平台/夹具修好撤销时它们会失败，届时翻成「撤销后逐字回到补全前」的真验收）。
故 14.2① 的判定维持：**该夹具验不了撤销，不是补全缺陷**；真机撤销验收归 14.5 的 GUI 会话。
