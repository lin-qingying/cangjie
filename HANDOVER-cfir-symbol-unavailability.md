# CFIR 符号不可用：问题清单、目标与进度交接

> 交接文档。给接手继续修复的会话：先读「第 1 节 现状」和「第 6 节 设计决策」，再读「第 5 节 缺口」。
> 最后更新：2026-11-11（第五轮：G7 失败收集器与 LSP codeAction 保留策略）

---

## 1. 现状一句话

三条崩溃的根因（符号恢复被建模成全函数）已在主干上改造为「可判定的缺失值」，崩溃路径全部关闭；
第二轮把 `analysis-api-cfir` 的 `cfirSymbol` 直连从 **39 处**收敛到 **12 处**（7 处非空契约 + 5 处
「未知 ≠ 空」的 fail-fast），并完成 G2–G5；第三轮统一了构造器 `typeParameters` 的两条口径。
第四轮补齐 G6 真缺失端到端夹具；第五轮完成 G7 worker 生命周期、批次内 drain 和 LSP codeAction retention 修复。

---

## 2. 问题清单

### 2.1 原始生产栈（3 条，同一根因）

| # | 现象 | 触发链 | 机制根因 |
|---|---|---|---|
| 1 | `CangJieIllegalArgumentExceptionWithAttachments: We should be able to find a symbol for class-like declaration`，**整段补全消失** | 补全渲染构造器尾文本 → `CaConstructorSymbol.valueParameters` → `createCaValueParameters()` 对库模块返回 null → 回落 `cfirSymbol.createCjValueParameters` → `lazyCfirSymbol` → `resolveToCfirSymbolOfType` → `CfirDeclarationForCompiledElementSearcher.findNonLocalClassLikeDeclaration` 取不到 → `errorWithCfirSpecificEntries` | 符号恢复被写成**全函数**：取不到就 `error()`，异常沿渲染链上抛，一层没接就带走整批候选 |
| 2 | 同一异常的第二条链（声明/表达式返回类型查询） | `CjCallableDeclaration.returnType` → `resolveToCfirSymbol(...).cfir` → 同上 | 同上；且该属性返回类型本就可空，抛异常属**过度反应** |
| 3 | `Inconsistency in the cache. Someone without context put a null value in the cache`，内联提示整片消失 | inlay hints 每条提示都问「是否在宏展开内」→ `macroExpansionMappingAt` → `getOrBuildCfir` 整文件解析 → extend 索引重建 → class-like 缓存：无上下文查询写入 `null`，随后带 PSI 的精确查询要求非空 → `getNotNullValueForNotNullContext` → `fixInconsistentValue` → error 日志 | **缓存键不承载正确性**：`CfirCache` 命中只看 key，而 `null` 被写成「命中」；两种精度共用同一个 ClassId 键 |

### 2.2 同源问题（修复过程中暴露，已纳入范围）

| # | 问题 | 原证据 |
|---|---|---|
| 4 | 缺失原因不可分类，只能靠异常消息文本匹配 | `message?.contains("must not be null due to the contract")` |
| 5 | extend 索引一次坏类型就让「全部重试」或「全部放弃」（无失败记忆） | `LLCfirSessionExtendProvider.resolveTopLevelExtendsToTypes` |
| 6 | 补全层用 `runCatching(Throwable)` 探测可用性，真缺陷被吞成「少一个候选」 | `CfirIndexCompletionSection`、`completionSymbolKind` |
| 7 | 属性层缺少「未知」的表达能力，只能抛或返回假值 | `isNamed` / `isVararg` / `hasDefaultValue` / `classKind` |
| 8 | `classKind` 的 `error("Unsupported class-like symbol")` 会抛到声明渲染、注解值转换里 | `CaDeclarationRenderer`、`CaCfirAnnotationValueConverter` |

---

## 3. 目标与验收（T1–T7）

