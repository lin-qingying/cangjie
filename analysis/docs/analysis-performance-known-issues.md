# Analysis API 性能监控：已知问题清单

配套文档：[`analysis-performance-statistics.md`](analysis-performance-statistics.md)（功能说明与使用方式）。

本文只记录**审计中确认的缺陷**，不记录设计取舍。每条给出证据、影响和修复方向，便于排期。

## 0. 审计范围与前提

**当前作用域：上报与报表产出只发生在 `:analysis:analysis-performance-test` 模块内。**

IDE / LSP 宿主侧的上报（`CangJieGlobalOpenTelemetryProvider` → `GlobalOpenTelemetry`）目前没有可运行出口，
不在本轮修复范围内，见 §4「已知但不在范围内」。

审计方式：

1. 通读 `analysis/low-level-api-cfir/.../statistics/` 全部 12 个主文件 + 11 个统计域 + 全部埋点接缝；
2. 通读 `analysis/analysis-performance-test/` 全部测试与 testFixtures；
3. 实跑 `.\gradlew-queue.bat :analysis:analysis-performance-test:test`（16 用例 / 8 类 / 0 失败 / 9m29s）；
4. 对本机已在运行的 otel collector + Prometheus 做**实测查询**验证（下文标「实测」的结论均来自真实查询输出）。

---

## 1. 上报链路

### P1【高】指标侧没有 resource，`service.name` 丢失

`analysis/analysis-performance-test/testFixtures/.../CaPerformanceTestTelemetry.kt:76-89` 只给 **TracerProvider** 设了
`Resource`，`SdkMeterProvider.builder()` 没有 `.setResource(...)`。

**实测**（collector `:9464/metrics`）：

```
cangjie_analysis_resolve_phases_types_runs_total{
  exported_job="unknown_service:java",   # ← 指标侧退化
  job="cangjie-analysis",
  otel_scope_name="cangjie.analysis.resolve", ...} 10
```

span 侧是 `cangjie-analysis-performance-test`，指标侧退化为 `unknown_service:java`。
叠加 `tools/otel/otel-collector-config.yaml:42-43` 的 `prometheus` exporter 未开
`resource_to_telemetry_conversion`，**多个来源 / 多次运行在同一个采集栈里无法区分，且不报错**。

**修复**：把 `Resource` 抽成一个 `val` 同时喂给两个 provider；collector 侧补
`resource_to_telemetry_conversion: {enabled: true}`。

---

### P2【高】全链路零 flush / 零 shutdown，尾部数据必丢

全仓 `forceFlush` / `shutdown()` / `addShutdownHook` 在遥测代码中**零命中**。
`LLStatisticsService.dispose()`（`LLStatisticsService.kt:146-152`）只 `scheduler.cancel()`。

`PeriodicMetricReader`（5s）与 `BatchSpanProcessor`（SDK 默认 5s）都用 daemon 线程且不注册 shutdown hook。

后果：

- 文档 §4「跑单个用例」这种 <5s 的运行，**一个数据点都推不出去**，测试仍然全绿；
- 每次运行的最后 <5s 窗口永久丢失。

> 与上游 `external/kotlin` 一致（Kotlin 的 `dispose()` 同样只 cancel）。偏离点在本仓自建的
> `CaPerformanceTestTelemetry`：它自己**持有** `SdkMeterProvider` / `SdkTracerProvider`，有责任 flush。

**修复**：testFixtures 里注册 shutdown hook（或基类 teardown）调用 `forceFlush()` + `shutdown()`。

---

### P3【高】短命测试进程 + cumulative ⇒ 文档给的 PromQL 实测两条都不可用

**实测**（查询本机 Prometheus）：

