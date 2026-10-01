# `.cjo` 反编译链方案实施记录（2026-09-30）

- 方案：`docs/cjo-decompiler-architecture-plan-20260930.md`（第五版）
- 实施分支：主仓 worktree `claude/optimistic-yonath-1f3a9a`；IDE 仓 `intellij-ide` 分支 `cfir-new`
- 本文件按技能的 Repair Report Fields 记录每个改动组的调查字段与验证字段；未验证项显式标注。

## 环境事实（影响验证方式，先记录）

1. **IDE 仓是复合构建**：`intellij-ide/settings.gradle.kts` 通过 `includeBuild("../")` 把主仓替换进 IDE 构建
   （`:cangjie:*` 任务路径即主仓源码）。因此 IDE 侧改动只能用主检出目录 `D:\code\intellij\cangjie` 的源码编译，
   本次实施在 worktree 中进行，**IDE 侧编译/测试在主仓改动合入主检出前无法进行**。
2. **主检出目录存在并行会话的未提交改动**：`.gitignore`、`analysis/decompiled/decompiler-to-file-stubs/build.gradle.kts`
   （新增 `:util` 依赖）、`CjoBinaryFileReader.kt`、`CangJieDecompiledFileViewProviderFactory.kt`、
   `CjoBinaryFileDecompiler.kt`、`CjDecompiledFile.kt`、`CangJieCompiledFileErrors.kt`，以及 `out/` 取消跟踪与 `.idea/*`。
   其中 `CjoBinaryFileReader.kt` 使用 `thisLogger()`，在本仓 `:dependencies:intellij-core` 下**无法解析**（`logger<T>()` 才是本仓可用 API），
   即该未提交版本在本仓自身构建中即编译失败。本次实施未改动主检出目录的任何文件。
3. 本仓 IntelliJ 平台 API 以 `logger<T>()` + `org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown` 为惯例，
   改动沿用该惯例。（主检出目录的并行版本使用 `thisLogger()`，在本仓与 IDE 复合构建下都解析不到。）
4. 主仓源码在两种构建下编译的平台 API 并不完全相同：IDE 复合构建里 `PsiModificationTracker.getInstance()`（无参）不可用，
   只有 `getInstance(project)`。本次改动只使用带 `project` 的重载。
5. 两个 Gradle 进程不能同时写同一个工作树的 `build/` 目录：本次曾因主仓测试与 IDE 复合构建并发而出现
   “`.class` 文件找不到 / 依赖类不可见”的假失败。验证必须串行执行。
6. 主检出目录的 `build/` 产物曾处于不完整状态（`.class` 缺失）导致 IDE 复合构建编译报
   `FileNotFoundException`；重新执行 `:psi:classes`、`:analysis:analysis-api:classes` 后恢复。
7. 主检出目录已快进到本 worktree 的实现提交（`11d298847`、`f989a4bff`）；被取代的旧未提交 P0 改动
   备份在 `tmp/parallel-wip-20260930.patch`。`.idea/*` 的既有本地修改未动。

## 改动组 1（P0 改动 1/2/4/5：文件类型层诊断与 null-safe、搜索根折叠、过期产物清理）

- 问题类型：文件类型层异常 + 静默失败 + 重复实现
- 根因：`CjoBinaryFileReader.readPackageFqName` 用 `runCatching { }.getOrNull()` 吞掉所有失败；
  `CangJieDecompiledFileViewProviderFactory.createFileViewProvider` 用 `checkNotNull` 在平台调用链里抛 `IllegalStateException`；
  `CjoBinaryFileDecompiler` 失败时返回空文本；搜索根折叠在三处各写一份。
- 官方证据：`cjc` 不适用（IDE 宿主行为）；证据来自平台机制——`FileManagerImpl.createFileViewProvider` 直接调用工厂且无 catch，
  异常会打断 `openFilesOnStartup` / `CodeFoldingNecromancer`；`LoadTextUtil` 是 `filetype.decompiler` 的同步入口。
- Kotlin 对位文件：`BuiltinsVirtualFileProviderBaseImpl`（枚举缓存）、`CandidateCollector`（builtins 候选判断）、
  `LLBinaryOriginLibrarySymbolProviderFactory.toBuiltinsSearchRoot`（搜索根折叠）、`LLFirBuiltinsSessionFactory`（`getOrPut` + `CachedValue`）。