| 契约 | 验收标准 | 当前 |
|---|---|---|
| **T1 缺失是一等状态** | 底层只有一条实现路径返回 `CfirResolution`；`reason` 是封闭枚举且**每个值都有真实产出点**；不再有字符串匹配分类 | ✅ |
| **T2 对外 API 不因缺失而抛** | 高频对外属性（形参 6 项、callable 返回类型、`classKind`）在符号缺失时返回 error type / null / 安全默认值；异常只留给「框架契约被破坏」 | ⚠️ 部分 |
| **T3 精度进 key** | 「任意命中」用 ClassId 键，「必须精确到这个声明」用 PSI 键；两者不共享条目 | ✅ |
| **T4 否定结果可失效、可记忆** | 可恢复缺失不被永久冻结（有失效窗口）；不可恢复缺失可记忆；重试有上界 | ⚠️ 达成但有性能副作用，见缺口 G2 |
| **T5 展示层不产出错误模板** | 形参未知时只显示 `name: ?`，不拼 named/vararg/default 标记；构造器形参整体不可用时**丢弃该候选**而非渲染 `()`；全部丢弃则回退无括号类型候选 | ✅ |
| **T6 单点失败不扩散** | 补全每个候选独立隔离；诊断每个祖先声明独立；extend 每个文件独立 | ✅ |
| **T7 可观测** | 缺失按 reason 分级记日志（可预期 → debug，代码缺陷/PSI 失效 → warn），同一结论不重复刷 | ✅ |

---

## 4. 已完成项

### 4.1 新增文件（4）

| 文件 | 内容 |
|---|---|
| `analysis/analysis-api/src/org/cangnova/cangjie/analysis/api/symbols/CaSymbolUnavailableException.kt` | 公共异常（基类 `CangJieRuntimeExceptionWithAttachments`）+ 3 值枚举 `MISSING_DECLARATION` / `UNHANDLED_DECLARATION_KIND` / `CORRUPTED_COMPILED_PSI` + `isExpectedAbsence` / `mayRecoverLater` |
| `analysis/low-level-api-cfir/src/.../cfir/symbols/CfirResolution.kt` | `Resolved` / `Unresolved`、`ResolutionDiagnostics`（日志分级）、`CfirUnresolvedSymbolException`、`valueOrNull` / `map` / `flatMap` / `getOrThrow`、`unresolved()` 构造器 |
| `analysis/low-level-api-cfir/test/.../symbols/CfirResolutionTest.kt` | 6 例：map 短路、异常分类、容错边界、getOrThrow 分类 |
| `analysis/low-level-api-cfir/test/.../caches/CfirThreadSafeCacheTest.kt` | 3 例：否定不持久化 / 非空命中 / `getValue` 旧语义不变 |

### 4.2 删除（1）

`analysis/low-level-api-cfir/src/.../cfir/caches/CfirCacheWithInvalidation.kt`
—— `getNotNullValueForNotNullContext` / `fixInconsistentValue` 是契约冲突的物证，零引用后删除。

### 4.3 修改（24）

| 层 | 文件 | 关键改动 |
|---|---|---|
| cfir-tree | `CfirCache.kt` | 新增 `getValueCachingIf(key, context, shouldCache)`（默认实现等价 `getValue`） |
| 缓存 | `CfirThreadSafeCache.kt` | 覆盖上述方法；移除 invalidation 实现，只保留「key 决定命中」单一契约 |
| 缓存 | `CfirThreadSafeCacheWithPostCompute.kt` | 覆盖同一方法：空结果从 map 移除（post-compute 照常执行） |
| low-level | `LLResolutionFacade.kt` | `resolveToCfirSymbolOrUnresolved` 为唯一实现；`resolveToCfirSymbol` = `getOrThrow` 兼容层 |
| low-level | `LowLevelCfirApiFacade.kt` | public facade；`resolveToCfirSymbolOfTypeSafe` 改为真正安全（缺失或类型不符都返回 null） |
| low-level | `CfirDeclarationForCompiledElementSearcher.kt` | 全部入口返回 `CfirResolution`；4 处「必然成立」改 `requireNotNull`；`!isPhysical → CORRUPTED_COMPILED_PSI`；诊断条目保留 classId/caModule/origin |
| low-level | `LLPsiAwareClassLikeSymbolCache.kt` | ClassId / PSI 双键；两条路径都不缓存 null；`tolerateMissingCachedSymbol` 按异常类型 + reason 判断 |
| low-level | `LLCangJieSourceSymbolProvider.kt` | 「已知精确声明」查询改走 PSI 键；删除 `buildAdditionalAttachments` 死参数与 40 行诊断构造器 |
| low-level | `LLCangJieStubBasedLibrarySymbolProvider.kt` | 同上 |
| low-level | `LLCfirSessionExtendProvider.kt` | 成功 / 待重试 / 放弃三态，上限 3 次；PCE 与 `IndexNotReadyException` 上抛；`rebuild` 只用成功文件 |
| low-level | `LLCfirDiagnosticVisitor.kt` | 祖先声明恢复改 `mapNotNull`，单符号缺失不再让整份诊断变空 |
| analysis-api-cfir | `CaCfirPsiSymbol.kt` | `cfirSymbolOrNull`；`MemoizedResolutionLazy`（按 `mayRecoverLater` 分级记忆化，可恢复缺失 TTL 500ms） |
| analysis-api-cfir | `CaCfirValueParameterSymbol.kt` | `name` / `isLet` / `isNamed` / `isVararg` / `hasDefaultValue` / `returnType` 全部降级 |
| analysis-api-cfir | `CaCfirClassLikeSymbols.kt` | 新增 `classKindOrNull`（未分类形态 warn + null），`classKind` 仅保留失败语义 |
| analysis-api-cfir | `CaCfirExpressionTypeProvider.kt` | `CjCallableDeclaration.returnType` 走结果类型，缺失返回 null |
| analysis-api-cfir | `CaCfirAnnotationValueConverter.kt` | 改用 `classKindOrNull` |
| analysis-api | `CaClassSymbol.kt` | 新增 `classKindOrNull`（默认 `= classKind`，供「总能确定」的实现） |
| analysis-api | `CaDeclarationRenderer.kt` | 类别不可判定时跳过渲染，不再 `error()` |
| completion | `CfirSnapshotFactory.kt` | 构造器尾渲染不再降级成 `()`；形参用 `type.isErrorType` 判定，未知只输出 `name: ?`；dedup key 加 callableId |
| completion | `CfirTopLevelCompletionSection.kt` | 分类器 + 顶层 callable 候选级隔离 |
| completion | `CfirIndexCompletionSection.kt` | 单符号判定 + 渲染整体隔离（`candidateOrNull`），替换 `runCatching` |
| 其他 | `DiagnosticCollectionFailures.kt` | **一行**编译错误修复（`also(failures::clear)` → `also { failures.clear() }`），非本次范围 |
| 基线 | `analysis/analysis-api/api/analysis-api.txt` | 已重新生成 |

