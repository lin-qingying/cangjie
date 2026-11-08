# RealProjectAnalysisTest / mariadb 错误整理（已闭环）

日期：2026-11-05（第二轮：完成两项「待确认」的实证）

数据源
- 主报告：`analysis/analysis-real-project-test/build/real-projects/reports/mariadb-master/cfir.json`（RUN-A）
- 对照报告：同路径 RUN-B（不挂载项目 `.cjo`），关键数字见 §2.2
- 探针日志：`build/real-projects/cjo-probe-runA.log`、`cjo-probe.log`、`conflict-probe.log`
- 检出：`build/real-projects/checkout/mariadb-master`（`cjpm clean/build` exitCode=0）

> 本轮为确认结论临时插桩 4 个文件，**已全部还原**（`RealProjectSession.kt` / `build.gradle.kts` /
> `CfirSourceSymbolProvider.kt` / `CfirConflictsHelpers.kt`），并用
> `:cfir:checkers:compileKotlin :analysis:analysis-real-project-test:compileTestFixturesKotlin` 验证可编译。

---

## 1. 总账（RUN-A，现状）

| 指标 | 数值 |
|---|---|
| 源文件 | 49（`src/cdbc` 12、`src/connection` 14、`src/protocol` 22、`package.cj` 1） |
| `projectPackageCount` | 2（实际 3 个非空包） |
| `dependencyCjoCount` | 2（`target/release/mariadb/` 下共 4 个 `.cjo`） |
| ERROR / WARNING | 545 / 65（合计 610） |
| 文件级异常 | 0 |
| 被隔离的分析期异常 | 69（16 个文件） |

`cjpm` 编译通过，故上述全部为本仓 CFIR 与官方编译器的分歧。

---

## 2. 主因（已实证）：`protected package` 漏过滤 → `.cjo` 搜索根退化为整个产物目录 → 源码与库各一份同名符号

### 2.1 探针 A：谁被挂载（`cjo-probe`，实跑输出）

```
[cjo-probe] filter=mariadb sourcePackages=[mariadb.cdbc, mariadb] dropAll=false
[cjo-probe] mariadb.cdbc.cjo       packageFqName=mariadb.cdbc        own=true  mounted=false
[cjo-probe] mariadb.cjo            packageFqName=mariadb             own=true  mounted=false
[cjo-probe] mariadb.connection.cjo packageFqName=mariadb.connection  own=false mounted=true
[cjo-probe] mariadb.protocol.cjo   packageFqName=mariadb.protocol    own=false mounted=true
```

- `sourcePackages` 只有 `{mariadb, mariadb.cdbc}` —— `src/connection/*.cj` 与 `src/protocol/*.cj`
  全部写作 `protected package mariadb.connection|mariadb.protocol`，
  `CjpmProjectModel.PACKAGE_DECLARATION = (?m)^\s*package\s+([\w.]+)` **不匹配修饰符前缀**，两包从未入选。
- 结果：这两个包的 `.cjo` 被当作「依赖」挂进 `CaStandaloneLibraryModule`（`dependencyCjoCount=2`）。
- `mariadb.cdbc.cjo` / `mariadb.cjo` 确实被正确过滤 —— **第一轮「cdbc 被重复也由 cdbc.cjo 造成」的推测不成立**，见 2.3。

### 2.2 对照实验：去掉 cjo 挂载后错误塌缩

同一份代码、同一 SDK、同一提交，只切换「是否挂载项目 `.cjo`」（临时 gradle 开关）：

| 诊断 | RUN-A（现状，挂载 2 个） | RUN-B（`dropCjo`，0 个） |
|---|---|---|
| ERROR 合计 | **545** | **48** |
| 被隔离异常 | 69 | 3 |
| `AMBIGUOUS_USE` | 258 | 0 |
| `CLASSIFIER_REDECLARATION` | 74 | 0 |
| `EXTEND_MEMBER_CANNOT_SHADOW` | 46 | 0 |
| `AMBIGUOUS_CONSTRUCTOR_CALL` / `AMBIGUOUS_FUNCTION_CALL` | 22 / 8 | 0 / 0 |
| `THROW_EXPR_WITH_WRONG_TYPE` | 18 | 0 |
| `NOT_MEMBER_OF` | 14 | 0 |
| `NO_MATCH_FUNCTION_DECLARATION_FOR_CALL` | 3 | 0 |
| `UNRESOLVED_REFERENCE` | 87 | 28 |
| `TYPE_MISMATCH` | 0 | 7 |
| `WRONG_NUMBER_OF_ARGUMENTS` | 6 | 6 |
| `REF_NOT_BE_TYPE` | 3 | 3 |
| `USE_EXPR_WITHOUT_IMPORT` | 2 | 2 |
| `ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED` | 2 | 2 |

⇒ 497/545 的 ERROR 由「源码 + 库双份符号」引发，与项目代码无关。

### 2.3 探针 B/C：重声明的产生点

**探针 B**（`CfirSourceSymbolProvider.recordClassLikeClassifier` 的重登记分支）：**0 行命中**。
→ 74 条 `CLASSIFIER_REDECLARATION` 不是 provider 侧 `ClassId` 二次登记产生。

