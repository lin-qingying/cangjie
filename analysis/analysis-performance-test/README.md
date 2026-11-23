# analysis/analysis-performance-test/ — Analysis API 性能/统计测试入口

独立模块承载 OpenTelemetry SDK 与统计断言基类，验证各统计通路（metrics / span）真实产出数据；每次运行自动记录并与历史基线对比，产出本地性能报表。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.analysis.api.performance.test`（testFixtures） | 统计测试基座：`AbstractAnalysisApiPerformanceTest`（用例基类）、`CaPerformanceTestTelemetry` / `CaPerformanceTestOpenTelemetryProvider`（OTel SDK 宿主，InMemory reader 供断言，可选 OTLP 导出）、`CaPerformanceBaselineComparator` / `CaPerformanceHistoryStore` / `CaPerformanceRecorder`（基线对比、运行历史落盘）、`CaPerformanceReportRenderer`（HTML / 文本报表） |
| `org.cangnova.cangjie.analysis.api.performance.test`（test） | 统计用例：PSI 解析、resolve 阶段耗时与 span 嵌套、raw build、宏构造三阶段、诊断收集、session 创建与反序列化、指标命名空间等 |

## 运行前置条件

- 无特殊前置：测试宿主由 `CaPerformanceTestServiceRegistrar` 直接把统计开关 `cangjie.analysis.statistics` 置为 `true`，等价于用户在 IDE 里改了该值。
- OTLP 导出与 JFR 采集为可选通路，不传对应参数时完全不启用（默认零开销）。

## 命令

```bash
./gradlew :analysis:analysis-performance-test:test
./gradlew :analysis:analysis-performance-test:test --tests "*CaParserStatisticsTest*"

# 可选：OTLP 导出到本地 collector（gRPC endpoint）
./gradlew :analysis:analysis-performance-test:test -Dcangjie.performance.otlp.endpoint=http://localhost:4317

# 可选：采集 JFR 记录（声明级阶段事件）
./gradlew :analysis:analysis-performance-test:test \
    -Pcangjie.jfr.output=analysis/analysis-performance-test/build/reports/analysis-performance/run.jfr
```

## 产物

| 文件 | 内容 |
|---|---|
| `build/reports/analysis-performance/performance-report.html` | 单文件 HTML 报表（人看） |
| `build/reports/analysis-performance/performance-report.txt` | 纯文本摘要（便于 grep 与 CI 留档） |
| `.gradle/performance-history/runs.jsonl` | 运行历史（最近 50 次，刻意不在 `build/` 下以便跨 `clean` 存活） |

## 依赖

- OpenTelemetry（`api` / `sdk` / `sdk-testing` / `exporter-otlp`，见 `libs.opentelemetry.*`）
- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`、`:analysis:analysis-api-cfir`、`:analysis:analysis-api-standalone`、`:analysis:low-level-api-cfir` 及其 testFixtures
- `:analysis:analysis-test-framework`、`:analysis:analysis-api-impl-base`（testFixtures）

## 相关文档

- `../docs/analysis-performance-statistics.md` — 性能统计使用说明（开启方式、指标清单、跑测试、看结果）
- `../docs/analysis-performance-known-issues.md` — 审计确认的缺陷、实测证据与修复方向
- `../README.md` — Analysis 模块总览