- CFIR owner 文件：`CjoBinaryFileReader.kt`、`CangJieDecompiledFileViewProviderFactory.kt`、`CjoBinaryFileDecompiler.kt`、
  `CangJieCompiledFileErrors.kt`、`CjDecompiledFile.kt`、`DecompiledCjoRepository.kt`、`DecompiledPackageDataFinder.kt`、
  `LLBinaryOriginLibrarySymbolProviderFactory.kt`
- 修复原则：失败出口一律留日志并原样重抛控制流异常，工厂不再用异常表达“不支持”；搜索根折叠归一到
  `decompiler-to-file-stubs` 的 `normalizeCjoSearchRoot / toCjoSearchRoot / distinctCjoSearchRoots / cjoSearchPathStringOf`。
- 覆盖 fixture：`CjoBinaryFileReaderTest`、`BuiltinsVirtualFileProviderCliImplTest`、`DecompiledTextBuilderTest`、`CompiledStubShapeTest`
- 验证：`:analysis:decompiled:decompiler-to-file-stubs:test`、`:analysis:decompiled:decompiler-to-psi:test`、`:analysis:decompiled:test`
  通过（20 用例，0 失败）。

## 改动组 2（IDE 改动 3：builtins/library 根变化时补发全局模块状态事件）

- 问题类型：S3 builtins session 搜索根烤死且 IDE 无失效发布方
- 根因：`publishGlobalModuleStateModificationEvent` 在 IDE 侧一处调用都没有；`CjWorkspaceModelSync.syncProject`
  每次都重建 stdlib project library，但事件从未随根变化发布，builtins session 里的 `CjoManager` 搜索根保持首次创建时的快照。
- 证据：`LLCfirSessionCacheStorageInvalidator.invalidate` 对 `KotlinGlobalModuleStateModificationEvent` 走
  `invalidateAll(includeLibraryModules = true)`；`CangJieIdeOutOfBlockModificationService` 只发 out-of-block 事件（对 builtins 无效）。
- Kotlin 对位：`KaBuiltinsSessionFactory` 注释明确 builtins 不受 out-of-block 修改影响，只能靠 global module state 事件失效。
- CFIR/IDE owner 文件：`CjWorkspaceModelSync.kt`（根列表比对 + 事件发布）、`modules/domain/project-model/build.gradle.kts`
- 修复原则：只在 stdlib library 实体的根列表真的变化时，于 `workspaceModel.update {}` 返回之后、写动作内发布事件；
  根列表不变时不发，避免每次项目打开都丢掉全部 session。
- 验证：把 `intellij-ide/settings.gradle.kts` 的 `includeBuild` 临时指向本 worktree 后，
  `:modules:domain:project-model:compileKotlin`、`:modules:ide:base:compileKotlin`、`:modules:ide:project:compileKotlin` 通过（随后恢复原值）；
  IDE 测试（`CjWorkspaceModelSyncStdlibTest` 等）未运行。

## 改动组 3（P1 改动 6/7/8：`accepts = isSupported`、detached session/module data、结构外 owner 降级）

- 问题类型：索引洞（`S9`）+ 文件类型层重复读 `.cjo`（`S2`）
- 根因：`accepts` 走 `hasStub → hasMetadata → readPackageFqName`，每次打开 `.cjo` 至少同步读 1–2 MB 两遍；
  `findOwningModule` 为 null 时 `readFile` 返回 null，平台 `buildFileStub` 返回 null，索引里没有该文件的条目。
- Kotlin 对位：`KotlinMetadataDecompiler` 的 `accepts` 只做 `isSupported`；`CoreStubTreeLoader.readOrBuild` 不读索引；
  `KotlinStandaloneIndexBuilder.findSharedDecompiledFile(...) ?: continue`。
- CFIR owner 文件：`CangJieMetadataDecompiler.kt`、`CjoModuleDataProvider.kt`、`DetachedCjoModuleData.kt`（cfir-common）、
  `DetachedCjoSession.kt`（cfir-providers）、`DecompiledCjoModuleDataProvider.kt`、`DecompiledPackageDataFinder.kt`、
  `CangJieBuiltInMetadataStubBuilder.kt`、`cangjie-analysis-decompiled.xml`
