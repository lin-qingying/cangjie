# IDEA 沙箱实例卡顿分析报告

- 分析时间：2026-10-22 12:46 ~ 12:56
- 实例：`intellij-ide` 的 `runIde` 沙箱（IntelliJ IDEA 2025.3.1 RC，`IU-253.29346.50`）
- 沙箱根：`intellij-ide/.intellijPlatform/sandbox/intellij-cangjie/IU-2025.3.1/`
- 工程：`C:\cj`（仓颉工程，源码仅 13 个 `.cj` 文件）

> 说明：本次会话的进程视图只暴露 DSH 自身作业对象内的进程（`Get-Process` 中无 `java.exe`/`idea64.exe`，
> `tasklist` 返回拒绝访问），因此无法在线 `jstack`。以下结论全部基于沙箱内 IDEA 自己产出的
> 冻结线程转储、后台写动作转储、`idea.log`、OpenTelemetry 指标与堆转储文件。

## 1. 运行参数与现状

`intellij-ide/build-logic/src/main/kotlin/cangjie.intellij-product.gradle.kts:217` 给出的 JVM 参数：

```
-Xms512m -Xmx6144m -XX:UseG1GC -XX:MaxMetaspaceSize=512m
-Didea.is.internal=true -Didea.debug.mode=true
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=<build>/dumpTmp
-XX:+ExitOnOutOfMemoryError（平台默认补充）
```

实际状态（`system_runIde/.pid` = 10964，`log_runIde/idea.log`）：

| 项 | 值 |
|---|---|
| 启动 | 12:43:02 |
| 最后一条日志 | 12:51:08（此后 5 分钟无输出，OTel 指标停在 12:50:26） |
| `.pid` | 仍存在（未正常退出清理） |
| 判定 | 进程在 12:51 前后非正常终止（OOM 直接退出 / 被杀），或进入完全 GC 停顿 |

## 2. 直接现象：今天的 4 次 UI 冻结

| 时间 | 冻结时长 |
|---|---|
| 12:45:32 | 8.2 s |
| 12:46:23 | 21.3 s |
| 12:49:49 | 10.4 s |
| 12:50:26 | **42.3 s** |

启动后 **2.5 分钟** 就出现第一次冻结，**3 分钟**内堆即接近耗尽。

## 3. 关键证据

### 3.1 堆已耗尽，GC 把整机拖死

`idea.log` 中 `PerformanceWatcherImpl` 记录：

```
12:46:10  High memory usage (free 124 of 6144 MB)
12:46:19  High memory usage (free 134 of 6144 MB)
12:50:04  High memory usage (free  28 of 6144 MB)   <-- 6GB 堆只剩 28MB
```

OpenTelemetry（`open-telemetry-metrics.2026-10-22-12-43-03.csv`）：

```
JVM.GC.collections          max 104
JVM.GC.collectionTimesMs    max 87278   (累计 GC 87.3 秒)
LowMemory.gcLoad            last 1.0    (GC 占用 100%)
LowMemory.gcOverloaded      last 1
LowMemory.memoryDeficitScore max 5
```

日志中 `Low memory signal received (gc overloaded: true)` 共 **209 次**，每 2~4 秒一条。

历史上已经发生过 **两次 IDE OOM 崩溃**：`intellij-ide/build/dumpTmp/` 下有
`java_pid42332.hprof`（18:12，8.9GB）与 `java_pid18884.hprof`（19:11，8.9GB），
两个 PID 都能与 `log_runIde/jcef_chromium_<pid>.log` 对上，即两次都是本沙箱 IDE 的堆转储，
合计占用 17.8GB 磁盘。

### 3.2 EDT 在等"写意向许可"，等了 42 秒

42 秒冻结的线程转储（`threadDumps-freeze-20261022-124946-.../threadDump-20261022-125004.txt`）中：

```
"AWT-EventQueue-0" TIMED_WAITING
  Unsafe.park
  LockSupport.parkNanos
  SuvorovProgress.dispatchEventsUntilComputationCompletes
  NestedLocksThreadingSupport$ComputationState.acquireWriteIntentPermit(...:432)
  NestedLocksThreadingSupport.doRunWriteIntentReadAction(...:728)
  NestedLocksThreadingSupport.runPreventiveWriteIntentReadAction(...:711)
  IdeEventQueue._dispatchEvent(IdeEventQueue.kt:492)
```

即：**EDT 在派发事件前申请写意向读锁，42 秒拿不到**。同一次运行还记录了
`Cannot execute background write action in 10 seconds`（`bg-wa/thread-dump-205654388.txt`），
说明后写动作同样被饿死 —— 典型的"长读动作持续占用 + 写动作饥饿"死结。