### 4.4 第二轮（2026-11-11）改动

| 缺口 | 文件 | 关键改动 |
|---|---|---|
| 入口 | `CaCfirSymbol.kt` / `CaCfirPsiSymbol.kt` / `cjSymbolUtils.kt` | `cfirSymbolOrNull` 上移到**基础协议** `CaCfirSymbol<out S>`（`val cfirSymbolOrNull: S?`，默认 `get() = cfirSymbol`），`CaCfirPsiSymbol` 覆写为带记忆化的真实实现；删除 `CaSymbol` 上的同名适配扩展 |
| G1 | `analysis-api-cfir/src` 约 20 个文件 | 27 处降级：非空 `CaType` → `ConeErrorType`；可空 → `null`；谓词 → 安全默认；`equals`/`hashCode` 全函数化（`psiOrSymbolHashCode/Equals`）。**保留 12 处**直连（7 处非空契约 + 5 处「未知 ≠ 空」的 fail-fast） |
| G1 | 新增 `scopes/CaCfirEmptyScope.kt` | 仅在「符号不可恢复」的 3 条路径（combined / extend / declarationSite）返回空作用域；`declaredMemberScope` / `memberScope` 保持 fail-fast（空作用域会把「未知」断言成「没有成员」） |
| G2 | `LLCfirSessionExtendProvider.kt` | 版本 = 文件集；失败文件同样提交；**全部**文件进 `rebuild`；逐条 extend 独立 try/catch（只吞 `CaSymbolUnavailableException`）；重建整体失败时先提交版本再上抛（异常只出现一次）；`degradedFiles` + PSI 修改计数作为重试信号 |
| G2 | `CfirExtendIndexStore.kt` | `rebuild` 逐条建模隔离并暴露 `lastRebuildFailureCount/lastRebuildFailure`；`targetKey == null` 的 extend 不进语义键索引与重复接口 occurrence |
| G3 | `CfirSnapshotFactory.kt` / `CfirTopLevelCompletionSection.kt` | 新增 `ConstructorCompletionCandidate(symbol, ordinal)`，把「符号 + 声明序号」绑成一个参数，去掉 `?: 0` 的静默默认 |
| G4 | `CfirCache.kt` / `CfirThreadSafeCachesFactory.kt` | `getValueCachingIf` 改 `abstract`；`CfirCaffeineCache` 补齐（`putIfAbsent` 语义与 `CfirThreadSafeCache` 对齐） |
| G5 | `LLCfirIdePredicateBasedProvider.kt` | 保持抛异常（Kotlin 同形）+ 补 `moduleForFile != session.caModule → null` |
| 语义修正 | `CangjieVariadicUtils.kt` | 变参判定改用 `containsErrorType()`（官方 invalid 沿 typeArgs 递归传播） |
| 语义修正 | `CaCfirVisibilityChecker.kt` | 未知 → **不可见**（官方 `IsLegalAccess` 默认拒绝） |
| 语义修正 | `CaCfirConstructorSymbol.kt` | `typeParameters` 不再回落 `emptyList()`（未知 ≠ 没有），与 `valueParameters` 同为类型化异常 |
| 测试 | `MemoizedResolutionLazyTest` / `CfirCaffeineCacheTest` / `CaCfirSymbolOrNullContractTest` / `CompiledDeclarationSymbolPathTest` | T4 记忆化与 TTL、G4 否定结果不持久化、降级入口必须声明在基础协议（防自递归）+ 缺失→`null`/可分类异常、decompiled 声明 → `valueParameters`/`returnType` 生产路径 |