- 修复原则：owner 解析以“结构内 → 真实 session；结构外 → per-file detached session”为唯一规则，不再返回 null；
  detached session 只注册反序列化真正会读的两个组件（`CfirLanguageSettingsComponent`、`CfirCangJieScopeProvider`），
  不注册 `CfirSymbolProvider`，不继承 `LLCfirModuleData`（其 `session` 有 fallback，会 `ClassCastException`）。
- 验证：`:analysis:decompiled:decompiler-to-file-stubs:test`（5）、`:analysis:decompiled:decompiler-to-psi:test`（21）、
  `:cfir:cfir-serialization:test`（115，5 skipped）全绿；`:analysis:stubs:test` 8 个用例中 7 个通过，
  `BuiltinsStubsTest` 的 `std.argopt.cjo.stubs.txt` 不匹配（`@APILevel` 侧 car 注解缺失）**在 HEAD（去掉本次改动）上同样失败**，
  属既有问题（golden 于 7272bf842 重生成，侧 car fixture 提交于 698a8e7ea），未由本次改动引起；46 份 `.stubs.txt` + 46 份
  `.decompiled.text.cj` 文件本身未改动。新增测试：`CangJieMetadataDecompilerAcceptanceTest`、`CangJieMetadataStubBuilderTest`、
  `CangJieMetadataStubBuilderDetachedTest`（结构外 `.cjo` → `DetachedCjoModuleData` + `DetachedCjoSession`）。

## 改动组 4（P2 改动 9/10/11a/12：枚举缓存、候选按根判断、断开构造期求值、索引键）

- 问题类型：`S5` 高频全量遍历、`S4` builtins 范围三处触发、`S3` 构造期遍历 SDK 目录、`S10` 缓存键缺根
- 根因：见上；`LLCangJieStubBasedLibrarySymbolProvider` 在构造期就 `createDeclarationProvider(scope)`，
  IDE 宿主实现会遍历作用域并触碰 stub 索引，使 builtins session 构造同线程重入 `getBuiltinsSession`；
  `CaIdeCandidateCollector` 对每个文件读 `builtinsModule.contentScope`（每次重新枚举 SDK 目录）。
- Kotlin 对位：`LLFirBuiltinsSessionFactory`（同样构造期求值，但 Kotlin builtins 不经 stub 索引——这是仓颉特有差异，已记录为偏离）；
  `BuiltinsVirtualFileProviderBaseImpl` 的枚举缓存；`CandidateCollector.collectBuiltinsCandidates`。
- CFIR owner 文件：`BuiltinsVirtualFileProvider.kt`、`CfirLazySymbolNamesProvider.kt`（新增）、
  `LLCangJieStubBasedLibrarySymbolProvider.kt`、`LLStubOriginLibrarySymbolProviderFactory.kt`、
  `sessionFactoryHelpers.kt`（删除未使用的 `annotationSearchScope` 形参）、`LLCfirBuiltinsSessionFactory.kt`、
  `DecompiledBinaryIndexImpl.kt`、`DecompiledPackageDataFinder.kt`、`CjoSearchPath.kt`（新增显式根构造）、
  `LLBinaryOriginLibrarySymbolProviderFactory.kt`
- 偏离记录：Kotlin `LLKotlinStubBasedLibrarySymbolProvider` 在构造期即 `createDeclarationProvider`；仓颉不能照搬，
  因为 builtins `.cjo` 走 stub 索引（`CaIdeScopeCangJieFileCollector`），构造期求值会重入。
- 修复原则：把“建 session”与“用 session 查符号”之间的求值推迟到第一次真实查询（lazy 代理组件），
  使 session 构造不再遍历 SDK 目录、不触碰 `.cjo` stub；枚举结果按“根集合 + 修改计数”缓存并补 `checkCanceled`；
  builtins 候选改为根路径包含判断；索引键并入根列表摘要，修改计数缺失时退回 PSI 计数而非常量 0。
- 主仓测试补充：`CangJieMetadataStubBuilderTest.invalidFileIsRejectedWithoutHeaderRead`
  （`!isValid` 的 `.cjo` 不触发头部读）。
  （`contentScope` → 枚举、`declarationProvider` → stub 索引）全部推迟，重入边不再存在；
  此时再加“半成品 session”只会引入一个任何误用都会 `error(...)` 的中间态。依据是代码推导，未在运行期复核。