### 3.3 19 个线程排队等同一份 PSI 的 AST

109 个线程中 **27 个**在仓颉插件代码里，其中 **19 个 `BLOCKED`**，阻塞点完全一致：

```
com.intellij.psi.impl.source.PsiFileImpl.loadTreeElement(PsiFileImpl.java:218)
com.intellij.psi.impl.source.PsiFileImpl.calcTreeElement(PsiFileImpl.java:841)
com.intellij.psi.impl.source.PsiFileImpl.getNode(PsiFileImpl.java:1040)
org.cangnova.cangjie.psi.CjCommonFile.getFeaturesDirective(CjFile.kt:343)
org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder.buildFile$lambda$20(PsiRawCfirBuilder.kt:379)
...
org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirFileBuilder.buildRawCfirFileUncached(...:43)
org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirMacroConstructionInputKt
    .buildPreMacroRawBuildResultForModule(...:60)
org.cangnova.cangjie.macro.analysis.LLCfirMacroSessionConfigurator.runConstructionAttempt(...:127)
```

**19 个并发任务各自在做同一模块的整模块 pre-macro raw CFIR 重建**（`buildRawCfirFileUncached`
按定义不带缓存），每个都被 `PsiFileImpl.calcTreeElement` 的监视器串行化，
排在后面的只能等。工程一共只有 13 个 `.cj` 文件，却被并发重算 19 遍。

### 3.4 持锁线程正在做"debug 模式"的 PSI 解析

占据监视器的线程栈（148 帧）：

```
Throwable.fillInStackTrace            <-- 抓取完整栈
Throwable.<init>
com.intellij.lang.impl.MarkerOptionalData.notifyAllocated(MarkerOptionalData.java:76)
com.intellij.lang.impl.PsiBuilderImpl.createMarker(PsiBuilderImpl.java:900)
com.intellij.lang.impl.PsiBuilderImpl.mark(PsiBuilderImpl.java:891)
org.cangnova.cangjie.parsing.CangJieExpressionParsing.parseWhile(CangJieExpressionParsing.kt:1855)
...
LazyParseableElement.ensureParsed(LazyParseableElement.java:182)
FileElement.calcStubbedDescendants(FileElement.java:127)
FileElement.getStubbedSpine(FileElement.java:111)
FileTrees.reconcilePsi(FileTrees.java:170)
PsiFileImpl.loadTreeElement(PsiFileImpl.java:229)
PsiFileImpl.calcTreeElement(PsiFileImpl.java:841)
CjCommonFile.getFeaturesDirective(CjFile.kt:343)
PsiRawCfirBuilder.buildFile(PsiRawCfirBuilder.kt:379)
```

`PsiBuilderImpl` 字节码确认了这是**只有 debug 模式才会走**的分支：

```
13: getfield myDebugMode:Z
17: ifeq 31
20: ... MarkerOptionalData.notifyAllocated(markerId)
```

而 `MarkerOptionalData.notifyAllocated(int)` 的实现是无条件
`new Throwable("Created at the following trace.")` 并存进 `myDebugAllocationPositions`。

问题在于本仓库**无条件打开了这个模式**：

- `psi/src/org/cangnova/cangjie/parsing/CangJieParser.kt:156` → `psiBuilder.setDebugMode(true)`（文件解析）
- `psi/src/org/cangnova/cangjie/parsing/CangJieParser.kt:201` → `psiBuilder.setDebugMode(true)`（懒解析 block expression）

后果：解析每个文件、每个被懒解析的 block，**每次 `mark()` 都要构造一个带完整栈的 Throwable**，
并额外执行 marker 平衡断言等 debug 逻辑。这正是"为了读一个 `featuresDirective` 而把整棵树
（所有 lazy block）解析出来"时最昂贵的一步：CPU 成倍放大、短命对象暴增，直接推高 GC 压力，
同时把 `PsiFileImpl` 的锁握得更久，让 19 个线程排更久。

### 3.5 其它放大因素

- **重复全量重建索引**：`CjProjectsServiceImpl - Stub index rebuild requested after workspace initialization`
  出现 18 次，并触发 `Reindex requested by project root model changes`；每次启动都无条件重建。
- **分析异常后重试**：`LLCfirDiagnosticVisitor` 连续报
  `Error while resolving CfirClassImpl from IMPORTS to IMPLICIT_TYPES`（`LLCfirModuleLazyDeclarationResolver.kt:323`），
  诊断被中断、结果标记不完整，会诱发重复的解析/高亮请求。
