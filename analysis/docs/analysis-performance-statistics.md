# Analysis API 性能统计使用说明

本文说明仓颉 Analysis API 性能统计的开启方式、指标清单、如何跑测试与看结果，以及每条通路
各自的可验证边界。目标是让"IDE 卡在哪里"这个问题有可复现的测量，而不是靠猜。

> 本文描述的是**设计意图**。审计中确认的缺陷、实测证据与修复方向见
> [`analysis-performance-known-issues.md`](analysis-performance-known-issues.md)。

## 1. 开启统计

统计只有一个开关，Analysis 侧与 LSP 侧共用：

```
cangjie.analysis.statistics
```

声明在 `analysis/analysis-api-platform-interface/resources/META-INF/analysis-api/cangjie-analysis-api-platform-interface.xml`，
默认 `false`，`restartRequired=true`。

**在测试里**：`CaPerformanceTestServiceRegistrar` 直接把它置 `true`，等价于用户在 IDE 里改了这个
值，测试宿主不需要额外配置。

**在 IDE 里**：在注册表里把该键改为 `true` 并重启工程。IDE 侧由 `CaIdeStatisticsStartupActivity`
在工程打开后启动统计服务。

开关关闭时，各通路的观察者不会被注册，采样路径零开销：接口在 session 上以可空形式查找，为
`null` 就直接执行动作，不取单调时钟。

## 2. 指标后端

所有指标通过 OpenTelemetry 写出，命名空间是 `cangjie.analysis.*`（Analysis API）与
`cangjie.lsp.*`（LSP 服务）。两者共用同一个后端实例，因此一次导入就能在同一个看板里同时看到
"请求多慢"与"分析多慢"。

后端由工程级服务 `CangJieOpenTelemetryProvider` 提供，生产实现是
`CangJieGlobalOpenTelemetryProvider`，即取平台的 `GlobalOpenTelemetry.get()`。平台没有初始化
OpenTelemetry SDK 时它退化为 noop，统计调用不产生上报，也不抛错。

统计有两个信号，走同一个后端、去往不同的看板：

- **指标（metrics）** → Prometheus：聚合视图，回答"平均多慢、哪条路在变慢"；
- **链路（traces / span）** → Jaeger：单次调用视图，回答"这一次调用里时间花在哪"。

每个耗时接缝同时产出两者，名字可直接对照：span 名取对应指标名去掉指标后缀后的前缀
（如 `cangjie.analysis.resolve.phases.types`），维度信息放在 span 属性里（见第 3 节）。

## 3. 指标清单

### 分析侧 `cangjie.analysis.*`

| 指标 | 含义 |
|---|---|
| `analysisSessions.analyze.invocations` | 分析入口被调用的次数 |
| `analysisSessions.lowMemoryCacheCleanup.invocations` | 低内存缓存清理次数 |
| `analysisSessions.caches.*` | 三个解析缓存的命中/未命中/淘汰 |
| `resolve.phases.<phase>.{duration,runs,declarations}` | 语义解析各阶段耗时、次数、推进的声明数 |
| `rawBuild.<source>.<stage>.{duration,runs}` | raw CFIR 构建（PSI / LightTree × parse / convert） |
| `macro.<stage>.{duration,runs}` | 宏构造三阶段：符号索引、导入绑定、真实展开 |
| `macro.expansion.{files,surfaces,<outcome>}` | 展开规模与结果分类 |
| `diagnostics.collection.{duration,runs,diagnostics}` | 文件级诊断收集 |
| `diagnostics.elementCollection.*` | 元素级诊断收集 |
| `diagnostics.structureBuild.*` | 文件结构首次构建 |
| `diagnostics.structureElement.<set>.*` | 按 checker 集合的元素级收集 |
| `diagnostics.pass.<sema\|post_sema>.{duration,runs}` | 诊断遍历两段 |
| `sessionCreation.<kind>.{duration,runs}` | 按模块种类的 session 创建 |
| `scopes.sessionCreated` | scope session 创建次数 |
| `parse.<kind>.{duration,runs}` | PSI 解析，六个入口分桶 |
| `deserialization.classLike.{duration,runs}` | stub 反序列化 |
| `deserialization.cjo.<stage>.{duration,runs}` | `.cjo` 反序列化：包加载、按声明惰性 |
| `deserialization.cjo.declaration.declarations` | 惰性反序列化的声明总数 |
| `symbolProviders.combined.*` | 组合 symbol provider 缓存 |