**探针 C**（`CfirConflictsHelpers.collectTopLevelConflict` 的命中点）：**74 行，全部同形**：

```
[conflict-probe] decl=ColumnInfoImpl conflictingOrigin=Library declFile=column_info.cj
                 conflictingFile=null declPkg=mariadb.cdbc conflictingPackage=null
```

- `conflictingOrigin=Library` 74/74 —— 冲突方全部是**库符号**，且 `conflictingFile=null`（库符号无 PSI 文件）。
- 包括 `mariadb.cdbc` 包的 9 条，说明库侧**能查到 `mariadb.cdbc` 包**，尽管该 `.cjo` 未被显式挂载。

**为什么库能查到 cdbc**：`.cjo` 内的符号名计数（二进制检索）

| 类名 | mariadb.cdbc.cjo | mariadb.connection.cjo | mariadb.protocol.cjo | 是否被报重声明 |
|---|---|---|---|---|
| `ColumnInfoImpl` | 42 | 0 | 0 | 是 |
| `ConnectionImpl` | 70 | 0 | 0 | 是 |
| `MariaConnection` | 2 | 76 | 0 | 是 |
| `TransactionImpl` | **0** | 0 | 0 | **否** |
| `DriverTest` | **0** | 0 | 0 | **否** |

74 个被报重声明的名字，全部能在某个 `.cjo` 里找到；唯一 2 个未报的（`TransactionImpl`、`DriverTest`）
在 4 个 `.cjo` 中出现 **0 次**（cjc 未导出）。这与「76 个顶层分类器 → 74 条重声明」精确吻合。

**机制链（代码级）**

1. `RealProjectSession.collectDependencyCjoItems`：正则漏 `protected package` → 只过滤 2 个 `.cjo`。
2. `LLBinaryOriginLibrarySymbolProviderFactory.createDeserializedLibrarySymbolProvider`
   （`analysis/low-level-api-cfir/.../LLBinaryOriginLibrarySymbolProviderFactory.kt:82-86`）：
   ```kotlin
   .mapNotNull(VirtualFile::asCjoSearchRoot)
   ```
   而 `asCjoSearchRoot()` = `normalizeCjoSearchRoot(File(path)).takeIf(File::isDirectory)`，
   `normalizeCjoSearchRoot`（`DecompiledCjoRepository.kt:16`）对**文件**取 `parentFile`。
   ⇒ 挂载的「2 个 `.cjo` 文件」被归一化成**目录** `target/release/mariadb/`，
   `CjoSearchPath(additionalLibrarySearchPaths=[该目录])` 让目录内 **4 个 `.cjo` 全部可解析**，
   包括未被挂载的 `mariadb.cdbc.cjo` 与 `mariadb.cjo`。
3. `CfirSourceSymbolProvider.loadTargetClassLikeSymbols`（:584-594）：
   ```kotlin
   addAll(state.classifierCandidates[classId].orEmpty()...)
   for (provider in delegatedSymbolProviders()) {
       addAll(provider.getImportNamespace(target.namespace).classifiers(target.name))  // 库侧同名符号
   }
   }.distinct()
   ```
   源码候选与库侧候选并用**引用相等**去重 → 库侧是另一实例，保留为第二候选。
4. `CfirConflictsDeclarationChecker.checkFile` → `collectTopLevel`（`CfirConflictsHelpers.kt:187`）
   → `packageMemberScope.processClassifiersByName` → `collectTopLevelConflict`：
   库符号未绑定，跳过 module 一致性比较（:562-571）→ 判定冲突 → `CLASSIFIER_REDECLARATION`。
   同一双份符号也污染 `CfirPackageScope` → 类型解析得双候选 → `AMBIGUOUS_USE` /
   `AMBIGUOUS_CONSTRUCTOR_CALL` / `AMBIGUOUS_FUNCTION_CALL`。

### 2.4 修复接缝（两处，缺一不可）

1. **必修**：`CjpmProjectModel.PACKAGE_DECLARATION` 支持修饰符前缀
   ```kotlin
   Regex("""(?m)^\s*(?:(?:public|protected|internal|private)\s+)?package\s+([A-Za-z_][A-Za-z0-9_.]*)""")
   ```
   实测 `sourcePackages` 将变为 3 个包，`dependencyCjoCount` 归 0，错误回落至 RUN-B 的 48。
2. **加固**：`asCjoSearchRoot` 把单文件 root 扩成整目录是设计使然（同目录 `.cjo` 常需互相依赖），
   因此**只要项目自身产物与真实外部依赖同目录，就会被误当依赖**。建议在库侧按包名过滤：
   `CaStandaloneLibraryModule` 的包集合中剔除与源码模块同名的包，或 `CjoManager` 加载时跳过
   `packageFqName ∈ sourceModule.packages` 的 `.cjo`。否则任何多包项目都会重演本问题。

---

## 3. RUN-B（干净模块图）剩余的 48 个 ERROR —— 这些才是真实缺口

### E1. 枚举项解析（28 条 `UNRESOLVED_REFERENCE`，单一根因）

全部是「同包/导入枚举的构造器名当值使用」时解析不到：

