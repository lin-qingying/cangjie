# 已知问题与 STUBS extend 修复记录（2026-11-09）

## 1. 已修复：STUBS 下库 extend 成员不可见（`Array.size` / `Int64.toString`）

- 现象：IDE（STUBS）里 `array.size` 报 `'size' is not a member of 'Array'`；`size().toString()`
  解析不到符号。
- 根因：`LLCfirAbstractSessionFactory` 的主源码 session 模板只注册 `dependencyProvider.providers`
  作为 extend 来源；STUBS 下该 provider 是 stub 形态（`LLCangJieStubBasedLibrarySymbolProvider`），
  而 `CfirExtendProviderComposer` 只消费反序列化 provider，因此 `extend Array<T> <: Collection<T>`
  与 `extend Int64 <: ToString` 等库 extend 不可见。
- 修复：四类 session 的装配统一为与主编译器同构的组合（`LLCfirAbstractSessionFactory`）：
  `own 源码 extend 索引 + builtins 会话 + 依赖会话` 的 `extendProviderOrNull`。
  builtins 与 binary library session 各自在工厂里注册 `CfirExtendProvider`：STUBS 用 `.cjo`
  反序列化来源，BINARIES 用 composite/反序列化 provider（`LLModuleWithDependenciesSymbolProvider`
  本身不是反序列化 provider，必须传 `.providers`，否则被 `flattenDeserializedProviders` 过滤成空）。
  依赖会话的库 roots 若已被 builtins 根完全覆盖（`isCoveredByBuiltins`），组合时跳过，
  避免同一批 `.cjo` 产生两份 extend。
- 循环模块依赖由 `CfirExtendProviderComposer` 的**重入防护**终止，守卫为
  `cfir/cfir-serialization/test/.../CfirExtendProviderComposerCycleTest`。
- 守卫（均可独立运行）：
  - `analysis/analysis-api-cfir/test/.../ArraySizeStubsResolutionTest`
  - `analysis/analysis-api-cfir/test/.../ArraySizeStubsLibraryExtendTest`
  - `analysis/analysis-api-cfir/test/.../ArraySizeResolutionTest`（BINARIES 对照）
  - `analysis/low-level-api-cfir/test/.../ExtendProviderUniquenessTest`
    （STUBS 原点；`getExtendsForClass(Array)` 中 `size` 的提供者必须唯一）
  - `analysis/decompiled/decompiler-to-psi/test/.../RealSdkDecompiledSignatureTest`
    （反编译文本必须含 `extend Array<T> <: Collection` 与 `prop size`）
- mutation 证据：临时禁用装配中的 builtins/库 extend 来源，两条 STUBS 用例均变红；恢复后全绿。

## 2. 已知外部失败（不在本修复范围）

`SourcePostSemaDiagnosticsTest.enumEntryShadowedByTypeName` 与
`CfirAnalysisDiagnostics2TestGenerated > Enum > testEnumEntryShadowedByTypeName`：

- 该 fixture 不含 `??`；失败原因是枚举项与同名类型共存时，值位置解析被
  `std.core.Error`/`String` 与 protected class 等候选遮蔽；
- 用 `git stash` 移除本次全部改动后仍失败，失败文本与修复后逐字一致 → 非本修复引入；
- 需单独立项（对齐官方 `EnumSugarTargetsFinder.cpp` 的 `RefineTargets` 语义），在其工作流
  落地后重跑这两个 suite。

## 3. 已知限制：跨 session 的 extend 归属

`CfirDeserializedExtendProvider` 的 `getContainingExtend`/`getPackageFqName` 是 **identity map**
（Cfir callable symbol 未重写 equals）。装配收敛后，source session 组合的是 builtins/binary
session **自己的** extend provider，因此 `moduleData.session` 与索引实例一致，归属查询预期命中；
但当前守卫仍只断言诊断/符号解析，尚无归属断言（follow-up）。

另有一类跨模块可见性缺口（HEAD 即存在，非本修复回归）：`CaLibrarySourceModule` 依赖不满足
`dependencyModule is CaLibraryModule`，其 `.cjo` extend 与源码 extend 对消费方都不可见；
编译器侧经 session 组合可见。最小复现：`// MODULE_KIND: LibrarySource` 的库声明
`extend Box <: Sized`，主模块访问 `box.size` → STUBS 下 `NOT_MEMBER_OF`。

## 4. 性能基线（`:analysis:analysis-performance-test`，本机 2026-11-09，装配收敛 + 重入防护后）

- `CaAnalysisApiStatisticsTest`：7 tests / 0 fail / 5.185s
- `CaSpanTimingTest`：2 tests / 0 fail / 0.852s
- `CaSessionAndDeserializationStatisticsTest`：1 test / 0 fail / 0.017s

（以上为最终装配形态下的基线。）

## 5. 后续项

- **归属断言**：装配收敛后应命中；补一条 `getContainingExtend()`/`getDeclarationPackage()` 断言（见 §3）。
- **library-source 依赖的 extend 可见性**：见 §3 第二类缺口。
- **性能**：大项目首次成员解析耗时仍需单独基线。
- **结构去重**：可考虑在 `CfirDeserializedExtendProvider.buildIndex` 按 `extensionSignatureKey()`
  做结构去重，从根上消除同 roots 双份（当前用 `isCoveredByBuiltins` 在组合时跳过）。
- **重入防护粒度**：当前按 provider 实例 identity 短路；同线程对同一 session provider 的合法
  递归重入会返回空而不是部分索引（已知取舍，见 `CfirExtendProviderComposer` KDoc）。
- **extend 物化的已知限制**（2026-11-10）：
  - 物化 extend 复用 `.cjo` 反序列化的 `typeParameters`：其 `containingDeclarationSymbol` 仍指向
    二进制 extend（同一 TP 声明成为两个 extend 的子节点）；成员类型引用因此自洽，但 owner 校验/遍历
    需注意。等价的，复制的 annotations 的 `containingDeclarationSymbol` 也仍指向二进制声明，
    `status` 与二进制声明共享实例（可变别名）。
  - 结构键 = 接收者 + 父类型 + 成员（kind|name|参数个数），不含参数类型/返回类型/约束；std 的
    505 个 extend 无碰撞，但理论上不同形状的 extend 可能碰撞（需在新增 std extend 形态时复核）。
  - accessor（getter/setter）的 `status`/`dispatchReceiverType` 未随物化成员复制（std 的
    `static prop Max/Min` 这类仍为默认值），属 P2 待办。
  - 性能：物化按目标惰性执行，`getAllExtends()` 不物化；但 perf 计时在并发构建环境下抖动大
    （曾测得 6.19s/0.94s，也曾 9.5s/2.0s），尚未给出稳定 ≤+5% 的结论，需在安静环境复测。
- **去重覆盖**：`isCoveredByBuiltins` 只覆盖 `CaLibraryModule`（roots 全在 builtins 根内）；
  `CaLibrarySourceModule.binaryLibraryModule`、`CaLibraryFallbackDependenciesModule` 与 session
  自身 provider 未过滤，库-vs-库同 roots 也不覆盖。
- **提交卫生**：排除 `.idea/`、`.scratch/`、`.acl-recovery/`、`.atomcode/`、`.dsh-tmp/`、
  `.gradle-queue/`、`.workbuddy/`、`HANDOFF-codeAction.md`、`vscode_cangjie/`；其中
  `.idea/compiler.xml`、`.idea/kotlinc.xml`、`.scratch/*.txt` 是**已跟踪的 M**，按仓库规则
  不能 checkout/reset，需用户决定保留脏改还是另行处理。
