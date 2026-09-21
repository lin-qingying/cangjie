# 修复计划：Linkage/PrivateLimit 解析期符号归属 + 版本门禁下的 EXPORT_SAME_PRIVATE_DECL

日期：2026-09-21
前置证据：`cfir/analysis-tests/build/linkage-private-dup-analysis-20260921.md`（根因链、双 SDK 实测、官方取证）
状态：**计划，未实施**。执行前提：`.gradle/queue` 空闲（检查 `owner.pid`）。

---

## 0. 背景一段话

`linkage/private_limit/private_dup0{1,2,3}` 共 6 条 LLT 失败（LLT+PSI），根因是
**class-like 符号 → 声明文件的归属被 ClassId 单值表压平**，导致官方"顶层 private 只对同文件可见"
（`LookUpImpl.cpp:454-458`）判定拿到错误输入：第一文件 2 候选报 `AMBIGUOUS_USE`，第二文件自己的 `A`
被过滤报 `UNDECLARED_TYPE_NAME`。同时，官方 v1.0.2（提交 `e3200e1`，2025-09-16）新增
`sema_export_same_private_decl`（CFIR 已有孤儿实现 `checkExportSamePrivateDecl`，逐文件计数、
无 linkage 前置、无 fixture 消费）。**用户决定：支持该诊断，用版本门禁控制。**

## 1. 目标 / 非目标

**目标**
1. P0：修复解析期符号归属，6 条失败转绿（默认口径下 dup01/02 转为期望 `EXPORT_SAME_PRIVATE_DECL`，见 §2 口径修订）。
2. P1：新增 `LanguageVersion.CANGJIE_1_0_2` 与 `LanguageFeature.ExportSamePrivateDeclCheck` 版本门禁。
3. P2：把孤儿实现重写为与官方对齐的包级检查（包级收集 + 导出面判定 + 官方锚点 + 门禁）。
4. P3：fixtures 双口径覆盖（默认版本报、`// LANGUAGE_VERSION: 1.0.0` 钉零）。

**非目标**
- 不引入 `Linkage` 枚举 / 不改 cfir-tree 生成代码（判定以 session 级派生服务承载）。
- 不实现 note（`builder.AddNote`）——CFIR 诊断基建无 note 家族（`CfirErrors` 无 `*_NOTE` 先例），差异登记在 KDoc（§5.4）。
- 不动 `external/cangjie_compiler` 镜像。

## 2. 口径修订（重要，覆盖前一轮结论）

- 生产与测试的默认语言版本 = `LanguageVersion.LATEST_STABLE`（1.1.3）：
  `LanguageVersionSettings.kt:605`（`DEFAULT`）、`LanguageVersionSettingsBuilder.kt:36`、
  `LanguageVersionSettingsConfigurator.kt:29`（无参数时沿用 current）。
- 因此 `sinceVersion = CANGJIE_1_0_2` 的门禁在**默认口径下是开的** ⇒
  **修订**前一轮"fixture 不改"的结论：dup01/dup02 在默认口径下期望**加** `<!EXPORT_SAME_PRIVATE_DECL!>` 标记
  （与 cjc 1.0.5 实测一致）；v1.0.0 行为改由**新增钉版 fixture**（`// LANGUAGE_VERSION: 1.0.0`，测试基建已支持，
  `LanguageVersionSettingsBuilder.configureUsingDirectives:81-84`）表达。
- 官方语义锚点（cjc 1.0.5 实测，见分析报告 §3.2b）：error 在**首个声明**的 identifier
  （testa.cj:3:15），后续声明为 note（本文档决定不实现 note，见 §5.4）；dup03 形状零诊断。

## 3. P0：解析期符号归属修复（先做，其它都依赖它）

### 3.1 现状证据
- `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirSourceSymbolProvider.kt`
  - `:886` `state.classifierMap: MutableMap<ClassId, CfirClassLikeSymbol<*>>`（单值，首声明胜出）
  - `:891` `state.classifierContainerFileMap: MutableMap<ClassId, CfirFile>`（单值）
  - `:775-803` `recordClassLikeClassifier`：后续同名声明只进 `nameConflictsTracker`（`:789-795`）
  - `:190` 仅有 ClassId 版 `getCfirClassifierContainerFileIfAny(fqName: ClassId)`
- `cfir/providers/src/.../CfirProvider.kt:81-82`：symbol 级查询默认折回 ClassId（根节点）。
- 消费点：`CfirAccessibilityChecker.kt:554-570` `privateAccessible`（`:565` 同文件判定）；
  `CfirProviderUtils.kt:93-102` `getContainingFile()`。