- 回归事故与修复：第一版 `CfirLazySymbolNamesProvider` 在解析前对包名返回 null、对能力标志返回 true，
  而 `CfirCompositeSymbolNamesProvider.flatMapToNullableSet` 里任一子 provider 返回 null 会让整个聚合为 null，
  `CfirCachedSymbolNamesProvider` 随即按“无顶层 classifier 的包”过滤 → 包作用域、覆写链、`isSubclassOf`
  的符号消失。表现为 `:analysis:analysis-api-cfir:test` 20 失败（基线 4）、`:analysis:low-level-api-cfir:test`
  6 失败（基线 4）。修法：代理任何访问都先建真实索引；组合器的三个能力标志由构造期 `val` 改为 `get()`，
  组合器构造不再触发求值。修复后两个套件与基线逐条一致。
- 验证：以 `df71d7212` 建基线工作树对照：`:analysis:analysis-api-cfir:test` 1514 条 4 失败（注解 4 条，既有）、
  `:analysis:low-level-api-cfir:test` 131 条 4 失败（Cjmp CallerFirst 4 条，既有），与基线逐条相同；
  主仓全部生产代码编译通过（含 IDE 复合构建）；`:analysis:decompiled:*`、`:analysis:stubs`、`cfir:cfir-serialization` 如上。

## 改动组 6（P2 改动 12b：builtins session 创建重入守卫）

- 问题类型：建 session 期间同线程重入的唯一出口（对缓存 map 递归 → `Recursive update`，或静默拿到空 session）
- 根因：`getBuiltinsSession` 用 `builtinsSessions.getOrPut`；映射函数内 `createBuiltinsSession` 一旦回到本入口，
  `ConcurrentHashMap` 会抛 `Recursive update`，而方案也明确禁止换成 `computeIfAbsent`（同样的 map）。
- 修复：新增 `BuiltinsSessionCreationGuard`（ThreadLocal 记录当前线程正在创建中的 `(平台, session)`）：
  重入命中时返回半成品 session 并写一条 warn（线程名、平台、session 的 `toString`），登记在 `finally` 里清除；
  同一键二次登记 `check` 失败（编程错误，应当先走重入短路）。`LLCfirBuiltinsSession` 增加
  `isUnderConstruction` / `markConstructionCompleted()`，构造完成前才半成品，`toString` 带 `construction=`，
  这样半成品上误读 `CfirProvider` 时 `ArrayMapAccessor` 的错误信息（`No '...' in array owner: $thisRef`）
  能直接看出 session 还没建完。守卫只在创建期间挡在 `CfirSymbolProvider` / `CfirProvider` 注册之前，
  12a 之后这两者本来就不在构造期求值，守卫是防御层。
- 与 Kotlin 对位：Kotlin `LLFirBuiltinsSessionFactory` 同样是 `getOrPut` 且无守卫；Kotlin builtins 不经 stub 索引，
  构造期不会重入（已记录为仓颉特有偏离），因此这里额外加了守卫而不是照搬。
- 验证：`BuiltinsSessionCreationGuardTest` 5 个用例通过（重入返回同一实例、正常/异常退出都清除登记、
  多键独立、ThreadLocal 语义、同键二次登记报错）；`:analysis:low-level-api-cfir:test` 136 条 / 4 失败，
  4 条与基线相同（Cjmp CallerFirst 既有失败），新增 5 条全绿。

- 验证：`:modules:ide:base:compileKotlin` 通过（复合构建指向 worktree）。

## §2 崩溃根因：机制定案（方案 §2.4）

- 机制：当时 `accepts = isSupported && readSafely { readPackageFqName != null }`，§2.2 四个 null 出口任一为假，
  工厂 `checkNotNull` 就抛 `IllegalStateException: decompiler is not registered`，`FileManagerImpl.createFileViewProvider`
  链路无 catch，入口是 `openFilesOnStartup` / `CodeFoldingNecromancer`。四个出口对用户是同一个表现，
  所以当时的日志无法区分它们。