### 4.5 第三轮（2026-11-11）改动

| 项 | 文件 | 关键改动 |
|---|---|---|
| 构造器类型参数口径 | `CaCfirConstructorSymbol.kt` | 新增 `ownerClassTypeParameters()`：CFIR 分支不再取构造器自身列表，而是按 `callableId.classId` 取 class-like 符号 → `CaTypeParameterOwnerSymbol.typeParameters`；取不到抛可分类 `CaSymbolUnavailableException`。与源码路径统一为「外层类的类型参数」（官方 `GetCurrentGeneric`，`src/Sema/TypeCheckUtil.cpp:868-876`） |
| 回归用例 | `CompiledDeclarationSymbolPathTest.kt` | 新增 `typeParametersMatchOwner=true` 断言：decompiled `std.core.Array<T>` 的构造器类型参数数量必须与所属类一致 |
| 文档 | 本文档 | 新增 §5.2（口径已统一）、§5.3（G7 复核证据与修法）、§5.4（G6 真缺失夹具可行性结论） |

### 4.6 第四轮（2026-11-11）改动

| 项 | 文件 | 关键改动 |
|---|---|---|
| 测试专用 decompiler | `CompiledDeclarationUnavailableTest.kt` | 只为 G6 configurator 替换 `TestModuleDecompiler`；编译 `removedArtifact` 的源码模块得到旧 CJO PSI，默认 decompiler 与通用模块工厂保持原样 |
| 端到端用例 | `CompiledDeclarationUnavailableTest.kt` / `testData/compiledSymbolUnavailable/compiledDeclarationUnavailable.cj` | 旧 CJO 含 `RemovedDeclaration`，当前 CJO 不含；在 `BINARIES` 来源下断言 compiled/physical PSI 映射到当前 library，构造器 `cfirSymbolOrNull` 返回 null、返回类型为 error type，`valueParameters` 抛出 `MISSING_DECLARATION` |
| 验证 | 相关 Gradle 任务 | `:analysis:analysis-test-framework:compileTestFixturesKotlin`、`:analysis:analysis-api-cfir:compileTestKotlin` 与 `:analysis:analysis-api-cfir:test --tests "*CompiledDeclarationUnavailableTest"` 通过 |

### 4.7 第五轮（2026-11-11）改动

| 项 | 文件 | 关键改动 |
|---|---|---|
| G7 失败记录所有权 | `DiagnosticCollectionFailures.kt` | 全局队列不再以 `Thread` 为 key；每条记录关联不持有 Thread 的轻量 `ThreadFailureState` 与 generation，`clear()` 只删除调用线程当前代次记录 |
| G7 批次等待 | `DiagnosticCollectionFailures.kt` | collection 外的 `drain()` 仍等所有活动批次结束；当前线程处于 collection 时直接消费已记录快照，避免等待自身结束 |
| G7 回归用例 | `DiagnosticCollectionIsolationTest.kt` | 验证调用线程 `clear()` 不丢 worker 记录、active collection 内 drain 返回，以及外部 drain 等待 worker 批次结束 |
| LSP codeAction 保留策略 | `CangjieServerContext.kt` / `CangjieTextDocumentService.kt` | `codeAction` 与 `resolveCodeAction` 经共享诊断 collection 边界；生产默认不保留 isolated Throwable，测试宿主仍可 drain |
| LSP 回归用例 | `CangjieCodeActionFailureRetentionTest.kt` | fake facade 在 codeAction 列表与 resolve 阶段分别记录 isolated failure；production retention=false 后全局 drain 为空 |
| 验证 | low-level 与 LSP 定向测试 | `DiagnosticCollectionIsolationTest` 与 `CangjieCodeActionFailureRetentionTest` 均通过 |

---

## 5. 缺口（待办）

