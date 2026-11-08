# 快速修复（Quick Fix）覆盖缺口分析

> 范围：基于 K2 quick-fix 注册机制（`CangJieQuickFixRegistrar` / `CangJieQuickFixService`）的缺口盘点。
> 结论基于当前仓库源码快照，不含 `external/` 与 Kotlin 参考实现。

## 1. 结论

| 指标 | 数值 |
|---|---|
| CFIR 诊断总数（`CaCfirDiagnostics.kt` 生成的 `CaCfirDiagnostic` 子接口） | **553** |
| 走 K2 注册机制装配的 quick fix | **1** |
| 覆盖率 | **0.18%** |
| `lazyList` 通道注册数 | 0 |
| `importOnTheFlyList` 通道注册数 | 0（消费侧也未接线） |
| 不走 K2 通道、直挂 PSI/检查的存量修复 | 8（宏 4、未使用 1、cjpm.toml 2、LSP 0） |

一句话：**框架已经对齐 Kotlin K2 的三条通道，但只接了 1 个 factory，其余 552 条诊断点开 Alt+Enter 没有任何修复。**

## 2. 现状基线

### 2.1 框架层已具备的能力

| 组件 | 位置 | 状态 |
|---|---|---|
| 注册器抽象类 + 扩展点 | `code-insight/api/.../fixes/CangJieQuickFixService.kt:76` | 有 `list` / `lazyList` / `importOnTheFlyList` 三条通道 |
| 注册器实现 | `code-insight/fixes/src/.../CangJieQuickFixRegistrar.kt` | 仅注册 1 条 |
| 装配 XML | `code-insight/fixes/resources/META-INF/code-insight/cangjie-code-insight-fixes.xml` | 扩展点 `org.cangnova.cangjie.codeinsight.quickfix.registrar` |
| 列表/工厂协议 | `code-insight/api/.../CangJieQuickFixesList.kt`、`CangJieQuickFixFactory.kt` | `IntentionBased` / `ModCommandBased` 两种 factory |
| 高亮消费（普通） | `intellij-ide/modules/ide/base/.../CangJieDiagnosticHighlightVisitor.kt:194` | 已接线 |
| 高亮消费（lazy） | 同文件 `registerLazyFixes()` | 已接线，但无 factory 可产出 |
| 高亮消费（import-on-the-fly） | `CangJieQuickFixService.getImportOnTheFlyQuickFixesFor` | `internal` 且**全仓库无调用方** |

### 2.2 唯一走 K2 通道的修复

| 诊断 | 修复 | 实现 |
|---|---|---|
| `CaCfirDiagnostic.AbstractMemberNotImplemented` | Implement members | `implementMembers` factory → `ImplementMembersQuickfix`（`code-insight/override-implement/.../ImplementMembersHandler.kt:85`） |

### 2.3 已实现但绕过 K2 通道的修复（双轨）

| 修复 | 挂载方式 | 位置 |
|---|---|---|
| 导入宏包 `{pkg}` | `Annotator` 静默 annotation | `modules/ide/macro/.../MacroCallQuickFixes.kt:130` |
| 改宏名 + 导入 | 同上 | 同文件 `:163` |
| 编译宏包 `{pkg}` | 同上 | `CompileMacroPackageIntentionAction.kt:42` |
| 查看宏展开结果 | `Annotator` 内联 `IntentionAction` | `MacroCallSiteAnnotator.kt:200` |
| Safe delete（未使用声明） | 复写平台 `UnusedSymbol` HighlightInfo | `CangJieUnusedHighlightingProcessor.kt:288,347` |
| 移除 `v` 前缀 / 移除后缀（cjpm.toml） | `LocalInspectionTool` + `LocalQuickFix` | `modules/domain/package-manager/.../ProjectVersionQuickFix.kt:37,53` |
| LSP Code Action | 能力已声明，服务端返回 `emptyList()` | `AnalysisApiCangjieAnalysisFacade.kt:838` |

## 3. 缺口

### 3.1 框架层缺口