- 既有同构先例：callable 的归属就是**按 symbol** 的（`State.callableContainerFileMap`，`:895` 附近）——
  本修复就是把 classifier 对齐到同一模式，不是新发明。
- 注意区分：conflict checker 路径（`CfirConflictsHelpers.kt:543` `conflictingFile ?:` 显式传入）
  自带文件来源，不受此 bug 影响；**类型解析/可访问性路径**才是受害者
  （`CfirTypeCandidateCollector.kt:67/:96` → `isAccessible`）。

### 3.2 改动
1. `CfirSourceSymbolProvider.State` 增加 `classifierDeclarationFileMap: MutableMap<CfirClassLikeSymbol<*>, CfirFile>`
   （key = symbol 身份，与 callable 容器文件索引同形）；`recordClassLikeClassifier` 中**每次**登记
   （含 tracker 分支）都写入。
2. `CfirSourceSymbolProvider` override `getCfirClassifierContainerFileIfAny(symbol: CfirClassLikeSymbol<*>): CfirFile?`
   优先按 symbol 身份查；未命中回落 ClassId 表（外部/反序列化符号）。
3. **保留** ClassId 单值 `classifierMap`/`classifierContainerFileMap`：它们服务
   `getClassLikeSymbolByClassId` 的"稳定首项"语义（`CfirPackageMemberScope.kt:87-97`）、
   import 解析等 ClassId 语义消费点，不属于本 bug。
4. 实施前先 `grep -rn "getContainingFile()" cfir --include=*.kt`（排除 build/bin/tests-gen/gen）
   列全消费点，确认无一依赖"同 ClassId 折叠为同文件"的错误行为。

### 3.3 预期
- testa：聚合候选 `[A_testa(可见), A_testb(归属 testb.cj → 被过滤)]` = 1 ⇒ `AMBIGUOUS_USE` 消失。
- testb：本文件 `A` 正确归属 ⇒ 可见 ⇒ `UNDECLARED_TYPE_NAME`/`UNRESOLVED_REFERENCE` 消失。
- P0 单独验证：3 个 fixture 在默认口径下应只剩 `EXPORT_SAME_PRIVATE_DECL`（P2 落地前会先"多出"该诊断，
  因为门禁默认开而旧实现无门禁——**P0 与 P1/P2 需在同一验证轮内落地**，或临时把 P1 的 feature
  先落地再跑 P0 验证；推荐提交顺序见 §8）。

## 4. P1：版本门禁基建（common）

### 4.1 `LanguageVersion.CANGJIE_1_0_2`
- `common/src/org/cangnova/cangjie/LanguageVersionSettings.kt:64-67`：在 `CANGJIE_1_0_0` 与
  `CANGJIE_1_0_5` **之间**插入 `CANGJIE_1_0_2(1, 0, 2)`（enum 比较依赖声明顺序）。
- `ApiVersion` 无需新增条目（`ApiVersion.createByLanguageVersion` 按版本号构造；`LATEST=entries.last()` 不变）。
- ripple：跑 `common:test --tests '*LanguageVersionSettings*'`；grep 对 `LanguageVersion.entries`
  的枚举断言（`LanguageVersionSettingsTest.kt`、`LanguageVersionSettingsConfiguratorTest`）。

### 4.2 `LanguageFeature.ExportSamePrivateDeclCheck`
- 位置：`LanguageFeature` 枚举（`LanguageVersionSettings.kt:137` 起），新增：
  ```kotlin
  /**
   * 同包同名顶层 private nominal 声明均需导出时报 EXPORT_SAME_PRIVATE_DECL。
   *
   * 官方于 v1.0.2（提交 e3200e1，2025-09-16，CheckFunctionLinkage.cpp
   * AnalyzeFunctionLinkage）引入，v1.0.0 无此检查；上游注释自述为
   * PrivateDecl.ti 符号名不含文件名的临时 workaround。cjc 1.0.0/1.0.5
   * 双 SDK 实测确认分界（见 linkage-private-dup-analysis-20260921.md §3.2b）。
   */
  ExportSamePrivateDeclCheck(
      LanguageVersion.CANGJIE_1_0_2,
      behaviorAfterSinceVersion = LanguageFeatureBehaviorAfterSinceVersion.CanStillBeDisabledForNow(NO_ISSUE_SPECIFIED),
  ),
  ```
