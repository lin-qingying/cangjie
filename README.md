# Cangjie

基于 Kotlin/JVM 的仓颉编程语言编译器实现，架构参考 Kotlin K2，功能对齐官方仓颉编译器。

## 编译器管线

12 阶段管线设计（详见 `cjfir-compiler-stages.md`）：

```
源码 (.cj)
  → LOAD_PLUGINS     插件加载
  → PARSE            源码解析（PSI/LightTree）
  → CONDITION_COMPILE 条件编译（@When 裁剪）
  → IMPORT_PACKAGE   包导入（.cjo 外部依赖）
  → MACRO_EXPAND     宏展开
  → CFIR_BUILD       PSI → Raw CFIR
  → CFIR_RESOLVE     多 Phase 语义解析 + 诊断检查
  → FINALIZE         脱糖 → 泛型实例化 → 溢出策略
  → MANGLING         名称修饰
  → SAVE_CJO         .cjo 序列化
  → CFIR2CHIR        CFIR → CHIR 转换 + 优化
  → CODEGEN          CHIR → LLVM IR → 机器码
```

## 模块说明

| 模块 | 职责 | 状态 |
|------|------|------|
| `cfir` | CFIR 数据模型：类型系统、IR 树、访问者 | 已实现 |
| `cfir-build` | 阶段 6: PSI/LightTree → Raw CFIR | 计划中 |
| `cfir-resolve` | 阶段 7: 多 Phase 语义解析 | 计划中 |
| `cfir-serialization` | 阶段 10: .cjo 序列化 | 计划中 |
| `chir` | 阶段 11: CHIR 定义和 CFIR→CHIR 转换 | 计划中 |
| `tests:test-infrastructure` | Kotlin 风格测试基础设施（Directive/TestServices/配置DSL） | 进行中 |

## 构建

```bash
./gradlew :cfir:compileKotlin    # 单模块编译
./gradlew compileKotlin          # 全量编译
./gradlew check                  # 运行所有检查和测试
```

## 测试约定

全项目测试实现与组织规范见：`TESTING_CONVENTIONS.md`。

## 开发规范

项目级开发规范与工程治理约定见：`DEVELOPMENT_CONVENTIONS.md`。

## 测试框架进展

- 已引入 Kotlin 风格的轻量测试配置模型：`TestConfigurationBuilder`、`TestFacade`、`AnalysisHandler`、`AbstractCangjieCompilerTest`。
- 采用树形测试模块结构：测试基础设施归属 `:tests:test-infrastructure`。
- 当前已确认并遵循规则：**testData 与测试代码按模块归属放置**（例如 `psi2cfir` 测试仍放在 `cfir/raw-cfir/psi2cfir` 模块内）。
- Raw CFIR 测试入口已对齐 Kotlin 风格为 **Generated 类**（模块内自洽，不依赖独立 `compiler-tests`）：
  - 生成器：`cfir/raw-cfir/psi2cfir/testFixtures/org/cangjie/cfir/builder/TestGeneratorForPsi2Cfir.kt`
  - 产物：`cfir/raw-cfir/psi2cfir/tests-gen/org/cangjie/cfir/builder/RawCfirBuilderTestCaseGenerated.kt`
  - 抽象基类：`cfir/raw-cfir/psi2cfir/testFixtures/org/cangjie/cfir/builder/AbstractRawCfirBuilderTestCase.kt`
- Raw CFIR testData：`cfir/raw-cfir/psi2cfir/testData/rawBuilder`。
- 已接入 `DUMP_CFIR` 指令与 golden file 对比；默认为严格比对模式（不自动改写期望），可通过 `-Dupdate.test.data=true` 显式更新期望文件。
- `:cfir:raw-cfir:psi2cfir:test` 会自动先执行 `generateRawCfirBuilderTests`，因此新增 `.cj` 文件后可自动生成对应测试方法。
- 已新增 4 类测试入口（对齐 Kotlin 分类）：
  - `RawCfirBuilderLazyBodiesByAstTestGenerated`
  - `RawCfirBuilderLazyBodiesByStubTestGenerated`
  - `RawCfirBuilderSourceElementMappingTestGenerated`
  - `RawCfirBuilderTestCaseGenerated`