### LSP 侧 `cangjie.lsp.*`

`cangjie.lsp.request.<method>.{duration,runs,failures}`，方法名的 `/` 换成 `.`。耗时不含排队
等待——排队属服务器背压，混进去会让补全变慢被误读成分析变慢。失败单独计数，因为"请求慢"与
"请求报错"对使用者的含义完全不同。

### 链路 span（traces）

每次真实调用同时产出一条 span。span 名与对应指标同名（去掉指标后缀），维度放在属性里；
下表省略 `cangjie.analysis.` 前缀（LSP 请求 span 为 `cangjie.lsp.*`）：

| span | 属性 | 说明 |
|---|---|---|
| `analysisSessions.analyze` | — | 分析会话根 span；一次分析调用一棵链路树的根 |
| `resolve.phases.<phase>` | `cangjie.resolve.phase`、`resolve.files`、`resolve.declarations` | 语义解析各阶段；按需解析里每次阶段推进都有一条 |
| `rawBuild.<source>.<stage>` | `cangjie.rawBuild.source/stage/bodyBuildingMode` | raw CFIR 构建 |
| `macro.<stage>` | `cangjie.macro.stage/mode/outcome/files/surfaces` | 宏构造三阶段 |
| `diagnostics.pass.<phase>` | `cangjie.diagnostics.phase` | 诊断遍历两段 |
| `diagnostics.collection` / `elementCollection` / `structureBuild` / `structureElement.<set>` | `cangjie.diagnostics.count` | 诊断收集四个维度 |
| `deserialization.classLike` / `deserialization.cjo.<stage>` | `cangjie.deserialization.stage/declarations` | 反序列化两条通道 |
| `parse.<kind>` | `cangjie.parse.kind/succeeded` | PSI 解析六个入口 |
| `sessionCreation.<kind>` | `cangjie.session.kind` | session 创建 |
| `cangjie.lsp.request.<method>` | `cangjie.lsp.method` | LSP 请求；分析侧的 span 会挂到它下面 |

同线程上的 span 按打开顺序自然嵌套（分析会话根 span 之下是阶段 span）；失败的操作同样产生
span：解析/遍历/收集以异常结束时 span 照常结束并记录已耗时长，LSP 请求以 ERROR 状态结束
并带异常事件。

### CLI 侧

编译器阶段耗时走 `PhaserProfiler`，不写 OpenTelemetry：

- `--report-perf`：报告写到 stderr
- `--dump-perf <path>`：报告写到文件

覆盖 CFIR 前端分析、`CjoWritePipelinePhase`（写出 CJO）、组合阶段协议。**注意**：目前全仓没有
`AbstractFrontendPipeline` 的生产子类，`compiler:cli` 下也没有 `fun main`，所以这两个开关
只在测试里走通，真实 `cjc` 上还跑不起来。

## 4. 跑测试

### 全部性能用例

```bash
./gradlew :analysis:analysis-performance-test:test
```

### 单个用例

```bash
./gradlew :analysis:analysis-performance-test:test --tests "*CaParserStatisticsTest*"
```

### 覆盖清单守卫

```bash
./gradlew :analysis:low-level-api-cfir:test --tests "*LLStatisticsCoverageManifestTest*"
```

这条守卫要求**每个埋点目标都在清单里登记负责验证它的用例**（`LLStatisticsCoverageManifestTest`）。
新增埋点忘记登记时它立刻变红，因此"到底埋了哪些点、各自由谁验证"有单一答案可查。