- 出口 1/3 被字节级证据排除；出口 2（`.cjo` 正在被 cjpm 写入、读到截断内容）与出口 4（VFS 刷新瞬间 `!isValid`）
  与“紧跟 `UnindexedFilesScanner - Reason: On project open`”“同一文件两个 file id + `Reload From Disk`”的现场一致。
  原始日志已随沙箱重建丢失，**具体命中哪个出口无法回溯**，如实记录。
- 关闭：改动 7 之后 `accepts` 只做类型级判断，四个出口与 `accepts` 无关；改动 1 的降级 provider + 各出口一条 warn
  让同类现场下次能从日志区分。残留：启动瞬间 `!isValid` 的标签页停在空文本直到重载（与 Kotlin 同形）。
- 回归覆盖：主仓 `CangJieMetadataStubBuilderTest` 的 `!isValid` 与 broken header 用例；
  IDE `CangJieDecompiledFileViewProviderFactoryTest` 直接打工厂入口（空内容 `.cjo`、`!isValid` `.cjo` 只降级不抛），
  放在 `modules/test-support`（`ide/base` 作为模块没有自己的 `plugin.xml`，其测试环境拿不到 `cjoFileDecompiler` 扩展点）。
  `:modules:test-support:test` 全模块 BUILD SUCCESSFUL。
- 沙箱读数（方案 §9.1/4/5）未做：重建后的沙箱没有项目状态与 `recentProjects.xml`，复现需要 GUI 新建带 SDK 的工程
  并恢复 `.cjo` 标签页；等价入口由 `CjWorkspaceModelSyncStdlibTest` 的重平台用例覆盖（`PsiManager.findFile`、文档文本）。

## 剩余项- 改动 13（描述符防漂移约束）：已实施，`CangJieDecompiledDescriptorParityTest` 3 个用例通过。
  描述符集合从测试宿主 `org.cangnova.cangjie.testSupport.xml` 声明的模块出发取 include 闭包，
  不用类路径全扫描（后者会混进主仓描述符与第三方 maven 元数据）。
- 改动 16/18（启动恢复、事件发布）：代码写在 `CjWorkspaceModelSyncStdlibTest`，已随测试宿主修复一并跑通
  （`testOpeningBuiltinsCjoAfterProjectOpenDoesNotThrow` / `testStdlibRootChangesPublishGlobalModuleStateEventOnlyWhenRootsChange`），
  见下方“测试宿主缺口”一节。
- 改动 15 的 `!isValid` 分支：已实施（覆写 `LightVirtualFile.isValid` 为 false，断言
  `contentsToByteArray` 从未被调用），`decompiler-to-file-stubs` 6 个用例通过。
- 12b（`getBuiltinsSession` 重入守卫 / 半成品 session）：仍未实施，理由同改动组 4。
- §2 崩溃根因：仍未定位；改动 1 提供的两层日志是取证手段，需要跑一次沙箱 IDE 读数。
- 既有失败（与本次无关）：
  - `BuiltinsStubsTest` / `std.argopt.cjo.stubs.txt` 侧车注解缺失，HEAD 上同样失败。
  - `CjWorkspaceModelSyncStdlibTest` 原先整类在项目创建阶段失败，属测试宿主缺口，已在本轮修复（见“测试宿主缺口”一节）。
  - `:modules:ide:base:test` 单独跑时 18 条中 10 条失败（图标注册 2、QuickDocumentation 7、
    SourceHighlighting 1），HEAD 源码与本分支逐条一致；这 10 条与 `.cjo` 无关。
    注意：`:modules:ide:base:test` 与 `:modules:ide:project:test` 放在同一次 Gradle 调用里跑时，
    `CjStubElementType` 静态初始化会撞上 “index 初始化完成后才创建 stub element type”，额外多挂 8 条
    （folding/formatting/decompiled-text-contract/highlighting）。这是任务顺序造成的测试宿主假象，不是代码
    回归；核对回归时必须单独跑一个测试任务。

## 验证基线（2026-10-01 补充）

