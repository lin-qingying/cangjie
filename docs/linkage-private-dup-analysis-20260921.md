# Linkage / PrivateLimit / private_dup0{1,2,3} 失败只读分析

日期：2026-09-21　范围：只读，未改动任何源码/fixture，未运行 Gradle（`.gradle/queue` 锁被并行会话持有）

## 1. 失败面

数据来源：最近一次全量 `:cfir:analysis-tests:test`（task `c2010ba1`，16:44:48 起，17:00:41 结束，exitCode=1）写出的
`build/test-results/test/*.xml`（该目录随后被并行会话的聚焦 run 覆盖，故下述数字是那次全量的快照）。

`CfirAnalysisLLTTestGenerated$Linkage` 组共 8 个用例（access01/02、access_err、base01/02 + PrivateLimit/PrivateDup01/02/03），
LightTree 与 PSI 两条路径共 16 条，**6 条失败**，全部是 `PrivateLimit`：

| fixture | LLT | PSI |
|---|---|---|
| `private_dup01`（两份 `private class A`） | FAIL | FAIL |
| `private_dup02`（class A / struct A） | FAIL | FAIL |
| `private_dup03`（`private func f(a: A)`） | FAIL | FAIL |

其余 10 条（access01/02/err、base01/02）通过；`Functionlinkage` 组全部通过。
**LightTree 与 PSI 两条路径的实际输出逐字符相同**，说明失败点在其共享的 provider/scope 层，与建树路径无关。

实际 vs 期望（三个 fixture 形状一致）：

```
testa.cj（第一单元）: private let a: <!AMBIGUOUS_USE!>A<!> = A()          ← 期望：无诊断
testb.cj（第二单元）: private let a: <!UNDECLARED_TYPE_NAME!>A<!> = <!UNRESOLVED_REFERENCE!>A<!>()  ← 期望：无诊断
```

注意不对称：**第一个文件报"歧义"，第二个文件报"未声明"**；表达式位置 `A()` 只在第二个文件报错。

## 2. 根因（单一共享 owner，两条耦合缺陷）

两个逻辑编译单元同属 `package testa`，各有一个顶层 `private class A`。官方语义下顶层 private 是**文件私有**：
`external/cangjie_compiler/src/Sema/LookUpImpl.cpp:454-458` `IsTargetVisibleToNode`
（`!target.TestAttr(PRIVATE) || *target.curFile == *node.curFile`），过滤点在 `:567` 的 `LookUpImpl::Lookup`
与 `:653-658` 的 `LookupTopLevel` —— 两个 `A` 各自只能被本文件看见，因此**两条零诊断**。

CFIR 侧把"哪个文件声明的"压成了 ClassId 单值索引，于是两处机制同时失效：

### 2.1 符号 → 声明文件归属被 ClassId 压平（直接原因）

- `cfir/providers/src/.../resolve/providers/CfirSourceSymbolProvider.kt`
  - `:886` `state.classifierMap: MutableMap<ClassId, CfirClassLikeSymbol<*>>`（**单值**）
  - `:891` `state.classifierContainerFileMap: MutableMap<ClassId, CfirFile>`（**单值**）
  - `:775-803` `recordClassLikeClassifier`：`previousSymbol == null` 才写这两个表；否则只把第二个声明
    连同 `newSymbolFile` 送进 `nameConflictsTracker`（`:789-795`），**不写入文件索引**。
  - `:808-813` `computeClassId(pkg, shortName)` —— 键里没有 file 维度。
- `cfir/providers/src/.../resolve/providers/CfirProvider.kt:81-82`
  `getCfirClassifierContainerFileIfAny(symbol) = getCfirClassifierContainerFileIfAny(symbol.classId)`
  —— **符号级查询被折回 ClassId 键**。全仓无 symbol 身份的覆盖实现（唯一 override 是
  `CfirSourceSymbolProvider.kt:190` 的 ClassId 版本）。
- 消费方：`cfir/providers/src/.../resolve/providers/CfirAccessibilityChecker.kt:554-570` `privateAccessible`，
  `:565` `if (ownerClass == null) return useSiteFile != null && useSiteFile == declarationFile`
  —— 顶层 private 的"同文件可见"判定本身写对了，但它拿到的 `declarationFile`
  由 `CfirProviderUtils.kt:93-102` → `getContainingFile()` 提供，即上面那张 ClassId 单值表。