> 第二轮状态（2026-11-11）：G1–G5 已处理，G6 只补到「机制级」用例，端到端仍缺。

| 级别 | 缺口 | 定位 | 状态 / 建议 |
|---|---|---|---|
| — | **G1 符号属性层直连 `cfirSymbol`** | `analysis-api-cfir/src` | ✅ 39 → 12。其中 7 处是**非空契约**（`CaCfirTypeCreator` 的 1 个 type-parameter `require*`、`CaCfirSignatureModel` 的 2 个签名构造 + 2 处等值、`CaCfirBasePropertyAccessorSymbol` 的访问器派生）；另外 5 处是审查后**主动回归 fail-fast** 的「未知 ≠ 空」点（`superTypes` 同族：`declaredMemberScope`/`memberScope`、文件作用域声明、cdoc 类型参数）。它们要么抛**可分类**的 `CaSymbolUnavailableException`，要么只在「符号种类不对」时 `error()`——对应设计决策 8 / 11 |
| — | **G2 extend 每次查询全量重建 + 重试预算烧穿** | `LLCfirSessionExtendProvider.ensureIndexIsFresh` | ✅ 重建判据改为**文件集版本**；失败文件同样提交（不再被排除），全部文件进 `rebuild`；删除 `pendingRetryFiles` / `failedAttempts` / `MAX_EXTEND_RESOLVE_ATTEMPTS`——官方本来就没有多轮/重试（`src/Sema/PreCheck.cpp:2270-2302`），重试粒度改为「索引版本变化 + PSI 新鲜度」 |
| — | **G3 构造器 dedup key 无法区分未知类型重载** | `CfirSnapshotFactory.deduplicationKey` | ✅ 改用 `ConstructorCompletionCandidate(symbol, ordinal)`，序号作为**声明身份**在字符串键下的替身。**不要**改成参数类型签名：类型未知时两边都是 `?`，恰恰会重新合并（Kotlin 用 `firSymbol` 身份判等，永远可区分） |
| — | **G4 Caffeine 分支未覆盖 `getValueCachingIf`** | `CfirThreadSafeCachesFactory` | ✅ 基类改 `abstract`（违约变成编译错误），`CfirCaffeineCache` 补齐；新增 `CfirCaffeineCacheTest` 3 例 |
| — | **G5 predicate 走抛异常路径** | `LLCfirIdePredicateBasedProvider:117` | ✅ **保持抛异常**（Kotlin 同位置也是 `resolveToFirSymbol(...).fir` 且全文无 catch），并按 Kotlin 补上缺失的 `moduleForFile != session.caModule → return null` 模块一致性过滤 |
| **G6 / P1** | 无「真实 compiled 声明缺失」的端到端回归用例 | `analysis-api-cfir` 专用 `TestModuleDecompiler` | ✅ 新增错配夹具和端到端用例；定向测试通过。之前的 4 条机制/生产路径用例仍保留 |
| **G7 / P2** | `DiagnosticCollectionFailures` 线程强引用 + `drain()` 自等待 + codeAction retention 漏接 | `DiagnosticCollectionFailures.kt` / LSP request boundary | ✅ token 队列与批次内 drain 修复完成；LSP codeAction/resolve 已纳入 production retention=false；两组定向测试通过，见 §5.3 |
| — | 4 处 low-level 内部 fail-fast（改签 / predicate / AllCandidates / CInterop） | 见第 6 节 | **有意保留**；第二轮确认 predicate 属这一类 |

### 5.1 第二轮新增的两条「已文档化偏离」

| 偏离 | 位置 | 为什么必须偏离 |
|---|---|---|
| `cfirSymbolOrNull` 单一可空入口 | `CaCfirSymbol.kt`（声明）/ `CaCfirPsiSymbol.kt`（实现） | Kotlin 每个声明必有 `FirSymbol`（无 `FirErrorDeclarationSymbol`、无可空 `firSymbol`）；CFIR 的库声明**真的会恢复失败**。按 `CLAUDE.md`「无对应物时新增 Cfir API 并记录偏离」。入口必须只有**一个**且声明在基础协议上——曾有「只声明在子接口 + `CaSymbol` 适配扩展」的形态，因扩展解析回自身导致 `StackOverflowError` |
| `CfirCache.getValueCachingIf` | `CfirCache.kt` | Kotlin `FirCache` 无此 API，它照常缓存 null 并靠 session 失效兜底；仓颉 session 只在模块修改事件时重建，单靠失效会把「索引未就绪」冻结到会话结束 |

### 5.2 构造器 `typeParameters` 的两条口径（第三轮已统一）