- **LSP 未就绪**：`server 'CangJie' for mapping IntelliJ language 'Language: CangJie' not available` ×7（噪声，不是主因）。
- **调试参数**：`-Didea.is.internal=true`、`-Didea.debug.mode=true`、`-XX:-OmitStackTraceInFastThrow`
  全部处于开启状态，断言/栈抓取路径常开。
- **堆上限与机器**：16GB 物理内存、`-Xmx6144m` IDE + Gradle daemon（`-Xmx3g`）+ Edge 家族进程，
  余量很小；`-XX:+ExitOnOutOfMemoryError` 又让 IDE 一旦 OOM 直接退出，而不是降级降速。

## 4. 因果链

```
每个分析会话 configure() 提交一次"整模块 pre-macro raw CFIR 重建"（无单飞、无缓存）
        ↓  ×19 并发
每次重建第一步读 featuresDirective → getNode() → 强制加载/懒解析整棵 AST
        ↓  串行化在 PsiFileImpl 监视器上
解析器被无条件 setDebugMode(true) → 每个 mark() new Throwable（抓完整栈）
        ↓
解析 CPU 与分配量成倍放大 + 19 份 CFIR/AST 同时存活
        ↓
6GB 堆 3 分钟内耗尽（free 28MB）→ GC 占比 100%（累计 87s）
        ↓
长读动作 + GC 停顿 → 后台写动作饥饿（>10s）→ EDT 写意向许可 42s 拿不到
        ↓
UI 冻结 8~42s；昨天两次 8.9GB 堆转储即 OOM 崩溃
```

一句话：**卡顿不是单一 UI 线程问题，而是"每会话一次、无去重的整模块 CFIR 重建"把解析
（且是 debug 模式解析）放大 19 倍，撑爆 6GB 堆后 GC 抖动，最终把 EDT 饿死。**

## 5. 建议（按收益/成本排序）

### P0-1 关闭解析器 debug 模式（改动最小、收益最大）

`CangJieParser.kt:156` / `:201` 的 `setDebugMode(true)` 改为受开关控制，默认关闭，仅在
`-Dcangjie.parser.debug=true` 时开启。预计解析吞吐提升数倍，分配率与 GC 压力同步下降。

```kotlin
private val PARSER_DEBUG = java.lang.Boolean.getBoolean("cangjie.parser.debug")
...
if (PARSER_DEBUG) psiBuilder.setDebugMode(true)
```

### P0-2 宏快照构造做"单飞 + 缓存"

- `LLCfirMacroSessionConfigurator.configure()`（`intellij-ide/modules/ide/macro/.../LLCfirMacroSessionConfigurator.kt:69`）
  对同一 `session/caModule + revision` 只允许一个 in-flight 任务，其余请求复用结果或直接跳过；
  现在每个 session 都 `submit` 一个整模块构建，是 19 路并发的来源。
- `buildPreMacroRawBuildResultForModule()` 里 `buildRawCfirFileUncached` 改为按
  `VirtualFile + psiModificationCount` 缓存（或至少复用 `LLCfirFileBuilder` 的 cached 版本），
  避免每次重建都把 13 个文件重算一遍。

### P1-1 别让 `featuresDirective` 触发整树解析

`PsiRawCfirBuilder.kt:379` 为了取一个 directive 调 `file.featuresDirective`，
其实现（`CjFile.kt:343`）走 `getNode()`，会 `loadTreeElement` → `reconcilePsi` →
`calcStubbedDescendants` 把所有 lazy block 解析出来。建议：
- 从 lexer/stub 直接读 directive，或
- 只解析文件头（首个声明之前）区域，或
- 把 CFIR 构建放到 AST 已就绪的时机（例如索引/AST 缓存之后）。

### P1-2 去掉 `macroExpressionHeadSyntax()` 里的全文拷贝

`PsiRawCfirBuilder.kt:4286` 调用 `text`（`CompositeElement.getText` → `AstBufferUtil.toBuffer`）
会复制整个宏表达式 AST 文本；它作为默认参数在每个宏注解上被重复求值。
改为按 `startOffset..endOffset` 从文件字符序列切片。

### P2-1 运行参数

- `-Xmx6144m` 在 16GB 机器上叠加 Gradle daemon 过于激进；建议 4096m 起步，
  并加 `-XX:SoftRefLRUPolicyMSPerMB=50`，保留 `-XX:+ExitOnOutOfMemoryError`。