**后果**：两个同名 `A` 的"声明文件"都读成**首先登记的那个文件**（testa.cj）。
于是可见性判定在两个方向上同时出错：对 testa.cj 过宽（外文件的 `A` 也被判为可见），对 testb.cj 过严（自己的 `A` 被判为不可见）。

### 2.2 跨文件顶层 private 重名被当成"重声明"候选

- 候选聚合：`cfir/providers/src/.../declarations/CfirDeclarationAvailabilityProvider.kt:177-183`
  `classLikeCandidates = provider 索引 + nameConflictsTracker 的重声明`。
- 类型解析在拿到首选候选后**必然**再做一次全量聚合，并把 ≥2 个候选判为歧义：
  `cfir/resolve/src/.../resolve/CfirTypeResolver.kt:579-596` `classifierRedeclarationAmbiguity`，
  入口 `:439-448` `resolveSimpleClassLike` → `:466-471` `classLikeCandidates` → `:604-616`。
- 过滤发生在收集期：`cfir/resolve/src/.../resolve/CfirTypeCandidateCollector.kt:100-113` `isAccessible`
  → `CfirAccessibilityChecker.checkClassLike`（**已是既有接缝**）。

**合并解释两条不对称症状**：

- testa.cj：文件级 scope（`CfirFileDeclaredTopLevelScope`，按 use-site file 构造，正确给出本文件的 `A`）
  → `declarationFile` = testa.cj = use site → 可访问；随后聚合又补上被遮住的另一个 `A`，
  它的 `declarationFile` 同样错读成 testa.cj → 也可访问 → **2 个候选 → AMBIGUOUS_USE**。
- testb.cj：文件级 scope 给出自己的 `A`，但 `declarationFile` 错读成 testa.cj → 被过滤掉；
  包级 scope（`CfirPackageMemberScope.kt:87-97`，经 `getClassLikeSymbolByClassId` 返回 ClassId 单值首项）
  给出的还是那个被错读文件的 `A` → 也被过滤掉 → **0 个候选 → UNDECLARED_TYPE_NAME / UNRESOLVED_REFERENCE**。

即：**"框架里已有正确的可见性规则（file-private），却因为符号身份缺少 file 维度而拿到错误的 use-site 对照物"**。
这不是某个 fixture 的问题，而是"同一事实（声明属于哪个文件）有两个来源且其中一个是错的"。

修正上一轮记录：`REPAIR_LOG.md:7973-7979` 与 `:7983-7984` 说"候选收集路径没有消费 `privateAccessible`、
需先插桩确认 `processClassifiersByName` 各自返回哪个 A"——该判断**已过时**：候选收集现在**已经**调用
`CfirAccessibilityChecker`（`CfirTypeCandidateCollector.kt:67/:96`）。前沿应前移到"符号→文件归属"这一层。

## 3. 官方证据

### 3.1 解析期可见性（v1.0.0，即 `external/cangjie_compiler` 镜像 HEAD）

镜像 HEAD 为 detached `v1.0.0`（`b776b44`，2025-08-08）。

- `src/Sema/LookUpImpl.cpp:454-458` `IsTargetVisibleToNode`：顶层 private 目标必须 `curFile` 相同；
  `:567` 在 `LookupImpl` 中以该谓词过滤；`:653-658` `LookupTopLevel` 走同一实现。
- `src/Sema/PreCheck.cpp:246-251` `privateGlobalInDifferentFile`：GLOBAL+PRIVATE 且不同文件 → **重名豁免**。
  （CFIR 对应实现已存在：`CfirConflictsHelpers.kt:574-581` `isExemptCrossFileTopLevelPrivate`，
  已被 `CfirConflictsHelpers.kt:554` 消费，效果即上一轮记录"`CLASSIFIER_REDECLARATION` 已消失"。）
- 链接期靠文件名 mangling 区分同名 private（`src/Mangle/ASTMangler.cpp:274`、
  `src/Mangle/BaseMangler.cpp:161-171`），说明**同名顶层 private 在语言层是合法的**。

### 3.2 cjc 1.0.5 实测（初测，口径判定见 3.2b）

用 `/c/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc` 双文件同批编译（`--output-type=staticlib`）：

| 变体 | 对应 fixture | 官方诊断 |
|---|---|---|
| 两份 `private class A {}` + public 类内 `private let a: A = A()` | dup01 | **error `sema_export_same_private_decl`**（testa.cj:2:15，note → testb.cj:2:15） |
| `private class A` / `private struct A`，其余同上 | dup02 | **同上 error** |
| `private func f(a: A) {}` | dup03 | **零 error** |

