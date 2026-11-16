# analysis/analysis-real-project-test/ — 真实项目对照测试

真实项目对照测试：先 git 检出固定提交，`cjpm clean/build` 确认真实项目编译无错误（ground truth），再在同一项目上逐文件运行 CFIR 分析，断言无文件级异常、无被隔离吞掉的元素级异常。框架类放在 testFixtures，测试模块（cfir / lsp / ide）可通过 testFixtures 依赖复用。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.analysis.realproject`（testFixtures） | 测试框架：`RealProjectTestBase`（多项目调度、失败隔离与统一断言）、`RealProjectSession`（git 检出 → `cjpm clean/build` → cjpm 源码根 + `.cjo` 依赖组装为 standalone 模块图 → 逐文件 CFIR 诊断）、`CjpmRunner` / `GitFixture` / `RealProjects`（项目配置解析）、`RealProjectReport` / `RealProjectHtmlReport`（JSON / HTML 报告产出） |
| `org.cangnova.cangjie.analysis.realproject`（test） | 用例：`RealProjectAnalysisTest`（`@Tag("realProject")`，真实项目分析主入口）、`CjpmBinDependenciesParsingTest` / `CjpmPackageDeclarationParsingTest`（cjpm 工程描述解析） |

## 运行前置条件

- 需要网络（git clone 真实项目）与仓颉 SDK（cjpm 工具链）。
- 项目列表二选一：
  - 本模块目录下的 `projects.properties`（键格式 `<id>.repo` / `<id>.ref` / `<id>.sdk`（可选，缺省用 `-PcangjieHome`）/ `<id>.refresh`（可选）），或 `-PrealProjectConfig=<path>` 指定其它文件；
  - 构建参数 `-PrealProjectRepo=<git-url> -PrealProjectRef=<commit>`（单项目形态）。
- 未配置任何项目时用例跳过；`-PrealProjectFilter=<id>` 可只跑列表中匹配的项目。
- 单个项目失败（clone / cjpm / 分析异常）不中断其它项目，最后统一断言并产出报告。

## 命令

```bash
./gradlew :analysis:analysis-real-project-test:test
./gradlew :analysis:analysis-real-project-test:test -PrealProjectRepo=<git-url> -PrealProjectRef=<commit>
./gradlew :analysis:analysis-real-project-test:test -PrealProjectFilter=amqp
```

## 产物

- `build/real-projects/checkout/<id>-<ref 前 12 位>/` — 真实项目检出与 cjpm 构建；
- `build/real-projects/reports/index.html` — 全部项目索引；每项目在 `reports/<id>/` 下有 `cfir.json`（诊断、异常、隔离异常、耗时）与 `report.html`。

## 相关文档

- `REPAIR_REPORT.md` — 一次真实项目 CFIR 问题的修复报告（归因表与红→绿记录）
- `../README.md` — Analysis 模块总览
- `../../TESTING_CONVENTIONS.md` — 测试约定