| 位置 | 缺失名 | 上下文 |
|---|---|---|
| `src/protocol/client_command.cj:39/55/74/92/107/127` | `Query`、`StmtPrepare`、`StmtFetch`、`StmtClose`、`StmtReset`、`StmtSendLongData` | `super(Query)` 等，`CommandType` 枚举项 |
| `src/protocol/client_command_utility.cj` 7 处 | `Quit`、`InitDB`、`Statistics`、`Debug`、`Ping`、`ResetConnection`、`SetOption` | 同上 |
| `src/protocol/client_command_execute.cj` 5 处 | `ParameterCountAvailable`、`QueryAttributes`、`ReadOnly`、`NoCursor` | `Capability` / `CursorType` 枚举项 |
| `src/protocol/client_handshake_response.cj` 5 处 | `PluginAuthLenencClientData`、`ConnectWithDB`、`PluginAuth`、`ConnectAttrs`、`ZSTDCompressionAlgorithm` | `hasCapability(...)` 实参 |
| `src/connection/maria_connection_factory.cj:50/130` | `SSL`、`ConnectWithDB` | 同上 |
| `src/cdbc/column_info.cj:36/42`、`src/protocol/maria_binary.cj:363` | `Unsigned`、`NotNull` | `definition.hasFlag(Unsigned)`，`ColumnFlag` 枚举项 |

判断：包/导入作用域的索引未覆盖 **enum constructor**（值位置）。与 §2 无关，属独立缺口，且是当前最大的一块。

### E2. 类型名与枚举项同名时优先按类型解析（3 `REF_NOT_BE_TYPE` + 5 连带 `TYPE_MISMATCH`）

```
src/protocol/column_type.cj:88  case 0xFE => String        → type name cannot be used as an expression
src/protocol/enums.cj:69        case 0xFF => Error         → 同上
src/protocol/enums.cj:71        case 0x01 => AuthMoreData  → 同上
```
`String` / `Error` / `AuthMoreData` 既可能是类型名也是枚举项名，CFIR 只按类型解析、无「值位置回退到同类枚举项」。
与 E1 同源（枚举项未进值作用域），可一并修。

### E3. `WRONG_NUMBER_OF_ARGUMENTS` 6 —— `stdx.crypto.digest` 成员调用

```
src/connection/plugin.cj:33/37/42   sha256.finish()
src/connection/plugin.cj:74/77/81   sha1.finish()
```
`finish` 被当成「函数值调用」而非 `Digest` 成员方法 → stdx `.cjo` 反序列化的接口成员索引或重载解析问题。
独立于 §2（RUN-B 完全相同），需单独立项。

### E4. `USE_EXPR_WITHOUT_IMPORT` 2 —— 与官方不一致

```
src/cdbc/transaction.cj:59/81  synchronized(connectionMutex()) { ... }
```
`cjpm build` 通过，说明 `synchronized` 不需要 `import std.sync`；CFIR 该检查过严（误报）。

### E5. `ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED` 2

```
src/protocol/server_result.cj:116  Array<?Array<Byte>>(columnCount, repeat: [])
```
`repeat:` 的空字面量未按元素类型 `?Array<Byte>` 推断。

### E6. `TYPE_MISMATCH` 1（RUN-B 独有）

```
src/connection/maria_connection_factory.cj:144  connection.socket = tls
  inferred 'stdx/net/tls/TlsSocket', but 'std/net/...'
```
socket 字段类型被解析成具体实现而非 `std.net` 接口/父类型，需单独查 socket 字段声明与 stdx TlsSocket 的继承关系。

### E7. 警告 18（RUN-B）

`DEPRECATION_OVERRIDE_WARNING` 10 = **5 对重复上报**（`prepare_statement_client.cj:112/123`、
`prepare_statement_server.cj:121/132`、`query_result.cj:50` 各 2 次）、`DEPRECATED_WARNING` 5、`UNUSED_IMPORT` 3。
重复上报在 RUN-B 仍存在 → **与双份符号无关，是 checker 自身的重复上报缺陷**（修正第一轮把 28 条重复全归因于双包的说法）。

---

## 4. 分析期被隔离的异常（69 → 3）

| 家族 | RUN-A | RUN-B | 抛出点 |
|---|---|---|---|
| F1 `Expected CfirNamedReference or CfirErrorNamedReference but CfirNamedReferenceImpl found`（deprecated 检查触发 lazy resolve → `checkDelegatedConstructorIsResolved`） | 29 | 0 | `cfirCheckResolvedUtils.kt:451` |
| F2 `Diagnostic CjOffsetsOnlyDiagnosticWithParameters1 has no PSI-backed source`（reporter 直接 `error()`） | 40 | 3 | `LLCfirDiagnosticReporter.kt:101` |

- F1 全部由双份符号诱发（RUN-B 归零），但**接口判定本身仍是错的**：`CfirNamedReferenceImpl` 未实现被检查的接口
  → 属真实缺陷，只是当前被 §2 掩盖。
- F2 剩 3 条（全部在 `src/protocol/server_auth_more_data.cj`，路径为 declaration checkers）与 cjo 无关，
  需单独修：无 PSI 源的诊断应丢弃或降级到容器 PSI。
- 注意 F2 另外 37 条会在 §2 修复后消失 —— 修 F2 前应先修 §2，否则会被残留噪声误导。