触发条件（上游实现）：`AnalyzeFunctionLinkage` 先由 `AnalyzeLinkageBasedOnModifier` / `ExternalLinkageAnalyzer`
计算 linkage，再遍历 `IsNominalDecl() && PRIVATE && linkage != INTERNAL` 的同名声明报错。
dup01/02 中 `A` 被 public 类的成员类型与构造引用而被迫取得非 INTERNAL linkage，dup03 没有。

### 3.2b SDK 1.0.0 复编译（2026-09-21 补测，判定口径）

`/c/Users/lin17/.cangjie/sdks/cangjie-1.0.0/bin/cjc`（`Cangjie Compiler: 1.0.0 (cjnative)`）双文件同批编译，
与 1.0.5 逐项对照。注意本机 `CANGJIE_HOME` 默认指向 1.0.5，跑 1.0.0 必须显式覆盖
（两者后端模块目录不同：1.0.0 = `modules/windows_x86_64_llvm`，1.0.5 = `modules/windows_x86_64_cjnative`）；
为免污染仓库根，输出用 `-o <probe>\\out` 定向。

| 探针 | SDK 1.0.0 | SDK 1.0.5 |
|---|---|---|
| v1 = dup01（泛型版，两份 `private class A` + public 类内 `private let a: A = A()`） | **0 error**（exit 0） | error `currently, it is not possible to export two private declarations with the same name`（testa.cj:3:15，note→testb.cj:3:15） |
| v2 = dup02（class A / struct A） | **0 error** | 同上 error（note→testb.cj:3:16） |
| v3 = dup03（`private func f(a: A)`） | **0 error**（4 条 unused warning） | 0 error（同 4 条 warning） |
| n1 = 非泛型 dup01 | **0 error** | 同上 error |
| 对照 c_pub（两份 `public class A`） | `redefinition of declaration 'A'` + `ambiguous use of 'A'` | 同 |
| 对照 c_invisible（仅单侧声明、另一侧引用） | `undeclared type name 'A'` + `undeclared identifier 'A'` | 同 |

两点结论：

1. **两组对照证明 1.0.0 前端是活的且在检查同一件事**：同包跨文件同名 **public** 分类器报 `redefinition`，
   跨文件引用 **private** 分类器报 `undeclared type name` —— 与 `LookUpImpl.cpp:454-458` 的
   file-private 规则一致；而两份 **private** 同名声明的 dup01/dup02 在 1.0.0 下**零诊断**。
2. 1.0.0 与 1.0.5 在**全部解析期行为上完全一致**，唯一差异就是 1.0.5 新增的导出冲突检查。
   该诊断文案与 CFIR 的 `EXPORT_SAME_PRIVATE_DECL` 逐字相同
   （"currently, it is not possible to export two private declarations with the same name"），
   印证了 CFIR 该实现是从**更新上游**移植过来的。

⇒ **口径判定：fixture 期望（零诊断）在 pinned 参考版本 SDK 1.0.0 下成立，无需修改 fixture。**
dup01/dup02 与 cjc 的分歧是 1.0.5 的工具链增量，不是语义分歧。

**该检查在镜像 HEAD（v1.0.0）中不存在**：`git grep export_same_private_decl` 只在更新快照
`896235c`（release/1.1 期）命中（`include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:139`、
`src/Sema/CheckFunctionLinkage.cpp:633-645`），且上游源码注释明确自述为**临时 workaround**：

> If has same name private decls are non-internal in one package diag error.
> NOTE: It should be remove when bug of PrivateDecl.ti is fixed.
> The identifier for PrivateDecl.ti does not use the file name for differentiation.

即：这是上游自身 `.ti` 符号名不含文件名的产物，不是语言语义规则。

CFIR 侧现状（值得单独登记为缺陷）：`cfir/checkers/src/.../declaration/CfirGeneralSemanticsChecker.kt:290-329`
`checkExportSamePrivateDecl` 已存在该诊断（`CfirErrors.EXPORT_SAME_PRIVATE_DECL`，
message "currently, it is not possible to export two private declarations with the same name"），
但实现是**逐文件** `for (decl in file.declarations)` 计同名数，且**完全没有 `linkage != INTERNAL` 前置条件**，
而 CFIR 里根本不存在 linkage 模型（`cfir/cfir-tree/src`、`cfir/cfir-common/src` 中无 `linkage` 定义）。
后果：它永远无法覆盖官方那条"同包跨文件 + 非 INTERNAL linkage"的场景，对同文件内同名 private
则会与既有的 REDECLARATION 重复上报。全仓 `testData` 中**没有任何 fixture** 期望该诊断。