- `-Didea.is.internal=true` / `-Didea.debug.mode=true` 只在需要内部功能时开启（例如通过
  `runIde -PdebugIde` 之类的 profile），不要进默认 `getIdeJvmArgs()`。

### P2-2 索引重建去抖

`CjProjectsServiceImpl` 的 "Stub index rebuild requested after workspace initialization"
应只在工程模型/插件版本变化时触发，避免每次启动都全量重建。

### P3 磁盘清理

`intellij-ide/build/dumpTmp/` 下两个 8.9GB 堆转储（17.8GB）确认无用后删除；
如需继续取证，建议先 `jhat`/`jcmd` 离线分析再删。

## 6. 附：`Stub count (21811) doesn't match stubbed node length (22323)`

### 6.1 断言含义

`FileTrees.reconcilePsi` 在给"已有 stub 树"的文件构建 AST 时，会遍历 AST 的 stub 节点（stubbed spine），
并断言 **stub 树节点数 == AST 解析出的 stub 节点数**。两者不等即抛
`AssertionError: Stub count (N) doesn't match stubbed node length (M)`。

出现过的两组数值：

| 日志 | 次数 | 差 |
|---|---|---|
| `618` vs `725` | 14（10-21 早段） | 107 |
| `21811` vs `22323` | 4（10-21 17:19，10-22 12:45/46 起） | 512 |

### 6.2 触发链（10-21 17:19 完整栈）

```
CjTypeStatement.primaryConstructor (CjTypeStatement.kt:166-167)
  -> getStubOrPsiChild(PRIMARY_CONSTRUCTOR) 取不到
  -> PsiTreeUtil.findChildrenOfType(body, CjPrimaryConstructor::class.java)   // 兜底走 AST
  -> ASTDelegatePsiElement.getLastChild
  -> StubBasedPsiElementBase.getNode
  -> PsiFileImpl.calcTreeElement -> loadTreeElement -> FileTrees.withAst -> reconcilePsi
  -> AssertionError: Stub count (21811) != stubbed node length (22323)
```

上层调用方是库符号反序列化：

```
StubBasedClassDeserializationKt.deserializeClassToSymbol(StubBasedClassDeserialization.kt:165)
  -> LLCangJieStubBasedLibrarySymbolProvider.deserializeClass(...)
  -> ... -> LJCfirModuleLazyDeclarationResolver -> LLCfirDiagnosticVisitor
  -> SEVERE: CangJieIllegalArgumentExceptionWithAttachments: Error while resolving CfirClassImpl
            from IMPORTS to IMPLICIT_TYPES（诊断收集中断，结果不完整）
```

### 6.3 根因：两套独立的 stub 产生者

`CjDecompiledFile`（[CjDecompiledFile.kt:30](intellij-ide/../analysis/decompiled/decompiler-to-psi/src/org/cangnova/cangjie/analysis/decompiled/psi/file/CjDecompiledFile.kt)）：

- **stub 树**：`customStubBuilder = CompiledStubBuilder` → `StubTreeLoader.readOrBuild` →
  `CallableCjoStubBuilder` 直接从 `.cjo` 的 CFIR 构造（不经过 AST）。
- **PSI 文本**：`buildDecompiledText(compiledFileStub)` 渲染出来的"反编译源码"。
- **AST**：一旦有人 `getNode()`，平台会用 `CangJieParser.parse` **重新解析这段渲染文本**。

于是同一个文件有两套互相独立的 stub 产生者（`.cjo` 反序列化 vs 文本重解析），
平台却要求它们逐节点一致。`primaryConstructor` 的 `PsiTreeUtil` 兜底正好把库 PSI 拉进 AST，
于是差异暴露成断言。

`body?.getStubOrPsiChild(PRIMARY_CONSTRUCTOR)` 落空的原因在 cjo 侧：`createConstructorStub`
按 `declaration.isPrimary` 决定建 `PRIMARY_CONSTRUCTOR` 还是 `SECONDARY_CONSTRUCTOR`
（[CallableCjoStubBuilder.kt:363-400](analysis/decompiled/decompiler-to-stubs/src/org/cangnova/cangjie/analysis/decompiler/stub/CallableCjoStubBuilder.kt)），
渲染文本却统一渲染成主构造函数形态；两者判定/子节点不同时，stub 侧查不到 → 兜底 → 断言。

### 6.4 与卡顿的关系