| 文档中的查询 | 实测结果 |
|---|---|
| `sum by (__name__) (rate({__name__=~"cangjie_analysis_resolve_phases_.*_runs_total"}[5m]))`<br>`analysis-performance-statistics.md:196` | **HTTP 422，PromQL 解析错误**（`__name__` 不允许出现在聚合的 grouping labels 中） |
| `histogram_quantile(0.95, sum by (le) (rate(..._duration_milliseconds_bucket[5m])))`<br>`analysis-performance-statistics.md:192` | 返回 **NaN** |
| `sum(increase(..._duration_milliseconds_sum[30m]))` | **0** |

数据本身在 Prometheus 里是有的（`..._types_runs_total = 10`），但测试进程退出后不再有新样本，
`rate()` / `increase()` 全为 0。

**结论**：文档 §5「Prometheus 里一律用 `rate()` / `increase()` 看变化」对「跑完测试就结束」的流程**拿不到任何结果**；
要看趋势必须有一个常驻进程。

**修复**：文档补一节「短命进程下的读法」，或（更彻底，见 §3 讨论）让报表落在**文件**里而不是 Prometheus。

---

### P4【高】rawBuild 的 body 策略维度没进指标名，两种模式被合并

`LLRawBuildStatistics.kt:71-80` 的 `runKeys` 含 `bodyBuildingMode`，
但 `:99-106` 建计数器时名字只用 `LLStatisticsScopes.RawBuild.runs(source, stage)` ——
每个 `(source, CONVERT)` 因此注册了**两个同名计数器**（description 不同）。

**实测**：Prometheus HELP 只剩一条

```
# HELP cangjie_analysis_rawBuild_psi_convert_runs_total
      Number of convert executions from psi (bodyBuildingMode=LAZY_BODIES)
```

即 KDoc 承诺的「按 body 构建策略拆桶」在看板里完全不可见，`NORMAL` 的计数被并进 `LAZY_BODIES` 那条序列。

**修复**：把 `bodyBuildingMode` 加入指标名（`.runs.<mode>`）。会改指标名，
`LLStatisticsScopesTest` 与 `LLStatisticsCoverageManifestTest` 需同步。

---

### P5【中】耗时直方图整毫秒截断，亚毫秒样本塌成 0

全部记录点用 `elapsedNanos / 1_000_000L`（`NANOS_PER_MILLI`）。

**实测**：

```
cangjie_analysis_resolve_phases_cjmp_matching_duration_milliseconds_sum = 1   count = 5
cangjie_analysis_resolve_phases_types_duration_milliseconds_bucket{le="1"} = 8   count = 10
```

5 个样本总和 1ms —— 其中 4 个被记成 0。这与 `DURATION_BUCKETS_MS.kt:6-9` 的 KDoc
「覆盖**亚毫秒级**的单声明推进」自相矛盾，也是「按需解析里每次阶段推进都有一条」那条链路的主要失真源。

**修复**：细粒度耗时改用微秒/纳秒计量（`.setUnit("us")` + `/1000`），或把「阶段耗时」拆成
粗（毫秒，session/文件级）与细（微秒，声明级）两族直方图。

---

### P6【中】PSI 解析接缝不满足「零开销」承诺

`psi/src/org/cangnova/cangjie/parsing/CangjiePsiParseTimingService.kt:92-103` 的 `measure`
接收者**非空**，无条件执行 `TimeSource.Monotonic.markNow()`（一次堆分配 + 两次时钟读），
而该 service 被描述符**无条件注册**、`LLParserTimingService.delegate` 仍可能为 `null`。

文档 §1 与该类自己的 KDoc 都写着「**不取单调时钟**」。
其余 5 个接缝（resolve phase / raw build / macro / diagnostic pass / cjo 反序列化）
全部是可空接收者内联短路，只有 PSI 这一条不一致。

**修复**：给 `CangjiePsiParseTimingService` 加 `isRecording`，`measure` 内联体开头短路；
或按 registry key 条件注册。

---

### P7【中】同一个开关有两个真源