## 4. Kotlin 对位

- `external/kotlin/compiler/fir/providers/src/.../fir/scopes/impl/FirPackageMemberScope.kt:37-52`：
  与 CFIR 同形（ClassId 单值 `getClassLikeSymbolByClassId`），**不按可见性过滤**。
- 可见性判定：`external/kotlin/compiler/fir/checkers/src/.../fir/checkers/FirVisibilityChecker.kt:254-277`（Private 分支）
  → `:515-527` `canSeePrivateTopLevelDeclarationFromFile`，`:522` `useSiteFile == declarationContainingFile`
  —— 与 CFIR `CfirAccessibilityChecker:565` **逐字同构**。
- 过滤时机：`external/kotlin/compiler/fir/resolve/src/.../resolve/providers/impl/FirTypeCandidateCollector.kt:35-63`
  `processCandidate` 内 `:39 symbol.isVisible(useSiteFile, ...)`；不可见时**不丢弃候选**，而是降级为
  `K2_VISIBILITY_ERROR` / `ConeVisibilityError`（`:42-46`），再由 applicability 收敛。
  CFIR 的对应实现是**直接丢弃**（`CfirTypeCandidateCollector.kt:66-70/:95-97`）。
- 结构性差异：Kotlin **不设文件专属顶层 scope**（`.../fir/scopes/ImportingScopes.kt:105-115` 里只有
  `FirPackageMemberScope`），把"本文件优先"完全交给 use-site file 比较；CFIR 额外加了
  `CfirFileDeclaredTopLevelScope`，等于在 scope 层引入第二个"file 维度"来源。
  两个 file 维度来源中，**scope 层那个是对的，符号归属那个是错的**。
- Kotlin provider 侧同样用 ClassId 键的 `classifierContainerFileMap`（`FirProviderImpl.kt:152/:227`），
  但 Kotlin 顶层 private 是文件私有、且 `FirPackageMemberScope` 只被用作*候选来源*，
  可见性由 use-site file 判定 —— 结构上没有"两个同名声明同时进同一 ClassId 槽"的语义前提。

## 5. 同族扫描（Fixture Edit Gate 条件 4）

| 同族用例 | 现状 | 说明 |
|---|---|---|
| `Linkage/access01.cj`、`access02.cj`、`access_err.cj` | PASS（上一轮 P1 修过） | 失败的 import 及其级联抑制 |
| `Lookup/MultiFilesPrivate00`（跨文件引用另一文件的顶层 `private var ArrayList`） | PASS（LLT+PSI） | 同一个"跨文件 private 不可见"事实，值/表达式路径已正确；分类器路径未正确 |
| `Lookup/MultiFilesPrivate01` | PASS（空文件） | — |
| `Functionlinkage/*`（5 个 fixture） | PASS | CFIR 的 linkage 组只覆盖 internal 函数的可达性，不涉及 private 导出冲突 |
| `Extend/overload/private_visible0{1,2}`、`class/call_private_ctor_in_other_package` 等 | 未见失败 | — |

结论：**同族中只有"顶层 private 分类器跨文件同名"这一形状失败**，其余 private 可见性用例（值路径）正常。
另注：三个 fixture 的期望值（零诊断）与镜像 v1.0.0 语义一致，不是迁移期凭空写入的。

## 6. 口径判定（原 Escalation，已由 SDK 1.0.0 复编译解决）

技能要求"官方证据在 cjc 与 `external/cangjie_compiler` 之间矛盾时停下问用户"。原矛盾是：

- **cjc 1.0.5**：dup01/dup02 报 `sema_export_same_private_decl`；dup03 零诊断。
- **镜像 HEAD v1.0.0**：无该检查，三者皆零诊断（与 fixture 期望一致）。

**用户指定以 SDK 1.0.0 复编译后矛盾消除（见 §3.2b）**：

1. 1.0.0 与 1.0.5 在全部解析期行为上逐项一致（public 同名 → `redefinition`；跨文件引用 private →
   `undeclared type name`），唯一差异是 1.0.5 新增的导出冲突检查；