清单里区分两种情况：`reachableInAnalysisHost = true` 表示分析宿主真实路径能产生采样；
`false` 表示分析宿主上不触发（不跑宏构造、没有库模块、没有 IDE 补全路径），由该通路所属模块
自己的 seam 测试负责，且必须写明理由。

## 5. 看结果

指标是 OpenTelemetry 标准的 `LongCounter` / `LongHistogram`，链路是标准的 span，采集、聚合、
看板都交给现成组件，仓里不自己写渲染逻辑。

### 起本地采集栈

配置在 `tools/otel/`（collector 配置 + Prometheus 抓取配置 + docker compose）：

```bash
docker compose -f tools/otel/docker-compose.yml up -d
```

起三个容器：

- **OpenTelemetry Collector**：收 OTLP gRPC（`localhost:4317`）与 HTTP（`4318`），把指标翻成
  Prometheus 格式暴露在 `:9464/metrics`，把链路转给 Jaeger；同时把指标以 JSON 行落盘到
  容器内 `/tmp/otel/metrics.json`（离线与 CI 归档用），并打日志（出问题时先看 collector
  收到了什么）。
- **Prometheus**：按 5 秒间隔抓取 collector 的 `:9464/metrics`，前端 **http://localhost:9090**。
- **Jaeger all-in-one**：自带存储与链路 UI，前端 **http://localhost:16686**。

两个信号去向我们特意分开：**指标走 Prometheus，链路走 Jaeger**。此前曾把指标也发给 Jaeger，
collector 报 `unknown service ...MetricsService`——Jaeger 只实现 OTLP 的 trace 服务，不接
metrics，指标发给它注定失败。Prometheus 只认指标、Jaeger 只认链路，各归其位后两边都不需要"猜"。

> 状态说明：三条链路均已实测通过——测试进程 → collector 的接收、日志与落盘此前已用真实打点确认；
> collector → Prometheus 与 collector → Jaeger 已用 OTLP 合成探针实测，且真实性能测试推送的
> `cangjie_analysis_*` 指标与 `cangjie.analysis.*` span 已在 Prometheus / Jaeger 中确认到达。

### 把指标与链路推进去

```bash
./gradlew :analysis:analysis-performance-test:test \
    -Dcangjie.performance.otlp.endpoint=http://localhost:4317
```

指标与链路都会以 5 秒为周期推给 collector（链路在 span 结束时导出）。

这个属性名是 `cangjie.performance.otlp.endpoint`，`CaPerformanceTestTelemetry` 读它来决定是否
挂 OTLP 导出器。构建脚本已把它从 Gradle 命令行转发进测试 JVM：`Test` 任务另起 JVM，命令行上的
`-D` 只进 Gradle 自身进程，不转发就永远到不了测试进程，OTLP 导出这条路等于没接上。

Jaeger 里按 span 名（如 `cangjie.analysis.resolve.phases.types`）或属性过滤；一次 LSP 请求
是一棵完整的树（`cangjie.lsp.request.*` 为根，分析侧 span 挂在它下面）。

### PromQL 速查

指标名经过 Prometheus 翻译：`.` 换成 `_`，并追加类型/单位后缀——计数器加 `_total`，
直方图加 `_bucket` / `_sum` / `_count`，单位 `ms` 变成 `milliseconds`。所以
`cangjie.analysis.resolve.phases.types.duration`（毫秒直方图）在 Prometheus 里是
`cangjie_analysis_resolve_phases_types_duration_milliseconds_bucket`。以 `:9464/metrics`
的实际输出为准，下面是几条常用查询：

```promql
# TYPES 阶段耗时的 P95（毫秒）
histogram_quantile(0.95,
  sum by (le) (rate(cangjie_analysis_resolve_phases_types_duration_milliseconds_bucket[5m])))

# 各语义阶段的执行速率（次/秒）
sum by (__name__) (rate({__name__=~"cangjie_analysis_resolve_phases_.*_runs_total"}[5m]))

# 文件级诊断收集的 P95 耗时
histogram_quantile(0.95,
  sum by (le) (rate(cangjie_analysis_diagnostics_collection_duration_milliseconds_bucket[5m])))

# LSP 补全请求的 P95 耗时
histogram_quantile(0.95,
  sum by (le) (rate(cangjie_lsp_request_textDocument_completion_duration_milliseconds_bucket[5m])))
```