---

## 5. 结论与建议顺序

1. **修 §2.4-1（正则）** → ERROR 545→48，异常 69→3。这是本次唯一的「环境/装配缺陷」。
2. **加 §2.4-2（库侧包名过滤）** → 防止其它多包项目重演。
3. 修 **E1+E2（枚举项进值作用域）** → 可消掉 28+3（+5 连带）条，是剩余最大一块。
4. 修 **F1（`checkReferenceIsResolved` 接口判定）** 与 **F2（reporter 不抛异常）**。
5. 逐项处理 E3（stdx digest 成员）、E4（synchronized 误报）、E5（repeat 空字面量）、E6（socket 类型）、E7（重复上报）。

复跑命令（还原后可直接用）：
```bash
gradlew-queue.bat :analysis:analysis-real-project-test:test \
  --tests "*RealProjectAnalysisTest*" -PrealProjectFilter=mariadb
```

---

## 6. 2026-11-06 修复进展（cfir → analysis → 真实项目，三层递进）

约定：每个错误族先在 cfir 写复现并确认根因，再在 analysis 写用例验证，两层都过才回真实项目核对数量。

### 6.1 §2.4-1 装配缺陷（analysis 层）已修

- 根因：`CjpmProjectModel.PACKAGE_DECLARATION = (?m)^\s*package\s+...` 不匹配 `protected package`，
  `sourcePackages` 只认出 2 个包 → 项目自身 `.cjo` 被当外部依赖挂载。
- 修复：正则接受 `public|protected|internal|private` 修饰符前缀。
- analysis 用例：`CjpmPackageDeclarationParsingTest`（5 个，全绿；覆盖修饰符、制表符、注释行、行尾注释）。
- 实测：`projectPackageCount` 2→**4**（原文档预测 3，实际含 `mariadb.cdbc` 共 4 个非空包）、
  `dependencyCjoCount` 2→**0**、ERROR **545→48**、隔离异常 **69→3**、文件级异常 0。

### 6.2 E1 裸枚举构造器（28 条 `UNRESOLVED_REFERENCE`）已修

- **cfir 层结论：核心作用域/解析没有缺陷。**
  新增 `cfir/analysis-tests/testData/diagnostics2/enum/enumConstructorValueScope.cj`
  （`protected package` + 带成员函数的 `protected enum` + `Command <: Request` 三层构造链
  `super(Query)` + `if (hasCapability(SSL))` + `return Quit`）→ **0 诊断，通过**。
  另有 `cfir/analysis-tests/testData/diagnostics2/modifier/packageAccessModifier.cj`
  固化包修饰符语义（`public|protected|internal package` 合法、`private package` → `WRONG_MODIFIER_TARGET`）。
- **真正缺口在 analysis 层源码符号 provider**：
  `LLCangJieSourceSymbolProvider.getTopLevelCallableSymbols(To)` 只收集 `CfirFile` 顶层函数/属性
  和 pattern binding，漏掉 enum 声明里的 `CfirEnumConstructor`；而已编译库侧
  `LLCangJieStubBasedLibrarySymbolProvider` 早已按包级 callable 暴露枚举项（`enumConstructorCache`）。
  于是**只有同包源码枚举项解析不到**——正是 RUN-B 的现象。
  官方依据：`external/cangjie_compiler/src/Sema/TypeCheckCall.cpp:223`
  `CalculateEnumCtorScopeLevel`（enum 限定位之外的构造器按 global scope level 参与查找）。
- 修复：`LLCangJieSourceSymbolProvider` 增加 `enumConstructorCache` +
  `loadEnumConstructorsByCallableId`，在两个包级 callable 入口追加枚举项符号；
  符号身份仍取所属 enum 的 CFIR 声明（不新建第二份）。
- analysis 用例：`SourcePostSemaDiagnosticsTest.useCrossFileEnumConstructor`
  （数据 `analysis/low-level-api-cfir/testData/diagnostics/postSema/useCrossFileEnumConstructor.cj`）。
  修复前失败：`getTopLevelCallableSymbols(test, Query) == []`；
  修复后通过；`SourcePostSemaDiagnosticsTest` 18/18 通过。
- 实测：ERROR 48 → **20**（`UNRESOLVED_REFERENCE` 28 → **0**），隔离异常仍 3。
- 回归：`gradlew-queue.bat :analysis:low-level-api-cfir:test`（全模块）62 个 suite / 184 个用例，0 失败。

### 6.3 E2 枚举项名与类型名同名（9 条：`REF_NOT_BE_TYPE` 3 + 连带 `TYPE_MISMATCH` 6）已修

- **cfir 复现**：`cfir/analysis-tests/testData/diagnostics2/enum/enumEntryShadowedByTypeName.cj`
  （`enum ResponseCode { | Ok | AuthMoreData | Error }` + 同包 `class AuthMoreData` +
  `enum ColumnType { | Tiny | String }`，值位置出现 `case 0xFF => Error` /
  `case 0x01 => AuthMoreData` / `case 0xFE => String`）。
  修复前实测：三者都报 `REF_NOT_BE_TYPE, TYPE_MISMATCH`，与真实项目完全一致。
