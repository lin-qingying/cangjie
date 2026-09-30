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
- 12b（`getBuiltinsSession` 重入守卫 / 半成品 session）：**未实施**。12a 落地后，构造期求值链
  （`contentScope` → 枚举、`declarationProvider` → stub 索引）全部推迟，重入边不再存在；
  此时再加“半成品 session”只会引入一个任何误用都会 `error(...)` 的中间态。依据是代码推导，未在运行期复核。
- 验证：主仓全部生产代码编译通过（含 IDE 复合构建）；`:analysis:decompiled:*`、``:analysis:stubs`、`cfir:cfir-serialization` 如上。

## 改动组 5（IDE 改动 10：`CaIdeCandidateCollector` 按根判断）

- 验证：`:modules:ide:base:compileKotlin` 通过（复合构建指向 worktree）。

## 剩余项

- 改动 13/16/18（IDE 约束测试、启动恢复测试、事件发布测试）：未实施。IDE 仓复合构建读取主检出目录，
  其测试只有在主仓改动合入主检出后才能稳定运行；本次仅做编译级验证。
- 改动 15 的 `!isValid` 分支：`decompiler-to-file-stubs` 测试类路径没有 `VirtualFileWrapper`，未覆盖。
- 既有失败（与本次无关）：`BuiltinsStubsTest` / `std.argopt.cjo.stubs.txt` 侧 car 注解缺失，HEAD 上同样失败。
- §2 崩溃根因：仍未定位；改动 1 提供的两层日志是取证手段，需要跑一次沙箱 IDE 读数。