原状：源码路径取「外层类的类型参数」，CFIR 路径取「构造器自身列表」
（官方 `sema_forbid_generic_constructor`，`src/Sema/TypeChecker.cpp:1330-1341`；psi2cfir 也只转换
构造器自身列表，`PsiRawCfirBuilder.kt` 的 `typeParametersForConstructor`），因此同一个库构造器
在有/无 PSI 时会给出不同答案。

现状态：两条路径统一为「外层类的类型参数」——PSI 分支仍取
`getContainingTypeStatement().classSymbol.typeParameters`；CFIR 分支改为按
`callableId.classId` 取 class-like 符号再取 `CaTypeParameterOwnerSymbol.typeParameters`
（`CaCfirConstructorSymbol.ownerClassTypeParameters`）。外层类取不到时抛可分类的
`CaSymbolUnavailableException`，**不再**退回空列表。

回归用例：`CompiledDeclarationSymbolPathTest` 的 `typeParametersMatchOwner=true` 断言
（decompiled `std.core.Array<T>` 的构造器数量与类一致）。

### 5.3 G7 复核与修复结论（第五轮）

`analysis/low-level-api-cfir/src/.../diagnostics/DiagnosticCollectionFailures.kt` 的两个风险仍成立：

1. **线程强引用**：原 `failuresByThread: MutableMap<Thread, MutableList<Throwable>>` 会让全局单例持有
   退出 worker 的 `Thread` 及其 `ThreadLocalMap`，直到同线程 `clear()` 或全局 `drain()`。
   换成 `WeakHashMap` 会让 GC 丢掉仍须被报告的失败。现实现将异常放入受锁保护的全局队列，
   每条记录只持有轻量 `ThreadFailureState` token 与 generation，不持有 `Thread`；`clear()` 按当前
   worker 的 token 删除其记录，既保留可观测性，也不会锚定线程及其 ThreadLocalMap。
2. **`drain()` 条件自等待**：collection 外仍在 `activeCollections > 0` 时等待；同线程在
   `withCollection {}` 内调用时跳过等待，取出当前已记录快照。其它 worker 后续记录的异常由下一次
   `drain()` 获取。回归用例用 executor 限时等待，避免旧实现的自等待卡住测试进程。

Kotlin low-level FIR 只有 visitor 级异常隔离和日志，没有跨 worker 的失败聚合器；官方 Cangjie 编译器
也没有对应的全局 failure drain 生命周期。该 collector 是本地测试与 LSP 报告需求；LSP 的
`withCollection(retainFailures = false)` 仍关闭当前 worker 上的异常对象保留。

复核还发现 LSP `codeAction` / `resolveCodeAction` 会收集诊断，但此前直接调用 `analysisFacade`，
没有进入 `CangjieServerContext.withDiagnosticCollection`。两条请求现由 server context 统一包围，
沿用 push/pull diagnostics 的 production `retainFailures` 策略；新增 fake facade 回归用例在列表与
resolve 阶段分别记录异常，并断言生产配置下 `drain()` 为空。

### 5.4 G6「真缺失」端到端夹具（第四轮已完成）

已核实的代码分叉（决定 fixture 必须满足什么）：

1. `LLResolutionFacade.resolveToCfirSymbolOrUnresolved` 按**元素所属模块**的策略分流
   （`:194-199`）：`LAZY` → `findSourceCfirSymbol`，`STATIC` → `findCompiledCfirSymbol`。
   只有 STATIC 分支才会走到 `CfirDeclarationForCompiledElementSearcher`。
2. `CfirDeclarationForCompiledElementSearcher` 把「缺失」判成 `MISSING_DECLARATION`，
   把「反编译 PSI 已失效」判成 `CORRUPTED_COMPILED_PSI`（`:235` `!declaration.isPhysical`）。

测试数据包含两个独立编译产物：测试专用 `TestModuleDecompiler` 按需将 `removedArtifact` 源码模块
编译成旧 CJO，并只用它生成反编译 PSI；`lib` 独立编译当前 CJO，并作为 Analysis API library 的
binary roots。旧 CJO 的
`RemovedDeclaration(value: Int64)` 因此存在于映射到 `lib` 的物理、compiled PSI 中，却不存在于
`lib` 当前二进制索引。用例以 `LibrarySource` 为 use-site，使构造器 `valueParameters` 经过
`createCaValueParameters() == null` 的生产分支，再进入 compiled symbol 恢复。