- **根因**：`CfirCallResolver` 的值位置解析把 classifier 兜底放在 enum value 分支之前，
  于是同名类型（含默认导入的 `std.core.String` / `std.core.Error`）先被命中，
  零 payload 枚举项永远拿不到机会。
- 官方规则：`Sema/EnumSugarTargetsFinder.cpp` 的 `RefineTargets`：ref 带 target type 且不是
  call/pattern 时，只保留 owner enum 与 target type 一致的枚举项（`RefineTargetTy`，含
  `Option` 自动装箱）；没有 target type 时按 `IsSamePackage` 优先本包枚举项。
- 修复：`CfirCallResolver` 增加 `enumConstructorOutranksClassifier`，在 classifier 兜底前判断
  「同包（或有 target type 时同 owner enum）的零 payload 枚举项」是否存在，存在则不落回 classifier，
  继续走既有 enum value 分支（不新增第二套解析）。
- analysis 用例：`SourcePostSemaDiagnosticsTest.enumEntryShadowedByTypeName`
  （数据 `analysis/low-level-api-cfir/testData/diagnostics/postSema/enumEntryShadowedByTypeName.cj`）；
  `SourcePostSemaDiagnosticsTest` 19/19 通过。
- 回归：`:cfir:analysis-tests:test --tests "*CfirAnalysisDiagnostics2TestGenerated*"` 全绿。
- 实测：ERROR 20 → **11**（`REF_NOT_BE_TYPE` 3 → 0，`TYPE_MISMATCH` 7 → 1）。

### 6.4 当前剩余 11 条 ERROR（下一批目标）

| 族 | 诊断 | 条数 | 位置与形态 |
|---|---|---|---|
| E3 | `WRONG_NUMBER_OF_ARGUMENTS` | 6 | `plugin.cj:33/37/42/74/77/81` `sha256.finish()` / `sha1.finish()`：stdx `Digest` 成员被当成函数值调用 |
| E4 | `USE_EXPR_WITHOUT_IMPORT` | 2 | `transaction.cj:59/81` `synchronized(...)`：误报（cjc 无需 `import std.sync`） |
| E5 | `ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED` | 2 | `server_result.cj:116` `Array<?Array<Byte>>(columnCount, repeat: [])` |
| E6 | `TYPE_MISMATCH` | 1 | `maria_connection_factory.cj:144` `connection.socket = tls`：socket 字段解析成 `std/net/StreamingSocket` |
| F2 | 被隔离异常 | 3 | `server_auth_more_data.cj`：无 PSI 源诊断在 `LLCfirDiagnosticReporter` 直接 `error()` |

警告 18 条未变：`DEPRECATION_OVERRIDE_WARNING` 10（= 5 对重复上报）、`DEPRECATED_WARNING` 5、`UNUSED_IMPORT` 3。

### 6.5 E3 `sha1/sha256.finish()`（6 条 `WRONG_NUMBER_OF_ARGUMENTS`）—— 复现已缩小，根因待定

已确认的排除项（都不复现）：

| 复现尝试 | 位置 | 结果 |
|---|---|---|
| 源码接口 + struct 各实现 `finish()`/`finish(to!)` | `cfir/analysis-tests/testData/diagnostics2/calls/digestMemberCall.cj` | 通过（0 诊断） |
| SDK 库接口 `std.crypto.digest.Digest`（同包） | `.../calls/digestLibraryMemberCall.cj` | `digest.finish()` 通过；`digest.finish(16)` 报 `NEED_NAMED_ARGUMENT`（官方该参数是命名参数 `to!`） |
| 自建单包 `.cjo` 夹具（接口 + struct 同名重载） | `testData/libraries/digest_overload` + `testData/llt/library_member_call/library_digest_overload.cj` | 通过 |
| 自建跨包 `.cjo` 夹具（接口在包 A、struct 在包 B） | `testData/libraries/digest_cross_pkg` + `.../llt/library_member_call/library_digest_cross_package.cj` | 通过 |

在**真实 stdx 环境**下用最小 cjpm 探针工程复现成功（1 条诊断）：

```
ERROR | main.cj:8 | var d: Array<Byte> = sha256.finish() | Wrong number of arguments for function value call.
```

同一工程内 `sha256.finish(to: 16)`（命名实参）与接口类型 `d.finish()` 都不报错
⇒ 缺陷限定于「**具体库类型 + 零参调用**」，而不是「库侧同名重载」或「接口方法查找」本身。

探针工程（不提交，`.dsh-tmp/` 下）：`.dsh-tmp/probe-digest`（cjpm 项目，`import stdx.crypto.digest.*`）
+ `.dsh-tmp/probe.properties`，复跑：

```bash
gradlew-queue.bat :analysis:analysis-real-project-test:test \
  --tests "*RealProjectAnalysisTest*" \
  -PrealProjectConfig=D:/code/intellij/cangjie/.dsh-tmp/probe.properties \
  -PrealProjectFilter=probedigest --no-build-cache
```

下一步：把「具体库类型 + 零参调用」落到 CFIR 的候选/CallKind 上（`CfirMapArguments` 的 stderr 探针未进入
测试 XML 输出，改用断言式单测或 `System.out` 落文件），确认是落到 `isCallableValueCall` 还是
`CallKind.Function` 的空候选路径，再决定修法。