- **不是** 42 秒冻结的直接原因（那是堆耗尽 + GC + 19 路整模块重建）。
- 但它是**同一类错误的另一个出口**：把 stub-based 库 PSI 当 AST 用。每次触发都会：
  1. 对 2 万+ stub 节点的反编译文件做整文件 AST 重建；
  2. 抛异常中断 `LLCfirDiagnosticVisitor`，诊断不完整 → 上层重试 → 再来一轮；
  3. 增加堆压力（AST + 临时节点），与 §3 的内存问题叠加。
- 时间相关性：10-21 17:19:02 断言后 54 秒出现 54s 冻结；10-22 的 3 次断言紧邻 12:45 起的连续冻结。

### 6.5 修复建议（在 §5 之外单独列出）

| 优先级 | 改动 | 位置 |
|---|---|---|
| P0 | `primaryConstructor` 改为复用已有的 stub-first 访问器 `body.primaryConstructors`，**删除 `PsiTreeUtil` 兜底**；stub 缺失时返回 null，而不是把库 PSI 拉成 AST | [CjTypeStatement.kt:165-167](psi/src/org/cangnova/cangjie/psi/CjTypeStatement.kt) + [CjAbstractClassBody.kt:122](psi/src/org/cangnova/cangjie/psi/CjAbstractClassBody.kt) |
| P0 | `StubBasedClassDeserialization` 全链路禁止 `getNode()`：`primaryConstructor`、`body?.declarations`、`getSuperTypeList` 都只能走 stub 访问器；加"库 PSI 禁止 AST"的测试守卫 | [StubBasedClassDeserialization.kt:165-183](analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/stubBased/deserialization/StubBasedClassDeserialization.kt) |
| P1 | 对齐 cjo stub builder 与渲染文本：`isPrimary` 判定、`VALUE_PARAMETER`/`TYPE_REFERENCE`/annotations 子 stub 的生成必须与 `CangJieParser` 解析结果逐类型一致；补"stub 树 vs 解析 AST 的 stub 类型直方图"对比测试（覆盖 std 库 cjo） | `CallableCjoStubBuilder` / `ClassCjoStubBuilder` / `cjoStubBuilding.kt` |
| P1 | `CjFileElementType.getStubVersion()` 当前返回常量 `212`，而 `CangJieStubVersions` 注释已记录到 `217`；stub 布局变化未带版本号会让旧 stub 树继续复用。建议把版本与插件 build 号/格式变更绑定 | [CjFileElementType.kt:60](psi/src/org/cangnova/cangjie/psi/stubs/elements/CjFileElementType.kt) + [CangJieStubVersions.kt](psi/src/org/cangnova/cangjie/psi/stubs/CangJieStubVersions.kt) |
| P2 | 修复后清一次沙箱 `system_runIde/index` 与 `caches`，排除旧 stub 树干扰 | — |

### 6.6 定位差异节点的方法

写一个 `BasePlatformTestCase`/LightPlatformTestCase：取触发问题的 `.cjo`，
分别取 `file.stub` 的 `getPlainList()` 与 `(file.node as FileElement).stubbedSpine`，
按 `elementType` 计数做直方图差集，差集里出现最多的类型就是 cjo builder 漏建/多建的类型，
按它补 `create*Stub` 或调整渲染文本即可，无需再猜。

## 7. 实测：`std.core.cjo` 的 stub 类型直方图差异

### 7.1 方法与结论

临时测试（片段保留在 `.scratch/idea-lag/StubShapeHistogramDiffTest.kt.snippet`）对**已安装 SDK**
（`cangjie-sdk-windows-x64-1.1.3`）的 `std.core` 执行：

1. `CjoFileStubBuilder.buildFileStub` 构建 binary stub 树；
2. `buildDecompiledText` 渲染文本 → `PsiFileFactory.createFileFromText` 重新解析 → `calcStubTree()`；
3. 按 `stubType`（以及 `parentType>stubType`）做直方图差集。

结果：`binaryTotal=21810`、`sourceTotal=22322`，**差 +512**，与 IDE 日志的 `21811 / 22323`
只差 1（文件根节点计数口径不同），即**同一份文件、同一个差异**。

### 7.2 类型层面差异（source − binary）