| 编号 | 缺口 | 影响 |
|---|---|---|
| F1 | `lazyList` 无任何注册 | Implement Members 这类需全量 resolve 成员的重修复跑在普通通道，随诊断数量线性拖慢高亮 |
| F2 | `importOnTheFlyList` 消费侧未接线（`internal` + 零调用） | 「修复即补 import」链路无法生效，等于死代码 |
| F3 | `ModCommandBased` factory 从未被使用 | 所有修复都是传统 `IntentionAction`，无法进入 ModCommand 的预览 / 批量应用 / 多文件事务链路 |
| F4 | 高亮侧用 `getQuickFixesFor`（抛异常版）而非 `getQuickFixesWithCatchingFor` | 单个 factory 抛异常会中断该诊断**整条**修复枚举，且错误直接冒泡到高亮 |
| F5 | 无「诊断 → 修复」覆盖矩阵校验（CI 不校验注册数量/键的合法性） | 无法防回退；键写错（如注册到泛型基类）只打 error 日志，静默失效 |
| F6 | 无 quick-fix 测试 DSL / 统一 fixture 断言工具 | 现有 `CangJieQuickFixRegistrationTest` 手写，新增修复的测试成本高，实际只有 2 个测试类覆盖 |
| F7 | 无通用 suppress 修复 | 553 条诊断都无法一键 `@Suppress` 抑制（全仓库无 `SuppressIntentionAction` 使用） |
| F8 | registrar 只按诊断类分派，无优先级/排序声明 | 同一诊断多个 factory 时只能靠注册顺序，UI 顺序不可控 |

### 3.2 诊断覆盖缺口（按族）

| 诊断族 | 数量 | 已有修复 | 缺口要点 |
|---|---|---|---|
| Import / 导入 | 9（含 2 条互操作库导入，与互操作族重叠计数） | 0 | `UnresolvedImport`、`MemberNotImported`、`UseExprWithoutImport`、`ImportAliasConflict` 全无 AddImport/RemoveImport |
| 未使用 | 4 | 1（IDE 侧双轨） | `UnusedImport`、`UnusedExpression` 无修复；`UnusedVariable/UnusedFunction` 的修复不在 K2 通道，与诊断来源不一致 |
| 修饰符 / 可见性 | 15 | 0 | `RedundantModifier`、`RepeatedModifier`、`IncompatibleModifiers`、`WrongModifierTarget`、`CannotWeakenAccessPrivilege` 都是 100% 可自动修 |
| 类型不匹配 | 9 | 0 | `TypeMismatch`、`ArgumentTypeMismatch`、`ReturnTypeMismatch`、`AssignmentTypeMismatch` 有 `expectedType/actualType` 字段，但无 cast/改类型修复 |
| 推断失败 | 11 | 0 | `CannotInferParameterType`、`UnableToInferDecl`、`UnableToInferReturnType` 无「显式标注类型」修复 |
| 命名参数 / 实参 | 7 | 0 | `NamedParameterNotFound`、`UnknownNamedArgument`、`NeedNamedArgument`、`WrongNumberOfArguments` 无「改实名参 / 补实参」修复 |
| 抽象成员实现 | 4 | 1 | `NeedMemberImplementation`、`InterfaceMemberMustBeImplemented`、`SpecificMemberMustHaveImplementation` 可复用同一 factory，仅需多注册 3 个键 |
| 属性访问器 | 3+ | 0 | `PropertyMustHaveAccessors`、`ImmutablePropertyWithSetter` 无「生成 get/set」修复 |
| match / 模式 | 31 | 0 | `NonExhaustiveMatch` 带 `missingCases`，可直接生成缺失分支，当前零修复 |
| 弃用 | 17 | 0 | `DeprecatedWarning` 已带 `replacement` 字段，ReplaceWith 修复完全没接 |
| Override / 继承 | 34 | 0 | `NothingToOverride` 等可「删除 override 修饰符」 |
| 宏 | 32 | 4（双轨） | K2 `MacroUnresolved` 本身无 factory，宏修复完全依赖 PSI annotator |
| Java / Objc / Cjmp 互操作 | 82 | 0 | 大多无自动化修复价值，属可接受缺口 |
| ConstEval / 注解 / 其余 | 200+ | 0 | 低优先级 |

### 3.3 已存在但未被复用的底座

以下能力已实现，缺的只是把它们包装成 factory：

- 导入写入：`PsiImportPlanApplier`（幂等、冲突报告）+ `CjPsiFactory.createImportDirective`
- 成员生成：`CangJieMemberGenerationService` / `AbstractGenerateMembersHandler`（Implement members 之外的生成场景可直接复用）
- 安全删除：`CangJieSafeDeleteFix` + `SafeDeleteHandler`
- 宏导入：`MacroImportEditor.hasEquivalentImport/addMacroImport`

## 4. 路线图