### 6.6 E3 追加实测（2026-11-06 第二轮）

- 追加排除：接口给出 `finish()` 默认实现、实现类型只写 `finish(to!: Int64)`
  （夹具 `testData/libraries/digest_default_impl` + `.../llt/library_member_call/library_digest_default_impl.cj`）→ **通过**。
- 真实 stdx 探针（探针工程 `.dsh-tmp/probe-digest`）确认的调用形态：
  - `sha256.finish()`、`SHA256().finish()`、显式类型 `var sha256: SHA256` 三种接收者形态 **全部**报
    `WRONG_NUMBER_OF_ARGUMENTS`；
  - `sha256.finish(to: 16)`（命名实参）**通过**；
  - `var f: () -> Array<Byte> = sha256.finish`（函数值）**通过** —— 且官方 cjc 对同一表达式给出的函数类型是
    `() -> Struct-Array<UInt8>`，说明库侧确实存在真实的零参 `finish()`。
- CFIR 侧候选实测（用文件日志替代 stderr，已还原）：`sha256.finish()` 走了 `CallKind.Function` 普通调用路径，
  并同时创建了 **两个**候选：
  ```
  CfirNamedFunctionSymbol(finish) params=0[]          kind=Function
  CfirNamedFunctionSymbol(finish) params=1[to:true]   kind=Function
  ```
  ⇒ 零参候选存在且参数列表为空，最终却以「函数值调用参数个数不符」报错，说明**零参候选在后续 stage 被淘汰**，
  而不是候选缺失或索引问题。
- 仍未定位：零参候选被淘汰的具体 stage（`acceptCandidates` 日志未出现在该路径，说明调用走的是另一条收尾路径）。
  下一轮从 `CandidateFactory`/`CfirCompleteCandidate` 一侧按符号名过滤打点，或直接对该候选断言
  `applicability` 与 `diagnostics`。

### 6.7 E3 根因确认（2026-11-06 第三轮）：不是重载解析，而是 CJO 包依赖加载失败

用文件日志（未走测试 XML 的 stderr）在 `CfirCandidateCollector.consumeCandidate` 与
`CfirCallResolver.reduceCandidates` 打点，`sha256.finish()` 一个调用点的实测：

```
bestCandidates = [0p:app=RESOLVED:origin=Library:owner=stdx/crypto/digest/SHA256:
                     retTy=ERROR TYPE: Cannot resolve struct FullId: pkgId=0
                                    (Cannot load imported package at index 0)]
allGroup      = [上面这条, 1p:app=INAPPLICABLE_ARGUMENTS_MAPPING_ERROR:
                     origin=Library:owner=stdx/crypto/digest/SHA256:retTy=Unit]
```

结论链：

1. 零参 `finish()` 候选**本身是 RESOLVED**（参数映射、接收者都正确），唯一的问题是它的
   **返回类型退化成 `ConeErrorType`**，错误信息指向
   `Cannot load imported package at index 0` —— 即 `stdx.crypto.digest.cjo` 包头依赖表第 0 项
   （`std.crypto.digest`）**没有被加载**。
2. `reduceInvalidReturnTypeOverloads` 因此把「返回类型有效」的单参 `finish(to!)` 留下、
   把零参候选当作坏返回类型淘汰；单参候选参数个数不符 → 最终报
   `WRONG_NUMBER_OF_ARGUMENTS`。
3. 所以 **arity 诊断只是级联**，真正缺陷在「挂载的外部 `.cjo` 跨包引用无法解析到 SDK 的 std 包」。
   同一机制很可能同时解释 **E6**（stdx `TlsSocket` 被判成 `std/net/StreamingSocket` 之外的独立类型）。

下一轮动作：查 `CjoSearchPath` / `CjoManager` 对 mounted `.cjo` 的依赖解析——把 SDK stdlib/依赖包
根一并纳入搜索路径（builtins 用的同一根），使 `pkgId=0` 可加载；修好后 E3 与 E6 可能一起归零，
再回到 `:analysis:analysis-real-project-test:test -PrealProjectFilter=mariadb` 复核。

### 6.8 E3 + E6 已修（2026-11-06 第四轮）：库 provider 的 `.cjo` 依赖搜索根

- **根因（analysis 层装配）**：`LLBinaryOriginLibrarySymbolProviderFactory.createDeserializedLibrarySymbolProvider`
  只把库模块的 binary roots 传给 `CjoSearchPath`，**没有 builtins（SDK stdlib）根**，且不回退环境变量。
  于是库 `.cjo` 包头依赖表里的 `std.*` 包（`stdx.crypto.digest` → `std.crypto.digest`）加载失败，
  跨包引用退化成 `ConeErrorType`，进而触发 `reduceInvalidReturnTypeOverloads` 淘汰正确候选。
- **修复**：
  1. `CfirDeserializedSymbolProvider` 增加 `dependencyCjoManager`（默认等于 `cjoManager`），
     反序列化上下文中的**导入包解析**改用它；
  2. `LLBinaryOriginLibrarySymbolProviderFactory` 传两套搜索根：暴露面仍只含库模块 roots
     （否则 SDK std 包会被重复暴露，`Array`/`Byte` 报 `ambiguous use`），
     依赖解析面 = 库 roots + builtins roots。