来源必须设为 `BINARIES`：`CfirDeclarationForCompiledElementSearcher` 在该模式按当前 binary 的
`ClassId` 查找；`STUBS` 模式按调用方传入的 PSI 反序列化，旧 PSI 本身可能重新提供目标声明，不能证明 binary 缺失。
测试断言旧 PSI 的 `isPhysical` 和 `isCompiled`，且 `CjTestModuleStructure` 将它映射到当前 `lib`
模块；另行反编译当前 binary 并确认其中含 `CurrentMarker`、不含 `RemovedDeclaration`。Analysis API
禁止在 `LibrarySource` session 中直接查询另一个模块的 PSI，因此测试在该 session 中构造生产
`CaCfirConstructorSymbol` 叶子，随后验证 `cfirSymbolOrNull == null`、返回类型为 error type，且
`valueParameters` 抛出可分类的 `MISSING_DECLARATION`。

错配只由 `CompiledDeclarationUnavailableTest` 的 configurator 和 `TestModuleDecompiler` 服务实现，
通过现有可替换服务点接入；默认 `TestModuleDecompilerDirectory`、通用测试指令和模块结构工厂不变。


---

## 6. 设计决策（不要退回旧模式）

1. **缺失用值表达，不用异常探测**
   底层唯一实现返回 `CfirResolution`；`CfirUnresolvedSymbolException` 只作为仍要求非空符号的旧调用点的兼容适配（`getOrThrow`）。

2. **`lazy` 必须缓存「结果」，不能缓存「异常」**
   Kotlin `lazy(PUBLICATION)` 在初始化抛异常时**不写缓存** → 每次属性访问重跑整条恢复链。
   `MemoizedResolutionLazy` 缓存的是 `CfirResolution`。

3. **可恢复缺失不能被永久记忆**
   session 只在模块修改事件时重建，**不因 dumb 模式结束而重建**。所以：
   - 可恢复缺失（`mayRecoverLater = true`）→ 记忆 500ms，过期重算；
   - 不可恢复缺失（`UNHANDLED_DECLARATION_KIND`）→ 永久记忆（代码缺陷，避免重复告警）；
   - 成功 → 永久记忆。
   这一条同时挡掉两个极端：「冻结」与「无限重算」。

4. **缓存层不缓存 null**
   空的成因绝大多数是「索引未就绪」。缓存空值 = 冻结成永久缺失。
   重算成本由第 3 条的 TTL 吸收，不在缓存层重复做。

5. **精度必须进 key**
   `CfirCache` 命中只看 key。任何「必须精确到这个声明」的要求都必须换成 PSI 键，
   不能塞进 context（这正是第 3 条生产栈的根因）。

6. **补全只按类型捕获**
   只捕 `CaSymbolUnavailableException`。「未知」用 `type.isErrorType` 判断，不用 `try/catch` 探测。
   `runCatching(Throwable)` 会把真缺陷吞成「少一个候选」。

7. **展示层不能把「未知」渲染成「确定」**
   形参未知时 `isLet/isNamed/isVararg/hasDefaultValue` 全是不可信默认值，拼进尾文本会得到
   「看起来正常但形态错误」的调用模板。只输出 `name: ?`。
   构造器形参整体不可用 → 丢弃候选（`()` 会插入无参调用，是错误模板）。

8. **内部实现保留 fail-fast**
   改签、解析、predicate 里符号缺失意味着「无法确定目标」；静默跳过会作用在错误目标上。
   契约 T2 的适用范围是**对外 API**，不是内部意图明确的调用点。

9. **降级入口只有一个，且必须声明在基础协议上**
   `cfirSymbolOrNull` 声明在 `CaCfirSymbol<out S>`（默认 `get() = cfirSymbol`），
   `CaCfirPsiSymbol` 只做实现覆写。**禁止**再在 `CaSymbol` 上挂同名适配扩展：
   扩展体在 `CaCfirSymbol` 上解析不到成员时会解析回自身，形成 `StackOverflowError`
   （第二轮审查真实抓到的缺陷）。回归用例：`CaCfirSymbolOrNullContractTest`。

10. **等值与哈希必须是全函数**
    `equals`/`hashCode` 里禁止出现会抛的 `cfirSymbol`。符号不可恢复时哈希退化为
    `System.identityHashCode`，等值判定「不猜相等」（唯一例外是同一个库 PSI 对象）。
    否则一个不可用符号进入任何 HashSet/HashMap 都会在哈希阶段炸掉。