### 累计语义

指标按 JVM 累积（cumulative temporality），**跑得越久数值越大，不代表变慢了**。两种对比方式：

- Prometheus 里一律用 `rate()` / `increase()` 看变化，不要直接读累计值。
- 单元测试里取增量：测试基类的 `counterDelta {}` 与 `counterValue(name)` 就是干这个的。

### 进程内读取

测试宿主常驻一个 `InMemoryMetricReader`，用例直接读它（`CaPerformanceTestTelemetry` 的
`collectLongCounters` / `collectHistogramPointCounts` / `collectHistogramSums` /
`collectHistogramMaxima`）。这是断言用的通道，不需要起采集栈。

### 断言失败信息

每个用例的断言消息里带实际增量。断言失败时直接看消息即可知道差多少——这是最快的排查入口，
不用先起任何东西。

## 6. 各通路的可验证边界

**这一节是排查"为什么某个指标恒为 0"的依据。** 观察者接口的单测与统计域到指标名的映射测试都
绿，只证明两端存在，不证明接缝通了。PSI 解析就栽在这里——指标恒为 0，只有加诊断才发现没有
任何东西到达接缝。所以每加一个观察者都要配一个用真实 fixture、真实 session 走完整链路的
端到端用例，断言观察者回调而非查询结果。

已验证接缝的通路：

| 通路 | 验证方式 |
|---|---|
| PSI 解析 | `CaParserStatisticsTest` 走真实 `parsePsiFile`，断言采样 |
| `.cjo` 读侧 | `CjoDeserializationTimingSeamTest` 用真实 SDK fixture 加真实 provider，断言观察者回调 |
| `.cjo` 写侧 | `CjoWritePhaseProfilingTest` 真实编译，断言报告里有该阶段 |
| 组合阶段协议 | `FrontendPipelineCompositionTest` 断言子阶段经 phaser 协议执行 |
| LSP 请求级 | `CangjieRequestStatisticsSeamTest` 真实环境、真实 executor，断言耗时/次数/失败数，以及成功/失败请求的链路 span |
| 诊断、raw 构建、resolve phase、session 创建 | 性能用例走真实 `analyzeForTest` + `collectDiagnostics` 路径 |
| 宏构造 | `cfir/analysis-tests` 的 `MacroConstructionTimingObserverTest`，真实 construction |
| LightTree raw 构建 | `light-tree2cfir` 的 seam 测试，真实驱动 `buildCfirFile` |
| 链路 span（阶段与会话根） | `CaSpanTimingTest` 断言阶段 span 携带属性、且挂在分析会话根 span 之下 |
| 链路 span（PSI 解析） | `CaSpanTimingTest` 断言 `parse.file` span 与入口/成败属性 |

## 7. 已知边界

- **IDE 侧描述符无法在本仓库核实。** `intellij-ide` 是独立仓库。IDE 侧自维护
  `cangjie-ide-analysis-api-cfir.xml` 而不复用主仓库那份，新增 projectService 时两边都要加。
- **stub classLike 反序列化在分析宿主上不触发。** 它需要库模块，分析宿主的 testData 没有。
  IDE 打开含库工程时才会触发。
- **IDE 补全路径（lambda、code fragment 解析）在分析宿主上不触发。** 这些入口只在 IDE 补全时
  被调用，其覆盖目前停在指标名层。
- **CLI 阶段耗时在真实 `cjc` 上不可用**，原因见第 3 节。

## 8. 相关文档

- 测试约定：[TESTING_CONVENTIONS.md](../../TESTING_CONVENTIONS.md)
- Analysis 子系统：[analysis/README.md](../../analysis/README.md)
- 编译器阶段：[docs/cjfir-compiler-stages.md](../cjfir-compiler-stages.md)