| 位置 | 读法 |
|---|---|
| `analysis/analysis-api-platform-interface/.../CaStatisticsService.kt:30-32` | `by lazy(PUBLICATION)`，**每个 JVM 只读一次后永久冻结** |
| `lsp/.../CangjieRequestStatistics.kt:149` | 每次直读 `Registry` |

同一个 key `cangjie.analysis.statistics`，两侧可以给出不同答案 ——
正是 `CangjieRequestStatistics` KDoc 说要避免的情况。
测试宿主又在运行时 `Registry.setValue("true")` 且不还原。

**修复**：LSP 侧改用 `CaStatisticsService.areStatisticsEnabled`（单一真源）。

---

### P8【中】OpenTelemetry 版本分裂 1.39.0 vs 1.48.0

`gradle/libs.versions.toml:20-21` 钉 `1.48.0`，并**明确注释**说 1.39.0 会导致 `AbstractMethodError`。
但 4 处仍硬编码 1.39.0：

| 文件 | 行 |
|---|---|
| `analysis/analysis-api-platform-interface/build.gradle.kts` | 26 |
| `analysis/low-level-api-cfir/build.gradle.kts` | 43 |
| `analysis/analysis-api-cfir/build.gradle.kts` | 60 |
| `dependencies/intellij-core/build.gradle.kts` | 37（`runtimeOnly` + `isTransitive=false`） |

而 `lsp` 与 `analysis-performance-test` 用的是 `libs.opentelemetry.api`（1.48.0）——
同一测试 classpath 上两个版本并存，目前靠 Gradle 取高版本侥幸没出事。

**修复**：4 处硬编码全换成 `libs.opentelemetry.api`。

---

### P9【低】其余确认项

| 编号 | 问题 | 位置 |
|---|---|---|
| P9.1 | 20ms 空转调度器：10 个统计域**无一覆写** `update()`，IDE/测试期间每秒空转 50 次；`start()` 的 KDoc 却写着 "These tasks contribute to the collected statistics" | `LLStatisticsScheduler.kt:26-30,54`、`LLStatisticsDomain.kt:16` |
| P9.2 | `LLCaffeineStatsCounter.snapshot()` 的注释把原因写成「OTel 取不到 stats」，实际是本实现刻意不累计；导致 `Caffeine.cache.stats()` 永远全 0，且 `recordLoadSuccess/Failure` 是空实现 → **缓存加载耗时永久不可观测** | `LLCaffeineStatsCounter.kt:46-68` |
| P9.3 | 三个不同的 Caffeine 缓存（函数/属性/宏）共用同一个 `combinedSymbolProviderCallableCacheStatsCounter` → 三类缓存合并成一条序列 | `LLCombinedCangJieSymbolProvider.kt:90,99,108` |
| P9.4 | span 名每次调用现算 + locale 敏感 `lowercase()`（指标名则在构造期预计算进 Map，两者不一致） | `LLStatisticsScopes.kt:219,409` |
| P9.5 | `LLSpanTracker` 的 `ThreadLocal.withInitial { ArrayDeque() }` 对「只 end 不 start」的线程也分配，池化线程不 `remove()` | `LLSpanTracker.kt:38,59` |
| P9.6 | 6 处注释引用了并不存在的 scope 名 `checkerPass`（真实名是 `structureElement`） | `LLStatisticsScopes.kt:336` 等 |

---

## 2. 报表

### R1【高】`analysis/README.md` 承诺的「报表产出」全仓不存在

`analysis/README.md:52`：「性能统计测试入口：指标断言、**报表产出**、OTLP 导出」。

一方树（排除 `external/`、`build/`、`.claude/`、`intellij-ide/`、`deveco/`）穷举搜索
`statistics-report | ReportWriter | PerfReport | Grafana | …`：**没有任何写性能报表的代码**。

实际存在的「产出」只有三种，都不是性能报表：