- **三层验证**：
  - cfir：`pkgId=0` 机制在 `CjoFullIdResolver` 侧按搜索根工作，无需改 cfir 解析逻辑（本次改动为 provider 装配）；
  - analysis：探针工程（`import stdx.crypto.digest.*` + `SHA256().finish()`）实测
    `ERROR 1 → 0`、`collectionFailures 1 → 0`；中途出现的 `ambiguous use of 'Array'/'Byte'`
    正是「暴露面错误地包含 builtins」的证据，据此收敛为双 manager 方案；
  - 真实项目：ERROR **11 → 4**（`WRONG_NUMBER_OF_ARGUMENTS` 6 → 0，**E6 的 `TYPE_MISMATCH` 1 → 0**，
    与 E3 同源），隔离异常仍 3。
- 结论：E6 确认为 E3 同源（stdx `TlsSocket` 的类型参数 `std.net` 包同样加载失败）。

回归与用例：

| 层 | 用例 / 命令 | 实测 |
|---|---|---|
| analysis（真实 stdx 探针） | `.dsh-tmp/probe-digest`（`import stdx.crypto.digest.*` + `SHA256().finish()`） | 修复前 `ERROR 1 / collectionFailures 1`，修复后 **0 / 0** |
| analysis（LL 套件） | `:analysis:low-level-api-cfir:test` | 62 suite / **185 用例 / 0 失败** |
| cfir（序列化） | `:cfir:cfir-serialization:test` | 20 suite / **121 用例 / 0 失败** |
| 真实项目 | `-PrealProjectFilter=mariadb` | `ERROR 11 → 4`、隔离异常 3 |

补充用例：新增 `SourcePostSemaDiagnosticsTest.libraryMemberOverloadCall`
（`analysis/low-level-api-cfir/testData/diagnostics/postSema/libraryMemberOverloadCall.cj`，
库模块 + 零参/命名参数重载成员调用）。实测该用例在**打点前后都通过**——LL 测试宿主的
库模块搜索根由测试框架另行装配，无法复现「库 provider 缺 builtins 根」这一装配缺陷，
因此它只作为库成员重载调用的常规覆盖；E3 的缺陷复现仍以真实 stdx 探针工程为准。

### 6.9 E4 `synchronized` 误报（2 条 `USE_EXPR_WITHOUT_IMPORT`）已修

- **cfir 复现**：`cfir/analysis-tests/testData/diagnostics2/general/synchronizedLockFromSiblingImport.cj`
  （同包 `lockHolder.cj` `import std.sync.*` + `syncUse.cj` 无 import 直接 `synchronized(sharedLock)`）。
  修复前实测：`<!USE_EXPR_WITHOUT_IMPORT!>sharedLock<!>`，与 mariadb `transaction.cj` 完全一致。
- **官方语义（实测确认）**：`external/.../Sema/TypeCheckExpr/SynchronizedExpr.cpp:23`
  走 `ImportManager::GetSyncDecl("Lock")` → `GetImportedDecl(SYNC_PACKAGE_NAME, "Lock")` →
  `cjoManager->GetPackageMembersByName("std.sync", "Lock")`。也就是说判定的是**编译单元是否加载了
  `std.sync` 包**（包级 import 并集），不是当前文件的 import 列表。
  用 cjc 1.1.3 复验：单文件包无任何 `std.sync` import 时 `synchronized(())` 报
  `import 'sync' to use the 'synchronized' expression`（与现有 `llt/ErrMsgs/sync_0.cj` 期望一致）；
  而 mariadb 的 `mariadb.cdbc` 包里 `connection.cj` 导入过 `std.sync`，因此 `transaction.cj` 通过。
- **修复**：`CfirExpressionImportChecker.checkSynchronizedImport` 在「当前文件未 import」之后，
  再用**锁对象静态类型**判定：类型声明在 `std.sync` 包内（如 `Mutex`/`ReentrantMutex`）时
  说明该编译单元必然已加载 `std.sync`，不报该诊断。`quote` 的同类检查保持文件级（官方
  `ChkIfImportLibAST` 查的是当前文件的 imported packages，语义不同）。
- **三层验证**：
  - cfir：新用例通过；`llt/ErrMsgs/sync_0.cj`、`llt/synchronized/*` 仍通过（未导入且锁类型非 std.sync → 继续报）。
  - analysis：`SourcePostSemaDiagnosticsTest.synchronizedLockFromSiblingImport` 通过。
  - 真实项目：ERROR **4 → 2**（`USE_EXPR_WITHOUT_IMPORT` 2 → 0），隔离异常仍 3。

回归：`:cfir:analysis-tests:test --tests "*CfirAnalysisDiagnostics2TestGenerated*"`（57 suite / 387 用例）
与 `:analysis:low-level-api-cfir:test`（62 suite / 187 用例）全绿。

顺带记录一个**独立待查项**（与本轮目标无关，不阻塞）：SDK `std.crypto.digest.Digest.finish(to!: Int64)`
在 CFIR 里以命名参数调用时报 `ARGUMENT_TYPE_MISMATCH`（`digestLibraryMemberCall.cj` 已改为只覆盖零参调用，
并在文件头注明）。