- 选择 `CanStillBeDisabledForNow`：官方自述临时行为，保持 `// LANGUAGE: -ExportSamePrivateDeclCheck`
  可显式关闭（`featureSupportStatus` 中显式 DISABLED 在版本判定之后生效，`:499-502`，路径可测）。

## 5. P2：诊断实现重写（cfir/checkers）

### 5.1 删除旧实现
- `CfirGeneralSemanticsChecker.kt`：删 `:89` 调用与 `:290-329` `checkExportSamePrivateDecl(file)`
  （逐文件计数，语义错误）；`:446`/`:491` 附近的文档清单同步。

### 5.2 新 owner：conflicts 家族包级检查
- 位置：`CfirConflictsHelpers.kt` 同层新增（或并入该文件）`CfirExportSamePrivateDeclCheck`，
  与 `collectTopLevelConflict` 共用收集设施（`:518` `collectTopLevel` →
  `:227` `packageMemberScope.processClassifiersByName` + `nameConflictsTracker`）。
- 收集：对本包每个名字，取"全部已知同名顶层 nominal 声明" =
  provider 首项（ClassId 表）+ tracker 重声明（`CfirDeclarationAvailabilityProvider.classLikeCandidates:177-183`），
  过滤 `status.visibility == Private` 且顶层。
- **去重身份**：按 symbol 身份；文件序 = 包内文件登记序（provider 需暴露稳定文件序；
  若现有 API 无序，在 `CfirSourceSymbolProvider` 增加 `classifierDeclarationFileMap` 之外的
  登记序列表或按文件声明的顺序重建——实施时确定，禁止依赖 hashMap 迭代序）。

### 5.3 导出面判定（linkage != INTERNAL 的替代）
- 新 session 级服务（如 `cfir/providers` 下 `CfirExportSurfaceOracle`，session memoized），
  移植官方最小链（`external/cangjie_compiler/src/Sema/CheckFunctionLinkage.cpp`）：
  - `PerformPublicType`（`:536` 附近）：顶层 exported nominal ⇒ 其**成员变量**（不论成员可见性）
    `linkage = EXTERNAL` 且 `AddExportedTy(vd.ty)`（`:520-537` 区域）；
  - `AnalyzeExternalLinkageByExportedTy` worklist（`:545-558`）：被导出类型引用到的顶层声明 ⇒ EXTERNAL，
    其成员变量/类型继续入队（对齐官方传播闭包，禁止只做一层）；
  - private 成员**函数**不导出（官方规则 3，`:592-597` 注释）⇒ 解释 dup03 零诊断。
- 输出：`Set<CfirClassLikeSymbol<*>>`（linkage 非 INTERNAL 的顶层 nominal）。
- 实施者必须先通读 `CheckFunctionLinkage.cpp:180-660` 全文再落码；每个语义决定注明官方行号。

### 5.4 上报
- 门禁：`context.languageVersionSettings.supportsFeature(LanguageFeature.ExportSamePrivateDeclCheck)`，
  不满足直接 return（对齐 `CfirGeneralSemanticsChecker.kt:277-279` 既有模式）。
- 条件：同名组 ≥2 且**全部**成员 ∈ 导出面集合。
- 锚点：ERROR `EXPORT_SAME_PRIVATE_DECL` 于**首个声明**的 identifier source
  （`CfirErrors.EXPORT_SAME_PRIVATE_DECL`，factory 已存在 `CfirDiagnosticsList.kt:1645`，
  消息文案与官方逐字一致 `CfirErrorsDefaultMessages.kt:1047`）。
- **登记差异**（KDoc）：官方对后续声明附 note（"same with private declaration"）；
  CFIR 诊断基建无 note 家族（全 `CfirErrors` 无 `*_NOTE`），V1 不上报后续声明，
  差异与官方 `MakeRange(ret.first->second->identifier)`（`CheckFunctionLinkage.cpp:643`）一并注释。

## 6. P3：fixtures

| fixture | 版本 | 期望 |
|---|---|---|
| `private_limit/private_dup01/testb.cj`（改） | 默认(1.1.3) | testa 的 `A` 加 `<!EXPORT_SAME_PRIVATE_DECL!>`；testb 的 `A` 不加 |
| `private_limit/private_dup02/testb.cj`（改） | 默认 | 同上 |
| `private_limit/private_dup03/testb.cj` | 默认 | 维持零诊断 |
| 新增 `private_dup01_langver100/testb.cj` | `// LANGUAGE_VERSION: 1.0.0` | 两文件零诊断（P0 生效证明） |
| 新增 `private_dup03_langver100/testb.cj` | 同上 | 零诊断 |
| 新增 `private_dup_noreference/testb.cj`（同名 private 无任何 public 面引用） | 默认 | 零诊断（门禁开但未导出 ⇒ 不报，防过报） |