1. Gradle 默认的 JUnit HTML/XML（`build/reports/tests/test`）—— 是测试结果，不是性能；
2. Kover 覆盖率 —— 且 `:analysis:analysis-performance-test` 根本不在 `moduleThresholds` 里；
3. collector 的 JSONL（`tools/otel/otel-data/metrics.json`）—— 原始 OTLP JSON，需自行加工；
   且 `file` exporter 未配 `rotation`，会无上限增长。

佐证：worktree 里残留一份 `build/reports/analysis-performance/statistics-report.txt`，
但 `git log --all -S "statistics-report"` **无命中** —— 那是某次未提交的临时脚本产物，
指标清单也停留在早期子集（无 `macro.*`、无 `symbolProviders.*`）。

> 注意：规格文档 `analysis-performance-statistics.md:137-139`（「采集、聚合、看板都交给现成组件，
> **仓里不自己写渲染逻辑**」）与代码是一致的。**撒谎的是 README。**

---

### R2【中】文档指标名漂移

- 文档写 `analysisSessions.caches.*` —— **不存在 `caches` 段**（`Caches` 是普通 object，不贡献 name），
  真实是 `analysisSessions.resolveCallCache.*` 等；
- `resolve.phases.<phase>.{duration,runs,declarations}` 漏了 `files`；
- span 属性表写成 `resolve.files` / `resolve.declarations`，漏了全仓统一的 `cangjie.` 前缀。

**修复**：由 `LLStatisticsMetricNames` 生成文档表格，或加一条测试断言文档里的每个名字都能被它产生。

---

### R3【中】CLI 侧报表在真实 `cjc` 上不可用

`AbstractFrontendPipeline` 的子类只有测试里的 `ProbePipeline` 与 `CjmpArgumentProbePipeline`；
`compiler:cli` 没有 `fun main`。`--report-perf` / `--dump-perf` 只在测试里走通。

文档 §7 已承认，但 doc §3 仍把它列为一条通路，容易被当成可用能力。

---

## 3. analysis-performance-test 本地测试

> 实跑结论：16 用例 / 8 类 / **0 失败**。问题不是「跑不起来」，而是「测的不是它名字承诺的东西」。

### T1【高】没有一条性能断言

全仓**无 JMH / benchmark / 基线文件**。16 个用例全部只断言「计数器 / 直方图 / span 有没有被记录到」，
没有一条断言耗时、吞吐或与基线的偏差。类名 KDoc 自己写了：「这些用例不比较耗时」。

**它检测不出任何性能回归。** 模块名叫 performance-test，实际是 telemetry-wiring test。

---

### T2【高】一半用例是「自己驱动统计域」的自证测试

- `CaMacroConstructionStatisticsTest.kt:27-41`：`LLMacroConstructionStatistics(service)` 后手工调回调
- `CaSessionAndDeserializationStatisticsTest.kt:28-41`：同样 new 域、手工调

真实接缝一次都没被触碰 —— **接缝彻底断掉这两个用例照样绿**。

---

### T3【高】覆盖清单守卫自证，且已有 9 条假登记

`LLStatisticsCoverageManifestTest` 的 `verifiedBy` **只检查字符串非空**，
从不验证被引用的测试是否存在、是否真的断言了该指标。

已核实的假登记：

| 登记 | 实际情况 |
|---|---|
| 8 条 `SymbolProviders.Combined.*` → `CaAnalysisApiStatisticsTest` | 该类全文 151 行，**零** symbol provider 断言；这 4 个指标名全仓只出现在 `LLStatisticsScopesTest`（纯名字层） |
| `CangjiePsiParseKind.BLOCK_EXPRESSION` → `CaParserStatisticsTest` | 该类唯一用例写死 `CangjiePsiParseKind.FILE`（`:45`），`BLOCK_EXPRESSION` 从未触发 |

此外它以 `LLStatisticsScope` 对象为键，而绝大多数真实指标是**挂在已有 scope 上的后缀函数**
（`Resolve.Phases.files`、`Macro.Expansion.files/surfaces/outcome`、`Cjo.Declaration.declarations`），
新增这些守卫**不会红**；整个 `cangjie.lsp.request.*` 家族也完全不在范围内。