- 产品测试的“改动前”基线已取到：IDE 仓 detached 到 `f4c373c9`（`e525fd9e~1`，即本次两个 IDE 提交之前）
  + 主仓指向 `df71d7212` 基线工作树、树干净，跑 `:product:idea-plugin:test` 得 **28 条 / 5 失败**，
  与本分支 `cfir-new`（`2c138077`）+ worktree 主仓的干净树结果**逐条相同**：
  `CangJieCompiledFilesHighlightingTest` 3 条（`testLargeStdlibCjoCanOpenInEditor`、
  `testStdlibAstCjoFoldingDescriptorsStayInsideDocumentRange`、
  `testStdlibAstCjoPlaceholderDocumentReloadsAfterToolchainRegistration`）、
  `CangJieQuickFixRegistrationTest::testAbstractMemberNotImplementedRegistersImplementMembersQuickFix`、
  `CangJieReferenceBindingTest::testCjoDeclarationNameProvidesDeclarationTargetAndDocumentation`。
  结论：这 5 条是既有失败，**不是本次 `.cjo` 反编译改动引入的回归**；此前未判定只是因为没有真正的改动前基线。
- 基线工作树 `ide-baseline-df71d7212` 已删除（`git worktree list` 不再包含）。

## 测试宿主缺口（`intellij-ide/modules/test-support`）

- 现象：任何用到项目级服务的重型测试（如 `CjWorkspaceModelSyncStdlibTest`、
  `CangJieDecompiledFileTextContractTest`、`CjProjectOpenedActivityTest`）在创建项目阶段报
  `Cannot find service CangJieProjectStructureProviderService`，`stubElementTypeHolder` 扩展点也未注册。
- 机制（已用探针确认）：宿主 `META-INF/plugin.xml` 用 `xi:include` 引 `org.cangnova.cangjie.testSupport.xml`，
  后者用 `<module name="org.cangnova.cangjie.foundation"/>` 等拉起生产模块；平台解析 `<module>` 时
  （`PluginXmlPathResolver.resolveModuleFile` → `MixedDirAndJarDataLoader`）默认 `config-file` 是
  `META-INF/<name>.xml`，且**只在宿主插件自己的 jar 内查找**。而四个模块描述符位于各自 jar 根
  （`intellij-cangjie.foundation.jar!/org.cangnova.cangjie.foundation.xml` 等），宿主 jar 里没有，
  因此报 “module dependency … cannot be loaded or missing”。改成 `config-file="X.xml"`、`config-file="/X.xml"`、
  把描述符复制进 `src/main/resources/META-INF/` 均无效——因为解析范围是宿主 jar，不是测试类路径。
- 绕过探针：把 `org.cangnova.cangjie.testSupport.xml` 改为对四个生产描述符做 `xi:include`
  （按资源路径经宿主类加载器解析，与产品 `plugin.xml` 聚合方式一致），服务缺失错误消失；
  随后暴露第二层缺口：`NoClassDefFoundError: org/cangnova/cangjie/formatter/CangJieCodeStyleSettings`
  ——`ide/base` 对 code-insight 系列（formatter / folding / highlighting / refactoring）是 `compileOnly`，
  这些 jar 不进 test-support 沙箱 `lib`。探针同时给 test-support 补上 5 个 `testImplementation`
  依赖（与 `ide/base` 的 test 依赖集一致），结果见下。
- 探针结果（`xi:include` 宿主 + 5 个 code-insight `testImplementation`）：整类 11 条里 10 条通过，
  含本次新增的 `testStdlibRootChangesPublishGlobalModuleStateEventOnlyWhenRootsChange`（改动 18 已验证）；
  唯一失败是 `testOpeningBuiltinsCjoAfterProjectOpenDoesNotThrow`（改动 16），失败点是**测试自身**：
  直接在读动作外调 `PsiManager.findFile`，被 `ThreadingAssertions.softAssertReadAccess` 记为 error
  （`Read access is allowed from inside read-action only`），不是产品代码抛错；已改为 `ReadAction.compute`
  包裹并加 `isValid` 断言（把 SDK 路径带进消息）。
- 已落盘的修复（IDE 仓 `cfir-new`）：`org.cangnova.cangjie.testSupport.xml` 改为 `xi:include` 四个生产描述符
  并写明机制与代价；`modules/test-support/build.gradle.kts` 补 5 个 code-insight `testImplementation`；
  `CangJieDecompiledDescriptorParityTest.ideDescriptorResources()` 改为从宿主 `xi:include` 的 href 出发
  （`<module>` 形态作为回退）。