11. **「未知」与「空」不可互换**
    非空 `CaType` → `ConeErrorType`；可空 → `null`；
    但 `List`/`Set`/`Sequence` 型属性若语义是「成员集合」，缺失时必须 fail-fast，
    不能返回空集合（空集合是「确定没有」的正向断言）。
    例：`valueParameters`、`typeParameters`、`superTypes`、`declaredMemberScope`/`memberScope`、文件作用域声明。

12. **访问控制未知时 fail-closed**
    官方 `IsLegalAccess` 没有「未知 ⇒ 可见」分支，末位默认拒绝。
    「一次索引缺失不要隐藏整批成员」由展示层候选级隔离解决，不靠放宽可见性。

---

## 7. 验证方式

```powershell
# 判据 1：应为空（只剩文档注释引用）
rg "Inconsistency in the cache|fixInconsistentValue|getNotNullValueForNotNullContext" --glob "*.kt"
# 判据 2：补全模块不应再有 runCatching（除服务装配处）
rg "runCatching" code-insight/completion/src
# 判据 3：降级入口不得再出现「只声明在子接口 + 基础协议适配扩展」的形态
rg "val CaSymbol\.cfirSymbolOrNull" analysis/analysis-api-cfir/src
# 判据 4：cfirSymbol 直连应只剩 7 处「非空契约」
rg "\.cfirSymbol\b" analysis/analysis-api-cfir/src

# 基线（改 analysis-api public 声明后必须重新生成）
.\gradlew-queue.bat :analysis:analysis-api:generateApiSurfaceDump
.\gradlew-queue.bat :analysis:analysis-api:checkApiSurfaceDump

# 回归
.\gradlew-queue.bat :analysis:analysis-api-cfir:test                    # 491 套件 / 1697 例
.\gradlew-queue.bat :code-insight:completion:test :code-insight:hints:test   # 31 套件 / 199 例
.\gradlew-queue.bat :analysis:low-level-api-cfir:test --tests "*CfirResolutionTest*" --tests "*CfirThreadSafeCacheTest*" --tests "*CfirCaffeineCacheTest*" --tests "*CompiledDeclaration*" --tests "*Diagnostic*" --tests "*Extend*"
.\gradlew-queue.bat :analysis:analysis-api-cfir:test --tests "*MemoizedResolutionLazyTest*" --tests "*CaCfirSymbolOrNullContractTest*"
.\gradlew-queue.bat :analysis:analysis-api-cfir:test --tests "*CompiledDeclarationUnavailableTest*"
```

当前验证结果：`:analysis:analysis-test-framework:compileTestFixturesKotlin`、
`:analysis:analysis-api-cfir:compileTestKotlin` 和定向 `CompiledDeclarationUnavailableTest` 通过。
`:analysis:low-level-api-cfir:test --tests "*DiagnosticCollectionIsolationTest"` 与
`:lsp:test --tests "*CangjieCodeActionFailureRetentionTest"` 通过。
`:analysis:analysis-api-cfir:test`、`:analysis:low-level-api-cfir:test` 与 `:lsp:test` 全量测试尚未执行。

---

## 8. 接手注意事项

1. **工作区有大量他人未提交改动**（LSP 宏诊断推送、macro 快照失效、`compiler/frontend`、`StubBasedCfirMemberDeserializer`、`CfirElementFinder` 等）。
   **不要整体 `git reset` / `git checkout .`**，只操作第 4 节列出的文件。
2. 并发跑 gradle 一律用 `gradlew-queue.bat`（全局串行锁）。
3. 改 `analysis-api` 的 public 声明后必须跑 `generateApiSurfaceDump`，否则 `check` 失败。
4. 判断改动是否达标的三条 grep 判据见第 7 节。
5. 设计意图写在 `CfirResolution.kt` / `CaSymbolUnavailableException.kt` / `LLPsiAwareClassLikeSymbolCache.kt` 三个类注释里；
   改造新属性前先读它们，避免退回「逐处 try/catch」。

---

## 9. 非目标（避免扩大战线）

| 非目标 | 理由 |
|---|---|
| 一次消除全部 140 处 `cfirSymbol` 直连 | 需逐属性建立「未知」语义，是独立重构；本轮只覆盖崩溃路径与高频对外属性 |
| 改 `Analysis API` 既有对外语义（如把 `classKind` 改成可空） | 破坏兼容；改用新增 `classKindOrNull` 的增量方式 |
| 让「符号缺失」在 low-level 内部也静默 | 见第 6 节第 8 条 |
| 重构 `DiagnosticCollectionFailures` / LSP / macro 基础设施 | 属他人未提交改动范围 |
| 用 `try/catch(Throwable)` 兜住补全渲染 | 与 T2 / T7 直接冲突 |