| stub 类型 | binary | source | delta |
|---|---:|---:|---:|
| ANNOTATIONS | 1484 | 1955 | **+471** |
| MODIFIER_LIST | 1488 | 1958 | **+470** |
| USER_TYPE | 3493 | 3400 | −93 |
| TYPE_REFERENCE | 3540 | 3451 | −89 |
| REFERENCE_EXPRESSION | 3688 | 3600 | −88 |
| BASIC_TYPE | 1621 | 1574 | −47 |
| VALUE_PARAMETER | 1097 | 1062 | −35 |
| TYPE_ARGUMENT_LIST | 447 | 415 | −32 |
| TYPE_PROJECTION | 447 | 415 | −32 |
| FUNC | 878 | 883 | +5 |
| VARIABLE | 78 | 83 | +5 |
| BINDING_PATTERN | 78 | 83 | +5 |
| SECONDARY_CONSTRUCTOR | 103 | 96 | −7 |
| FIELD | 146 | 141 | −5 |
| SUPER_TYPE_ENTRY | 152 | 149 | −3 |
| CLASS_BODY | 170 | 168 | −2 |
| PRIMARY_CONSTRUCTOR / PROPERTY / PROPERTY_ACCESSOR / PROPERTY_BODY | 4 / 55 / 56 / 54 | 3 / 54 / 55 / 53 | 各 −1 |
| TYPE_PARAMETER / TYPE_PARAMETER_LIST | 110 / 99 | 111 / 100 | 各 +1 |

父节点视角的关键条目：

```
FILE>ANNOTATIONS          binary=0    source=458   delta=+458
FILE>MODIFIER_LIST        binary=0    source=458   delta=+458
FILE>FUNC                 binary=253  source=334   delta=+81
CLASS_BODY>FUNC           binary=575  source=499   delta=-76
TYPE_REFERENCE>BASIC_TYPE binary=1621 source=1574  delta=-47
```

### 7.3 根因：渲染文本里有 36 个非法的 `<error>` 类型占位

重新解析渲染文本后产生了 **557 个 `PsiErrorElement`**，第一个出现在 offset 9032：

```
struct String <: std.core.Collection<<error>> & std.core.Comparable<std.core.String> & ...
func get(index: Int64): std.core.Option<<error>> { ... }
func toArray(): std.core.Array<<error>> { ... }
func indexOf(b: <error>, fromIndex: Int64): std.core.Option<Int64> { ... }
```

`<error>` 是**字面写入渲染文本的**（全文 36 处），不是日志标注。
它来自 `TypeCjoStubBuilder`：

```kotlin
else -> createBasicTypeReferenceStub(parent, coneType.toString())   // TypeCjoStubBuilder.kt:171
...
if (normalizedTypeName !in BASIC_TYPE_NAMES) {
    createRenderedUserTypeReferenceStub(parent, normalizedTypeName) // :188-190
}
```

当反序列化得到 `ConeErrorType` 时，`toString()` 就是 `<error>`，它不是合法类型名，
于是被当成"用户类型"写进 stub，渲染器再原样输出 → 语法错误。

### 7.4 差异的因果链

```
cjo 反序列化在该文件里产生 36 个 ConeErrorType（应为 UInt8 等具体类型）
        ↓
TypeCjoStubBuilder 把 "<error>" 当成用户类型写进 stub
        ↓
渲染文本出现 std.core.Collection<&lt;error&gt;> / Option<&lt;error&gt;> / Array<&lt;error&gt;> 这类非法写法
        ↓
重新解析产生 557 个 PSI 错误（struct/class body 提前闭合）
        ↓
458 个声明被"甩"到文件级：FILE>ANNOTATIONS / FILE>MODIFIER_LIST 各 +458
同时丢失 CLASS_BODY>FUNC(-76)、TYPE_REFERENCE(-89)、BASIC_TYPE(-47)、VALUE_PARAMETER(-35)、SECONDARY_CONSTRUCTOR(-7)…
        ↓
FileTrees.reconcilePsi 断言 Stub count != stubbed node length
```

因此：**不是 stub 版本或索引陈旧**（`std.core.cjo` 已于 10-19 20:13 因 `OUT_DATED` 重建过），
而是**同一份 cjo 的两条产出路径不收敛**，触发点是 `ConeErrorType` 被渲染成非法文本。

### 7.5 修复建议

| 优先级 | 改动 | 位置 |
|---|---|---|
| P0 | `TypeCjoStubBuilder` 显式处理 `ConeErrorType`：不要输出 `<error>`；要么跳过该类型实参（保持 `<>`→ 改为不渲染实参列表），要么渲染一个合法占位（如 `std.core.Any`）并记录诊断。渲染文本必须**永远是可解析的** | `TypeCjoStubBuilder.kt:171`、`:188` |
| P0 | 查清这 36 个类型为何反序列化成 `ConeErrorType`（示例位置是 `UInt8`/`Rune` 等基本类型，`CfirTypeDeserializer` 的 `TypeKind` 映射需逐项核对） | `cfir-serialization/.../CfirTypeDeserializer.kt` |
| P1 | 渲染器加护栏：任何类型文本含 `<error>` 或非标识符字符时，降级为合法占位，避免一条坏类型毁掉整个文件的可解析性 | `decompiledTextBuilder.kt` / `CjTypeReference.getTypeText()` |
| P1 | 修好后 bump `CjFileElementType.getStubVersion()`（当前 212），强制丢弃受影响的 stub 索引 | `CjFileElementType.kt:60` |
| P2 | 把 `CompiledStubShapeTest` 扩展到**已安装 SDK** 的 `std.core`/`std.ast`，作为回归门禁（现在只覆盖 fixture） | `compiler-to-psi/test/.../CompiledStubShapeTest.kt` |