| 优先级 | 目标 | 具体项 | 验收 |
|---|---|---|---|
| P0 | 修框架缺陷 | F4 改 `getQuickFixesWithCatchingFor`；F1 把 Implement Members 迁到 `lazyList` | 现有 `CangJieQuickFixRegistrationTest` 通过；注释注入异常 factory 后其余修复仍可见 |
| P0 | 补 import 族 | `UnresolvedImport` / `MemberNotImported` / `UseExprWithoutImport` → AddImport（复用 `PsiImportPlanApplier`） | 3 个新测试：补 import、已存在时跳过、冲突时改限定名 |
| P1 | 补低成本高收益族 | 修饰符 15 条、抽象成员 3 条、`UnusedImport` → RemoveImport、`NonExhaustiveMatch` → 补分支、`DeprecatedWarning` → ReplaceWith | 每族至少 1 个 fixture 测试，注册表条目 ≥ 30 |
| P1 | 接线 F2 | `getImportOnTheFlyQuickFixesFor` 挂到 `CangJieDiagnosticHighlightVisitor`，AddImport 同时注册进该通道 | import-on-the-fly 在补全外场景可触发 |
| P2 | 双轨收敛 | macro 4 条、unused 1 条改由 registrar 注册（保留 Annotator 仅用于静默挂载） | 注册表新增 5 条；删除重复的 Annotator 直挂逻辑 |
| P2 | F5/F6/F8 | 覆盖矩阵 CI 校验、quick-fix 测试 DSL、factory 优先级字段 | CI 能拦住覆盖率下降；新修复写测试 ≤ 10 行 |
| P3 | 类型/推断/命名参数 | 需要类型转换策略与用户选择 UI（chooser），单独立项 | — |
| P3 | LSP Code Action | `AnalysisApiCangjieAnalysisFacade.codeActions` 映射到同一批 factory | code action 返回非空且修复可 apply |

## 5. 附录：优先补齐的映射表（24 条）

| # | 诊断 | 建议修复 | 复用底座 |
|---|---|---|---|
| 1 | `UnresolvedImport` | 删除该 import / 改为相近包 | `CjImportItem` PSI |
| 2 | `MemberNotImported` | 导入 `memberName` 所属包 | `PsiImportPlanApplier` |
| 3 | `UseExprWithoutImport` | 按 `importPath` 补 import | 同上 |
| 4 | `ImportAliasConflict` | 重命名别名 | — |
| 5 | `UnusedImport` | 移除 import | `CjImportItem` |
| 6 | `UnusedExpression` | 移除表达式 / 赋给 `_` | — |
| 7 | `RedundantModifier` | 删除 `modifier` | 字段已给出 token |
| 8 | `RepeatedModifier` | 去重 | 同上 |
| 9 | `IncompatibleModifiers` | 删除其一（给 chooser） | — |
| 10 | `WrongModifierTarget` | 删除修饰符 | — |
| 11 | `CannotWeakenAccessPrivilege` | 提升到 `baseVisibility` | 字段已给出 |
| 12 | `WeakVisibility` | 同上 | — |
| 13 | `NothingToOverride` | 删除 `override` | — |
| 14 | `NeedMemberImplementation` | 同 Implement Members | `MemberNotImplementedQuickfixFactories` |
| 15 | `InterfaceMemberMustBeImplemented` | 同上 | 同上 |
| 16 | `SpecificMemberMustHaveImplementation` | 同上 | 同上 |
| 17 | `PropertyMustHaveAccessors` | 生成 get/set | `AbstractGenerateMembersHandler` |
| 18 | `ImmutablePropertyWithSetter` | `let` → `var` | — |
| 19 | `NonExhaustiveMatch` | 补 `missingCases` 分支 | 字段已给出 |
| 20 | `TypeMismatch` | 作为 `expectedType` 使用 / 显式转换 | — |
| 21 | `ArgumentTypeMismatch` | 同上 | — |
| 22 | `NamedParameterNotFound` | 按目标签名改实参名 | 需 resolve 签名 |
| 23 | `DeprecatedWarning` | 按 `replacement` 替换 | 字段已给出 |
| 24 | `MacroUnresolved` | 复用宏 import / 改名修复 | `MacroCallQuickFixes` |

## 6. 备注

- 诊断接口定义由 `analysis-api-cfir-generator` 生成，新增修复时**必须**用具体诊断类作为注册键（`CaDiagnosticWithPsi::class` 会被 builder 打 error 日志并静默失效）。
- `deveco/modules/domain/package-manager` 与 `intellij-ide/modules/domain/package-manager` 存在逐字重复的 quick fix 实现，双轨收敛时需同步修改。