**文档「新增埋点忘记登记时它立刻变红」不成立。**

---

### T4【高】OTLP 导出零验证，配错了测试照样全绿

`build.gradle.kts:66-80` 的 `-D` 转发机制本身**是对的**（已验证 `providers.systemProperty` 读的是
Gradle 构建 JVM 的系统属性，`gradlew -D` 正是设置它；配置缓存也正确失效）。问题是：

- endpoint 只做 `takeIf { it.isNotBlank() }`，不校验格式、不设 timeout，失败只进 OTel 内部 logger；
- **没有任何测试断言「OTLP 导出器已按属性挂上」**。

worktree 的 build 目录里还留着一份已删除的 `TmpExporterProbeTest` 报告 ——
说明当初是用一次性探针手工验证的，之后删掉了。
文档 §5 的「三条链路均已实测通过」目前**没有任何自动化手段防止它腐化回假**。

---

### T5【中】测试隔离脆弱

- `CaStatisticsService.areStatisticsEnabled` 是 JVM 级 `by lazy`，而 `CaPerformanceTestServiceRegistrar`
  在运行时把**全局** Registry 置 `true` 且**不还原**（`:31`）。谁先读谁定终身。
- metric 侧只有 `resetSpans()`，**没有 metric reset**，所有用例必须自己写 before/after 增量。
  `counterDelta` 兜住了现有用例，但新增用例直接调 `counterValue()` 就会拿到跨用例污染的数字。
- `metricNamesUseCangjieNamespace` 断言「所有指标都必须在 `cangjie.analysis.` 下」，
  一旦 LSP 侧指标进同一 JVM 就会红。

---

### T6【中】默认跑一遍不产出任何可看的东西

不加 `-D` 时没有 OTLP exporter，也没有任何本地报告文件，输出只有 JUnit XML。
要看数据必须：起 docker 栈 + 传 `-D` + 手动查 Prometheus，而按 P3 那两条查询还查不出结果。

---

## 4. 已知但不在当前范围内

以下问题真实存在，但属于 IDE / LSP 宿主侧，**本轮（只在测试模块内上报与产出报表）不处理**，记录备查：

| 编号 | 问题 |
|---|---|
| N1 | `intellij-ide` / `deveco` 全仓**没有** `GlobalOpenTelemetry.set(...)` 或 `OpenTelemetrySdk.builder`。`CangJieGlobalOpenTelemetryProvider` 只会拿到 noop → 真实 IDE 里打开统计开关后，指标被**静默丢弃且不报错** |
| N2 | `CaFirStatisticsService`（`internal`）被 intellij-ide 的 XML 以 `serviceImplementation` 跨模块注册，靠 Kotlin `internal` 的字节码 public 特性生效 —— 能跑，但是模块边界上的妥协 |
| N3 | 文档引用的 `CaIdeStatisticsStartupActivity` 源码在 `intellij-ide`（独立构建），本仓库不可核实 |

---

## 5. 建议修复顺序

| 序 | 项 | 成本 |
|---|---|---|
| 1 | R1（改 README 一行） | 极低 |
| 2 | P1 + collector `resource_to_telemetry_conversion` | 两处配置 |
| 3 | P2（补 flush/shutdown） | 低 |
| 4 | P4（body 模式进指标名） | 中，需同步名称断言 |
| 5 | P5（细粒度耗时改微秒/纳秒） | 中 |
| 6 | P8（4 处硬编码版本） | 低 |
| 7 | P6 / P7（零开销一致性、开关单一真源） | 低 |
| 8 | T3 / T4（清单改按指标名对账；补「OTLP 导出器已配置」测试） | 中 |
| 9 | T1 / T2（若要真做性能回归，需基线文件或 JMH，并把自证用例换成真实接缝） | 高 |