- 复核发现：**宿主描述符放在 main 源集会污染消费者测试**。IDE 其他模块（`product/idea-plugin`、
  `ide/base`、`ide/project`）通过 `testImplementation(testFixtures(project(":modules:test-support")))`
  把 test-support 的 main 资源带进自己的测试 classpath；平台在单元测试模式下按 classpath 扫描
  `META-INF/plugin.xml`，于是“CangJie Test Support”作为一个独立插件被加载，与产品插件重复注册服务，
  `:product:idea-plugin:test` 28 条全挂在
  `CjSdkRegistry is already registered: CjSdkRegistryImpl`（`loadAppInUnitTestMode` 阶段）。
  这正是原 `plugin.xml` 注释警告的情形。
- 探针 9（描述符移入 `src/test/resources/`）：产品侧恢复原状（`:product:idea-plugin:test` 28/5，与改动前基线逐条相同）、
  `:modules:ide:base:test` 18/10（与 HEAD 一致）、`:modules:ide:project:test` 全绿；
  但 test-support 自身整类退回“找不到服务” (`Cannot find service CangJieProjectStructureProviderService` /
  `CangJieMessageBusProvider`)，描述符类路径断言也失败——**插件描述符只从 main 源集装配**，
  test 源集的资源不进入插件 jar，探针假设不成立。
- 探针 10（在 `PrepareSandboxTask.doFirst` 里替换占位符）：test-support 自身沙箱的 `intellij-cangjie.test-support.jar`
  里 `plugin.xml` 仍是未替换的模板（`xi:include` 计数 0）——插件 jar 由 `processResources`/`composedJar` 在
  沙箱任务之前打好，沙箱阶段再改源文件太晚。同一轮产品侧 28/28 失败是探针自身的副作用：doFirst 把改写后的
  描述符写回了源文件，随后的产品构建重新打包时带上了 include，属于污染而非结论。
- 探针 12（`processResources` 之前生成描述符）：构建脚本引用错误（该约定脚本里 `sourceSets.main` 不可解析），
  未跑起来；结论无。
- 探针 13（宿主描述符独立 jar + 只进 `testRuntimeOnly`）：四组全部符合预期。
  沙箱检查：test-support 的 `lib` 里只有 `cangjie-test-host-descriptor-2.1.4.jar` 带 `plugin.xml`，
  产品沙箱里只有 `idea-plugin-2.1.4.jar`。
  - `:modules:test-support:test`（两个类）14/14 全绿，含 `testOpeningBuiltinsCjoAfterProjectOpenDoesNotThrow`
    （读动作修正后通过，改动 16 得到验证）与 `testStdlibRootChangesPublishGlobalModuleStateEventOnlyWhenRootsChange`
    （改动 18 得到验证）；
  - `:product:idea-plugin:test` 28 条 / 5 失败，与“改动前”基线逐条相同；
  - `:modules:ide:base:test` 18 条 / 10 失败，与 HEAD 一致；`:modules:ide:project:test` 全绿。
- 由探针 9–13 定出的机制（写给下一个人）：
  1. 单元测试模式下平台加载哪些插件，取决于插件沙箱 `lib` 目录里哪些 jar 带 `META-INF/plugin.xml`；
  2. main 源集资源会随 `testFixtures` 依赖进入消费者测试的 classpath，并被平台当插件加载；
  3. test 源集资源不参与插件装配（不进沙箱 jar）；
  4. 插件 jar 内容在 `processResources` 阶段定死，沙箱任务里再改源文件已经太晚。
- 落盘的修复（IDE 仓 `cfir-new`，一次提交）：
  - `src/main/resources/META-INF/plugin.xml` 与 `org.cangnova.cangjie.testSupport.xml` 移到
    `src/test/hostPlugin/`，由 `hostPluginDescriptorJar` 任务打包，只以 `testRuntimeOnly` 进入本模块测试；
  - 宿主 `plugin.xml` 聚合四个生产描述符的 `xi:include`；5 个 code-insight 依赖改为 `testRuntimeOnly`；
  - `CangJieDecompiledDescriptorParityTest.ideDescriptorResources()` 从宿主 `META-INF/plugin.xml` 的 include 出发；
  - `CjWorkspaceModelSyncStdlibTest.testOpeningBuiltinsCjoAfterProjectOpenDoesNotThrow` 整段包进 `ReadAction.compute`。