### 6.10 E5 `repeat: []` 空字面量（2 条 `ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED`）已修

- **cfir 复现**：`cfir/analysis-tests/testData/diagnostics2/inference/repeatEmptyArrayLiteral.cj`
  （`Array<?Array<Byte>>(n, repeat: [])` 与 `Array<Array<Byte>>(n, repeat: [])` 对照）。
  修复前前者在 `repeat: []` 上报 `ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED`，后者通过——说明问题
  只在**元素类型是 `Option<T>`**时出现。用 cjc 1.1.3 复验同一形态编译通过（0 诊断）。
- **根因**：`ConeCangJieType.arrayLiteralElementType` 对 `Option<Array<Byte>>` 直接取
  `arrayElementType`，`Option` 不是数组 → 元素类型为 null → 空字面量报「无法推断」。
  官方 `ChkArrayLit` 在 `Option` 自动装箱后按解包类型检查数组字面量。
- **修复**：`arrayLiteralElementType` 增加 `Option` 解包（`optionElementType`）后再取元素类型。
- **三层验证**：cfir 新用例通过；analysis `SourcePostSemaDiagnosticsTest.repeatEmptyArrayLiteral`
  通过；真实项目 ERROR **2 → 0**。

### 6.11 F2 被隔离异常（3 条）已修

- **实际异常**：`CfirRecursiveConstructorCallChecker` 沿 `super(...)` 走到**其它文件**里的父类构造器时，
  读取仍是 lazy 的 `CfirLazyBlock.statements` → `IllegalStateException: CfirLazyBlock should be resolved
  before accessing` → `Exception in declaration checkers` → 整份文件的声明检查被隔离
  （`server_auth_more_data.cj` 的 3 条）。
- **修复**：`CfirRecursiveConstructorCallChecker` 增加 `resolvedBodyOrNull()`：
  `CfirLazyBlock` 视为「体未解析」，按「没有显式构造委托」处理，不再抛异常。
- **附带加固**：`LLCfirDiagnosticReporter` 不再对「无 PSI 源」的诊断 `error()`，改为
  降级到当前遍历栈里最近的 PSI-backed 容器（仍无容器才丢弃），避免单条诊断中断整份文件收集。
- **三层验证**：analysis `SourcePostSemaDiagnosticsTest.recursiveConstructorLazySuperConstructor`
  通过；真实项目隔离异常 **3 → 0**。

### 6.12 E7 诊断重复上报（`DEPRECATION_OVERRIDE_WARNING` 5 对）已修

- **analysis 复现**：`AnalysisApiCfirDiagnosticsTest.deprecatedOverrideReportedOnce`
  （`analysis/analysis-api-cfir/testData/diagnostics/deprecatedOverrideReportedOnce.cj`）。
  修复前实测同一 token 上两条完全相同的 `DEPRECATION_OVERRIDE_WARNING@(343,395)`。
- **根因**：`ExtraDeclarationCheckers` 与 `CommonDeclarationCheckers` 同时收录
  `CfirDeprecatedDeclarationChecker`、`CfirVArrayExtraChecker`（callable）与
  `CfirExtendExtraChecker`（extend）。`EXTENDED_AND_COMMON_CHECKERS` 按两个 filter 分别派发，同一
  checker 因此跑两遍。这与该测试类头注明的「三个集合互不重叠」契约矛盾。
- **修复**：`ExtraDeclarationCheckers` 只保留**仅扩展集合提供**的 checker
  （`CfirUnusedExpressionChecker`、`CfirDceUnusedDeclarationChecker`），三处重复收录移除。
- **三层验证**：cfir 侧 checker 集合差异属装配层（测试 harness 只挂 common 集合，无法复现重复）；
  analysis 用例修复后为 1 条；真实项目 WARNING **18 → 13**（10 → 5），重复对消失。

### 6.13 收尾总账（mariadb-driver，`-PrealProjectFilter=mariadb`）

| 指标 | 初始（RUN-A） | §2.4 修复后 | 现在 |
|---|---|---|---|
| ERROR | 545 | 48 | **0** |
| WARNING | 65 | 18 | **13**（5 `DEPRECATED_WARNING` + 5 `DEPRECATION_OVERRIDE_WARNING` + 3 `UNUSED_IMPORT`，均为 cjc 被 `-Woff deprecated/unused` 抑制的真实告警） |
| 被隔离异常 | 69 | 3 | **0** |
| 文件级异常 | 0 | 0 | **0** |
| `dependencyCjoCount` | 2 | 0 | 0 |
| 测试结果 | 失败 | 失败 | **通过** |

各层回归（最后一轮实测）：

| 命令 | 结果 |
|---|---|
| `:cfir:analysis-tests:test --tests "*CfirAnalysisDiagnostics2TestGenerated*"` | 57 suite / 388 用例 / 0 失败 |
| `:analysis:low-level-api-cfir:test` | 62 suite / 189 用例 / 0 失败 |
| `:analysis:analysis-api-cfir:test` | 全绿 |
| `:cfir:cfir-serialization:test` | 20 suite / 121 用例 / 0 失败 |