2. 1.0.0 下 dup01/dup02/dup03 **全部零 error**，与 fixture 期望和镜像 HEAD 源码三方一致。

**结论：LLT 期望口径 = v1.0.0（零诊断，解析期 file-private 语义），fixture 不改。**
dup01/dup02 与 cjc 1.0.5 的分歧是工具链增量（上游自述的 `PrivateDecl.ti` workaround），不是语义分歧。

CFIR 侧两个随之确定的判断：

- 2.1/2.2 的解析期缺陷是唯一的待修项——现在的 `AMBIGUOUS_USE` / `UNDECLARED_TYPE_NAME`
  在 v1.0.0 口径下是纯错误诊断。
- `CfirGeneralSemanticsChecker.checkExportSamePrivateDecl`（逐文件计数、无 linkage 前置、
  全仓无 fixture 消费）是从 1.0.5+ 上游移植的**孤儿检查**，与 pinned 口径不一致，建议单独评审
  （删除或补齐 linkage 前置——在 CFIR 无 linkage 模型的现状下，前者才是与口径自洽的选择）。
  本轮未动它。

方法学备注（影响后续所有取证）：此前 REPAIR_LOG 的探针基线一直是 cjc 1.0.5，而它比 pinned 镜像
（v1.0.0，`b776b44`）新——凡"官方该报什么"的判断，**必须同时跑 1.0.0 与 1.0.5**，
差异项一律回到镜像源码定位引入提交再裁决，不能只看其一。

## 7. 建议的修复接缝（本轮未实施，仅方向）

按"同一事实单一来源"收敛，而非在候选处加过滤器：

1. **符号归属**：让 class-like 的"声明所在文件"按 **symbol 身份**回答，而不是按 ClassId 折回单值表
   （`CfirProvider.kt:81-82` 的默认路由是根节点；`CfirSourceSymbolProvider` 需要登记每个声明的文件）。
2. **冲突登记**：跨文件 + private 的顶层分类器同名，在**登记进 `nameConflictsTracker` 时**就按官方
   `PreCheck.cpp:246-251` 豁免，复用既有 `isExemptCrossFileTopLevelPrivate` 这一事实来源，
   使类型解析的 `classLikeCandidates` 不再产出"跨文件 private 重声明"候选。
3. 2 条都落地后，testa/testb 各自只剩本文件的 `A` → 三者（含 dup03）在解析期均零诊断。

破坏半径待实测：`classifierMap`/`classifierContainerFileMap` 的写入与读取点、
`nameConflictsTracker.registerClassifierRedeclaration` 的消费点（含 `CfirConflictsHelpers` 与
`CfirDeclarationAvailabilityProvider`），以及 Kotlin 对位中"丢弃 vs 保留候选并标记"这一分歧是否要一并对齐。

## 8. 验证状态

- **cjc 官方取证：已完成**（1.0.0 与 1.0.5 双 SDK 全矩阵，§3.2 / §3.2b；探针与产物在
  `/tmp/cj_priv2/`，输出经 `-o` 定向，仓库根无残留）。
- **Gradle 侧仍未运行**：`.gradle/queue/owner.pid` 持有全局锁（并行会话在跑
  `...$ConstraintCheck.testOptionWithElement01`），且 `build/test-results/test/` 已被其重写，
  本报告 §1 的失败面数字来自 17:00:41 完成的全量快照。
- 因此 2.1/2.2 的根因目前是**静态代码 + 官方双版本对照 + 症状三角验证**，live 路径尚未插桩确认。
  下一轮（拿到 Gradle 后）的确认手段：
  1. 在 `CfirFileDeclaredTopLevelScope.processClassifiersByName`、`CfirPackageMemberScope.processClassifiersByName`、
     `CfirTypeCandidateCollector.candidatesFromScope/collectClassIdCandidates` 打印 symbol 身份
     （`System.identityHashCode`）与其 `getContainingFile()?.name`；
  2. 看 testa/testb 各自看到的候选集合与归属文件，即可闭环；
  3. 修复验证命令（`gradlew-queue.bat`）：`--tests '*TestGenerated$Linkage*' --tests '*TestGenerated$Lookup*' --continue`，
     全绿后跑全量回归（同时盯 `Lookup/MultiFilesPrivate00` 与 `Linkage/access*` 不得回归）。