- 当前能力状态：`SourceElementMapping` 与 `LazyBodies(ByAst/ByStub)` 均已可执行，`PsiRawCfirBuilder` 已支持 `BodyBuildingMode`（`NORMAL`/`LAZY_BODIES`）。
- 已新增 `CfirBasicTypeRef`，`CjBasicType` 在 RAW 阶段直接映射到基础类型引用，不再降级为 `Unsupported type element`。
- 已新增 `CfirVArrayTypeRef`，`CjVArrayType` 在 RAW 阶段直接映射为专用定长数组类型引用，保留元素类型与 `$N` 尺寸字面量。
- 当前测试发现范围：主 `RawBuilder` suite 与两个 `LazyBodies(ByAst/ByStub)` suite 现均扫描 `testData/rawBuilder` 根目录；`rawBuilder/expressions` 已补齐缺失表达式/错误恢复用例，并为 lazy 模式补齐同目录下的 `*.lazyBodies.txt` 基线。
- tests-gen 已加入 all-files-present 等效校验，新增 `.cj` 用例将被覆盖检查拦截漏测。
- 下一步建议：在同一框架上补齐 `CfirResolveFacade` + `DiagnosticsHandler`，并接入多模块/诊断类 testdata。
- 已完成诊断检查器框架对齐方案设计（计划补齐 Declaration/Expression/Type checkers 生成与运行入口）。

## OpenSpec 变更进展

- 已新增变更提案：`openspec/changes/fix-cfir-renderer-architecture/`。
- 该提案聚焦 `CfirRenderer` 架构升级：在保持 golden file 兼容的前提下，引入可组合 renderer/profile 设计，避免“仅服务 golden 对比”的能力定位。

### CfirRenderer profiles

- `CfirRenderer.withGoldenCompat()`：供 golden file 与回归测试使用，`CfirRenderer.render(element)` 兼容入口内部委托到该 profile。
- `CfirRenderer.withDebug()`：供调试和开发期可视化使用，当前与 golden 兼容输出共享默认组件，但 API 语义独立。
- `CfirRenderer.withReadability()`：供后续更偏可读性的文本输出使用，当前与默认组件共享实现。
- `resolvePhaseRenderer` 扩展点已在框架中预留，但本次变更**不额外输出** resolve phase 文本。
- 当前刻意**不引入** `CfirRendererOptions`：现有稳定需求可由离散 profile 覆盖，避免过早扩大配置面。
- 已新增变更提案：`openspec/changes/add-cfir-varray-type-ref/`。
- 该提案聚焦补齐 `CfirTypeRef` 对 `VArray<T, $N>` 的 Raw CFIR 建模缺口：在不改动 parser/PSI 与 cone 层既有设计的前提下，补充 `cfir-tree` 类型引用表示、`psi2cfir` lowering 与 rawBuilder 测试覆盖。
- 已新增变更提案：`openspec/changes/add-rawbuilder-missing-expression-tests/`。
- 该提案聚焦补齐 `cfir/raw-cfir/psi2cfir/testData/rawBuilder` 中对缺失表达式 / 错误恢复路径的测试覆盖，并参考 Kotlin `rawBuilder/expressions` 的目录组织方式收敛本仓库的套件发现范围。
- 已推进变更实现：`openspec/changes/fix-rawbuilder-let-position-handling/`。
- 本次实现保留现有具名 `CfirVariable(name)` 语义，同时新增 `CfirPatternVariable : CfirCallableDeclaration`（持有完整 `pattern`，并通过派生查询提供 `bindings` / `allPatternDeclarations`），避免再用具名变量或 property 语义掩盖 pattern variable。
- `PsiRawCfirBuilder` 已补齐 `CjFieldVariable` 与 `CjPatternVariable` 的 declaration dispatch；类体字段不再统一退化为 `<error-declaration>`，`classWithMembers`、`classWithTypeParameters`、`structDeclaration` 及新增 `classMembersOrderStability` 用例均已更新 normal/lazyBodies 基线。
- 已完成 `:cfir:raw-cfir:psi2cfir:test` 全量验证；为兼容 malformed-expression 的 by-stub 路径，测试基座补齐了最小 IntelliJ application/project 扩展点与同步 `AsyncExecutionService`，并同步更新 `sourceElementMapping` golden 以匹配修复后的真实表达式映射结果。

## 目录结构

```
cangjie/
├── cfir/                      # CFIR 数据模型
│   └── src/main/kotlin/org/cangjie/cfir/
│       ├── CfirElement.kt     # IR 根节点
│       ├── common/            # 基础类型（Name, Visibility）
│       ├── types/             # 类型系统（Cone types + TypeRef）
│       ├── declarations/      # 声明节点
│       ├── expressions/       # 表达式节点
│       ├── patterns/          # 模式匹配
│       ├── references/        # 引用
│       ├── symbols/           # 符号
│       └── visitors/          # 访问者模式
├── external/                  # 外部参考源码（不参与构建）
│   ├── cangjie_compiler/      # 仓颉语言编译器源码（C++ 参考实现）
│   ├── intellij-cangjie/      # IntelliJ 仓颉插件（Kotlin K1）
│   └── kotlin/                # Kotlin 编译器源代码（K2 架构参考）
├── cjfir-compiler-stages.md   # 编译器阶段设计文档
└── gradle/                    # Gradle 配置
```

## 技术栈

- **语言**: Kotlin/JVM
- **JDK**: 17
- **构建工具**: Gradle (Kotlin DSL)