### 7.6 另一处 618/725

10-21 上午的 `618 vs 725`（+107）出现在 `CfirFileImpl RAW_CFIR → IMPLICIT_TYPES` 路径，
差异量级与 `std.core` 同源（同一套渲染/反序列化逻辑），只是文件更小；修复 P0 后应一并消失。

## 9. 与 Kotlin K2 的对照（`<error>` 处理）

### 9.1 Kotlin 的做法（`external/kotlin`）

| 关注点 | Kotlin | 位置 |
|---|---|---|
| 占位 ClassId | 有，且与仓颉完全同款：`delegatedType?.classId ?: ClassId.fromString("<error>")` | `compiler/fir/cones/.../types/ConeTypes.kt:81-99` |
| 渲染入口 | `ConeTypeRenderer.renderConstructor` **先判** `is ConeClassLikeErrorLookupTag` → `ERROR CLASS: <diagnostic>`，再判普通 `ConeClassLikeLookupTag` | `compiler/fir/cones/.../renderer/ConeTypeRenderer.kt:207-245`（error 分支在 :232） |
| class-like 渲染 | `renderSimpleType` 调 `renderConstructor(type.getConstructor())`，所以带 error tag 的 class-like 也走 error 分支 | 同文件 :200-213 |
| delegatedType | 诊断渲染优先用 delegatedType；未推断类型参数额外加 `uninferred ` 前缀 | `compiler/fir/diagnostic-renderers/.../FirAdaptiveTypeRenderingKey.kt:83-124` |
| 是否回灌源码 | **否**。所有 Cone 渲染器只服务诊断与 dump（`FirErrors` / `FirRenderer` / `*ForDebug*`），Kotlin 的二进制 PSI 来自 stub/light class，不存在“渲染文本再解析回 stub”的流程 | — |

结论：**`<error>` 在 Kotlin 里只是内部占位 ClassId，永远不会出现在任何文本里**；一旦要输出，必然命中
`ConeClassLikeErrorLookupTag` 分支，输出 `ERROR CLASS: <diagnostic>`。

### 9.2 本仓库的两处偏差

1. **cjo stub 构建把 `ConeErrorType` 当普通 classifier**（本次 36 处 `<error>` 的直接来源）：

```kotlin
// TypeCjoStubBuilder.kt:137-155
when (coneType) {
    is ConePrimitiveType -> ...
    is ConeTypeParameterType -> ...
    is ConeClassifierType -> {                 // ← ConeErrorType 也是 ConeClassifierType，被这里吃掉
        val classId = coneType.lookupTag.classId   // = ClassId("<error>")
        createQualifiedUserTypeReferenceStub(..., segments = classId...)  // → 用户类型名 "<error>"
    }
    ...
}
```

Kotlin 里 `ConeErrorType` 是**独立分支**（与 `ConeClassLikeType` 同级，不是其子类），并且渲染永远先看 lookupTag。

2. **`renderClassLikeType` 绕过了 `renderConstructor`**：

```kotlin
// ConeTypeRenderer.kt:246-252
protected open fun renderClassLikeType(type: ConeClassLikeType) {
    idRenderer.renderClassId(type.classId)   // 直接打印 classId；带 error tag 时就是 "<error>"
    ...
}
```

本仓库 `renderConstructor` 其实已经有 Kotlin 同款分支（`ConeTypeRenderer.kt:84-86` 输出 `ERROR CLASS:`），
但 `renderType:144` 的 `is ConeClassLikeType` 分支先命中，永远不会走到 `renderConstructor` 的 error 判断。

### 9.3 对齐 Kotlin 的修法