- 期望值全部以 cjc 实测为据（1.0.5 报错版、1.0.0 零诊断版，探针见 `/tmp/cj_priv2/`）；
  `testAllFilesPresent` 元数据随目录自动生成（重跑 generator）。

## 7. P4：验证矩阵（全部经 gradle-queue-cli，禁止直连 gradlew）

```bash
java -jar gradle-queue-cli/build/libs/gradle-queue-cli.jar --project-dir 'D:\code\intellij\cangjie' \
  :cfir:analysis-tests:test --tests '*TestGenerated$Linkage*' --tests '*TestGenerated$Lookup*' \
  --tests '*LanguageVersionSettings*' :common:test --tests '*LanguageVersionSettings*' \
  --continue --console=plain > /tmp/linkage-fix.log 2>&1
```
1. P0 独立验证（P1 已合入后）：Linkage 切片 `136 tests` 基线上，仅 dup01/02 的
   `AMBIGUOUS_USE`/`UNDECLARED_TYPE_NAME` 消失、剩余 `EXPORT_SAME_PRIVATE_DECL`（旧实现无门禁期）。
2. P2 后：dup01/02 命中新标记，dup03/钉版 fixture 零诊断。
3. 全量回归：`:cfir:analysis-tests:test --continue`，与最近 ledger diff（fixed/regressed 逐条列名）；
   重点盯 `Lookup/MultiFilesPrivate00`、`Linkage/access*`、`Diagnostics*TestGenerated$Typealias`。
4. 输出解读只用本轮控制台日志 + `build/test-results/test/TEST-*.xml`（勿用陈旧 HTML）。

## 8. 提交拆分（每步验证过即 commit）

1. `common: CANGJIE_1_0_2 + ExportSamePrivateDeclCheck 门禁`（P1，纯新增，先落——它让 P0 验证期
   旧实现提前受门禁控制，避免中间态污染）。
2. `cfir/providers: 顶层 classifier 按符号归属声明文件`（P0，行为修复，Linkage 切片转绿验证）。
3. `cfir/checkers: EXPORT_SAME_PRIVATE_DECL 包级导出面检查`（P2，替换孤儿实现）。
4. `testData: Linkage/PrivateLimit 双口径 fixtures`（P3）。
5. `docs/REPAIR_LOG`：按技能 Repair Report Fields 补 verification 组。

## 9. 风险与破坏半径

- **P0**：`getContainingFile()` 消费点扫描为前置；行为变化仅限"同 ClassId 多声明"场景
  （现网即 3 个失败 fixture）；回归面 = 全量 LLT。
- **P1**：`LanguageVersion` 新增条目影响 `entries` 断言与 `parse("1.0.2")` 由报错变成功——需跑
  common 切片测试；无生产行为变化（默认版本 1.1.3 下新 feature 由 P2 消费）。
- **P2**：删除孤儿实现属行为可见变化（同文件同名 private 此前可能多报，现不再）；门禁默认开，
  语料中若有其它"同包跨文件同名 private 顶层 nominal 且被 public 面引用"的形状会新增诊断——
  已做同族扫描（分析报告 §5）未见，全量回归兜底。
- 并行会话风险：动手前 `find <dirs> -newermt "-5 minutes" -not -path "*/build/*"` 检查他人写入；
  单文件单次 Edit，改后 `grep -n <新符号>` + `git diff --numstat` 核验。

## 10. 开放问题（实施前确认）

1. note 的取舍：V1 不实现（本计划）vs 补 NOTE 诊断家族（涉及 generator + 渲染，另立任务）。
2. `CANGJIE_1_0_2` 进 enum（本计划，忠实官方分界）vs 门禁直接用 `CANGJIE_1_0_5`（零 ripple，
   但 1.0.2–1.0.4 语义失真；官方 tag 存在 v1.0.2/v1.0.3-beta）。
3. 导出面 oracle 放 `cfir/providers`（本计划）还是 `cfir/resolve`——按消费方
   （checkers 经 session 取用）与现有 `CfirDeclarationAvailabilityProvider` 的归属（providers）定。