```kotlin
// 1) TypeCjoStubBuilder.createTypeReferenceStub：在 is ConeClassifierType 之前插入
is ConeErrorType -> {
    val delegated = coneType.delegatedType
    if (delegated != null) {
        createTypeReferenceStub(parent, delegated)          // 与 ConeTypes.kt:98 的 classId 兜底同义
    } else {
        createBasicTypeReferenceStub(parent, ERROR_PLACEHOLDER)  // 合法占位，例如 "std.core.Any"
    }
}

// 2) ConeTypeRenderer.renderClassLikeType：与 Kotlin renderSimpleType 一致
protected open fun renderClassLikeType(type: ConeClassLikeType) {
    renderConstructor(type.lookupTag)        // error tag → "ERROR CLASS: ..."；普通 tag → classId
    renderTypeArguments(type.typeArguments)
    ...
}
```

注意：`ERROR CLASS: ...` 只能用于诊断输出，**不能**写进会被重新解析的源码文本；反编译文本要用合法占位。

## 11. 修复实施记录（2026-10-22）

### 11.1 改动

| 文件 | 改动 |
|---|---|
| [TypeCjoStubBuilder.kt](../../analysis/decompiled/decompiler-to-stubs/src/org/cangnova/cangjie/analysis/decompiler/stub/TypeCjoStubBuilder.kt) | `createTypeReferenceStub(ConeCangJieType)` 在 `is ConeClassifierType` **之前**新增 `is ConeErrorType` 分支：有 `delegatedType` 时渲染委托类型，否则退化为 `std.core.Any`；新增 `createErrorTypeReferenceStub` / `createAnyTypeReferenceStub`；`createRenderedUserTypeReferenceStub` 增加 `<error>` 兜底 |
| [ConeTypeRenderer.kt](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/render/ConeTypeRenderer.kt) | `renderClassLikeType` 改为 `renderConstructor(type.lookupTag)`，对齐 Kotlin `renderSimpleType`，使带 error lookup tag 的 class-like 输出 `ERROR CLASS: ...` 而不是占位 ClassId |
| [CangJieStubVersions.kt](../../psi/src/org/cangnova/cangjie/psi/stubs/CangJieStubVersions.kt) | `BUILTIN_STUB_VERSION` 217 → 218（`BINARY + 6`），强制重建 `.cjo` 二进制 stub 索引 |

`<error>` 占位 ClassId 本身保持不变 —— 与 Kotlin `ConeTypes.kt:98` 一致。

### 11.2 验证

对已安装 SDK 的 `std.core.cjo` 做 binary stub ⇄ 反编译文本重建 stub 的全量对比：

| 指标 | 修复前 | 修复后 |
|---|---|---|
| binaryTotal / sourceTotal | 21810 / 22322 | **21954 / 21954** |
| 类型级差异条目 | 23 种 | **0** |
| 路径级 shape 差异 | — | **无**（逐节点相等） |
| 反编译文本中的 `<error>` | 36 处 | **0** |
| 重解析 `PsiErrorElement` | 557 个 | **0** |

测试执行：

- `:analysis:decompiled:decompiler-to-psi:test` ✅
- `:analysis:decompiled:decompiler-to-stubs:test` ✅
- `:analysis:decompiled:decompiler-to-psi` 的 `CompiledStubShapeTest`（fixture 全量 shape）✅
- `:cfir:cfir-cones:test` ✅

诊断用临时测试已移除，片段保留在 `.scratch/idea-lag/StubShapeHistogramDiffTest.kt.snippet`。

### 11.3 副作用与后续

- 首次打开工程会因 stub 版本升级重建 `.cjo` 索引（一次性成本）。
- 待办（未在本次改动内）：`CjTypeStatement.primaryConstructor` 仍保留 `PsiTreeUtil` AST 兜底；它是把库 PSI 拉成 AST 的触发点，建议改为复用 `body.primaryConstructors`（stub-first），彻底消除 `reconcilePsi` 的触发面。
- 待办：把 `CompiledStubShapeTest` 扩展到真实 SDK 的 `std.core`/`std.ast` 作为回归门禁（当前只覆盖 fixture，而 fixture 不含 error type，覆盖不到本次分支）。

## 12. 修复后的验证口径

1. `idea.log` 中 `UI was frozen for` 消失或 < 1s；`High memory usage` 不再出现。
2. OTel：`LowMemory.gcLoad` ≈ 0，`JVM.GC.collectionTimesMs` 增长显著放缓。
3. 12:50 那次 42s 冻结对应的线程形态不再出现：`ApplicationImpl pooled thread` 不再批量 `BLOCKED`
   在 `PsiFileImpl.calcTreeElement`。
4. 启动后 10 分钟仍 `free > 1500 MB`（当前 3 分钟即 28MB）。
