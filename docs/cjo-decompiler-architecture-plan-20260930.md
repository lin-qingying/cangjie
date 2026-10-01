# `.cjo` 反编译链架构分析与修复方案

- 日期：2026-09-30（第五版：S4 循环边改为 owner 解析；改动 8 注册清单与模块落点；改动 12 拆 12a/12b；编号统一到 §6；S8 数字与 §14 残句修正）
- 基线：`main` @ 7272bf842
- 触发问题：IDE 启动恢复 `.cjo` 标签页时抛 `IllegalStateException: CangJie .cjo decompiler is not registered for <SDK>/std/*.cjo`
- 范围：主仓 `analysis/decompiled/**`、`analysis/low-level-api-cfir/**`、`cfir/cfir-serialization/**`；IDE 仓 `intellij-ide/modules/ide/base/**`、`intellij-ide/modules/ide/project/**`；对照 `external/kotlin` 的 K2 分析层
- 结论先行：
  1. 崩溃的根因**尚未定位**。`hasStub` 从 2026-05-10 起就是纯头部读（`isSupported && hasMetadata`），不经过 `readFile` / `findOwningModule` / 项目结构，第一版文档 §2 的根因链不成立。排除默认项目、`extensionList` 为空、头部格式与版本之后，只剩 `readPackageFqName` 返回 null 四个出口里的一个，其中 `readSafely` 在 `!isValid` 时静默返回 null 是最可能的一条。**先取证据，再改代码。**
  2. 与 Kotlin 一样成立的结构问题有四项：builtins session 烤死搜索根且 IDE 无失效发布方、符号提供者建 session 时同步遍历 SDK 目录并触碰 stub 索引、provider 每次全量遍历无缓存无取消点、搜索根折叠多处拷贝。IDE 与主仓两份 `.cjo` 描述符在源码层重复但**运行期互斥**（第三版误判为并存），改用约束测试防漂移，不删注册。这些连同诊断取证是第一批可以直接做的改动。
  3. 索引洞（启动时 owner 不可用导致 `.cjo` 无 stub 条目）成立，但作用面是符号侧，编辑器文本路径每次就地构建 stub 不受影响。
  4. CFIR 反序列化层的 session 绑定是仓颉相对 Kotlin 的必要差异，本轮不动。

---

## 1. 现象与证据

### 1.1 运行时证据

沙箱 IDE（`intellij-ide/product/idea-sandbox/IU-2025.3.1`，插件 2.1.4，IU-2025.3.1）：

| 日志 | 会话 | `not registered` 次数 | 涉及文件 |
|---|---|---|---|
| `idea.log` | 2026-09-29 16:35 → 2026-09-30 12:57 | 42 | `std.core` / `std.collection` / `std.unittest` / `std.ast` |
| `idea.1.log` | 2026-09-25 21:44 → 2026-09-29 15:32 | 63 | 同上 |
| 其余 10 份轮转日志 | 7 月至 8 月 | 0 | 无 |

- 项目：`untitled89`。SDK 解析成功（`Resolved stdlib from SDK: CangJie 1.0.5 (cjnative) - x86_64-w64-mingw32`）。
- 抛出位置全部是 `CangJieDecompiledFileViewProviderFactory.createFileViewProvider:27`，调用方是 `FileManagerImpl.createFileViewProvider`，最外层是 `openFilesOnStartup` 与 `CodeFoldingNecromancer`。
- 2026-09-27 12:35:56 与 2026-09-30 12:37:06 两个首次出现点，都紧跟在 `UnindexedFilesScanner - Reason: On project open` 之后。

> 沙箱目录在分析期间被并行的 Gradle 任务重建，以上日志的原始文件已不可读；上表来自分析过程中的逐次读取记录。后续结论不依赖已消失的日志帧。

### 1.2 伴随但独立的两个现象

- `java.lang.NoClassDefFoundError: Could not initialize class com.intellij.openapi.progress.impl.CheckCanceledEvent`，栈为 `CjDecompiledFile.getText` → stub 构建 → `CfirDeclDeserializer.deserializeReference` → `LLCangJieStubBasedLibrarySymbolProvider` → `CaIdeBuiltinsModule.getContentScope` → `BuiltinsVirtualFileProvider.getBuiltinVirtualFiles` → `VfsUtilCore.checkCanceled`。首次出现在 2026-09-25 21:44:16。该栈是单次读取记录，**未二次复核**；S5 的重入环由代码独立推导得出。
- `FileBasedIndexImpl - Indexing process should not rely on non-indexed file data`，同一个 `std.io.cjo` 出现两个 file id，`Last Action: Reload From Disk`，2026-09-29 16:35:18。

启动期读 `.cjo` 文本的入口不止 `openFilesOnStartup`：`CodeFoldingNecromancer` 也会对每个恢复的标签读 document 文本。两条入口最终都汇到 `CjoBinaryFileDecompiler.decompile` → `PsiManager.findFile`。

---

## 2. 崩溃根因：机制已定案（2026-10-01 补记，见 §2.4）

### 2.1 已排除

| 候选 | 排除依据 |
|---|---|
| `extensionList` 为空 | IDE 打包产物 `intellij-cangjie.base.jar` 的 23 个 `META-INF/*.xml` 无一引用 `cangjie-analysis-decompiled`；`cangjie-ide-analysis-api-cfir.xml` 只 include impl-base / stubs / cj-references / low-level-api-cfir / symbol-light-declarations 五个描述符，插件只加载自己的 `META-INF/plugin.xml`（`cn.cangnova.cangjie`）。`decompiled-1.1.1.jar` 里那份主仓描述符在类路径上但不在 include 链里，这是 `cangjie-ide-analysis-api-cfir.xml:3-6` 注释声明的**故意互斥**。因此 IDE 里 `cjoFileDecompiler` 恰一个实现、`BuiltinsVirtualFileProvider` 绑定 `IdeBuiltinsVirtualFileProviderImpl`（主仓测试容器里则绑定 CLI 实现） |
| 头部格式 / 版本门 | **字节级证据**：真实 SDK 三个版本（1.0.1、1.0.4、1.1.3）的 `std.core.cjo` 解码结果均为标识 `CJOF`、`version` 1.0.x、`cjoVersion` 0.1.0、`fullPkgName` `std.core`，vtable 槽位 4/6/8 对应字段 id 0/1/2；`CjoConstants` 为 0.1.0，`cjc` 1.0.5 写入相同值（`CjoManager.h:25-27`）；`CjoBinaryFileReaderTest` 用真实 SDK `.cjo` 断言 `std.core`；仓库 fixture `cfir/cfir-serialization/testResources/cjo-sdk`（README 写明复制自本机 SDK）里的 `std/std.core.cjo` 解码为 cjc 1.0.5、`cjoVersion` 0.1.0、`fullPkgName` `std.core`，与崩溃日志里的 SDK 版本完全一致 |
| `PackageFormat` 类缺失 | `deveco/product/build.gradle.kts:20-28` 记录了这个静默失败模式：`CjoBinaryFileReader.readPackageFqName` 运行期直接引用 `PackageFormat.Package`，类缺失时 `NoClassDefFoundError` 被 `runCatching` 吞成 null、不产生任何日志。当前沙箱 `lib/` 有 `flatbuffers-gen-1.1.1.jar`（483 个 `PackageFormat` 条目）与 `flatbuffers-java-25.2.10.jar`，`cfir-serialization → flatbuffers-gen` 的 `api` 依赖链（自 2026-03-18）把它带进运行时类路径；2.1.4 那次构建的 `lib/` 已随沙箱重建无法复核 |
| `readFile` / `findOwningModule` 参与 `accepts` | `CangJieBuiltInMetadataStubBuilder.hasMetadata` 从 `88b8bccbc`（2026-05-10）起就是 `CjoBinaryFileReader.readPackageFqName(virtualFile) != null`，纯头部读。`hasStub = isSupported && hasMetadata`（`CangJieMetadataStubBuilder.kt:53`），子类是唯一子类 |
| 默认项目分支 | `FileManagerImpl.createFileViewProvider`（2025.3）链路是 `getFileType` → `LanguageUtil.getLanguageForPsi` → `FileTypeFileViewProviders.forFileType` → 我们的工厂；无 `isBinary` 判断、无 `BinaryFileStubBuilders` 查询、无项目就绪检查。`createFileViewProvider` 里的 `!manager.project.isDefault` 守卫在工厂之后，走不到 |

### 2.2 剩下的唯一路径

`accepts(file)` = `isSupported(file) && readSafely { readPackageFqName(file) }`。`isSupported`（`:45-48`）比较扩展名与 `CangJieBuiltInFileType`，SDK `.cjo` 通过。于是只剩 `readPackageFqName` 返回 null（`CjoBinaryFileReader.kt:23-29`），四个出口：

1. `isCjoBinaryFile(file)` 为 false：扩展名不是 `cjo` 且 fileType 不是 `CangJieBuiltInFileType`。SDK 目录下的 `.cjo` 不满足。
2. `contentsToByteArray()` 抛异常、flatbuffers 根表解析失败、或 `PackageFormat` 类缺失（`NoClassDefFoundError`），全部被 `CjoBinaryFileReader.kt:25` 的 `runCatching { ... }.getOrNull()` 吞掉，**异常不会到达工厂**。读链本身无取消点（`VirtualFileImpl.contentsToByteArray` → `checkNotTooLarge`（上限 20 MB，SDK `.cjo` 约 2 MB 不适用）→ `PersistentFSImpl.contentsToByteArray`，全程无 `checkCanceled`；`PsiManagerImpl.findFile` 的 `checkCanceled` 在进工厂之前），所以 `ProcessCanceledException` 被吞这一形态可排除。最可能的是 `.cjo` 正被 cjpm 写入或替换时读到截断内容，flatbuffers 抛越界——与 1.1 里“紧跟 `UnindexedFilesScanner`、时间点在 `CangJieProjectSyncTask` 期间”吻合 |
3. `fullPkgName` 为空。
4. **外层 `readSafely`（`CangJieMetadataStubBuilder.kt:119-127`）在 `!file.isValid` 时直接返回 null，不记日志。**

出口 2（截断内容）与出口 4（`isValid` 为 false）都是静默失败点，日志里没有任何线索。出口 4 的机理是：VFS 刷新瞬间打开标签页时 `file.isValid` 为 false，`accepts` 变 false，工厂抛异常。两者都只是推测，靠 §2.3 的诊断日志定案。

### 2.3 下一步：先取证据

诊断要分两层：四个出口里只有 `!isValid` 能被工厂层区分，出口 1/2/3 都表现为读取层返回 null，异常不会到达工厂。

- **读取层** `CjoBinaryFileReader.readPackageFqName`（`:23-29`）：把 `runCatching { ... }.getOrNull()` 改为 `getOrElse { t -> if (t is ControlFlowException || t is CancellationException) throw t; logger.warn("...", t); null }`——`ControlFlowException` / `CancellationException` 必须先重抛，不要当普通失败记日志；其余打印 `Throwable::class.java.name` 与 message；同时把出口 1（`isCjoBinaryFile` 为 false）与出口 3（`fullPkgName` 为空）各记一条，否则它们在两层都是静默 null。
- **工厂层** `CangJieDecompiledFileViewProviderFactory.createFileViewProvider` 的 null 分支：记录 `file.isValid`、`file.fileType.name`、`file.length`、`extensionList.size`、`readPackageFqName(file)` 的返回值。

重跑沙箱，看 `idea.log` 里这两层。读数即定案：

| 日志显示 | 结论 | 对应改动 |
|---|---|---|
| 工厂层 `isValid=false` | VFS 瞬时失效；正确修法是 `accepts` 对 `!isValid` 返回 true（类型级）或 `readSafely` 在 `!isValid` 时重试 | 改动 7（`accepts = isSupported`）即可，同时消除整份读取 |
| 读取层有异常且工厂层 `isValid=true` | `.cjo` 正被 cjpm 写入/替换，读到截断内容，flatbuffers 抛越界 | 记录并按具体异常修；读取层日志里带 `file.length` 与 `isValid` 佐证 |
| `extensionList.size == 0` | 描述符加载在当前 IDE 与本仓源码树之间不一致 | 比对沙箱 `plugins/idea-plugin/lib` 的描述符 |
| 读取层有 `NoClassDefFoundError` | 运行时缺 `PackageFormat` | 按 `deveco/product/build.gradle.kts:20-28` 的打包说明排查 |

无论哪种，**改动 1（读取层与工厂层诊断 + null-safe）都应先做**：它把异常变成可诊断的降级，且不依赖根因。

### 2.4 结论（2026-10-01）

- **崩溃机制已定案**：工厂 `checkNotNull` 在 `accepts` 返回 false 时抛 `IllegalStateException: decompiler is not registered`，而 `FileManagerImpl.createFileViewProvider` 链路没有 catch，入口是 `openFilesOnStartup` 与 `CodeFoldingNecromancer`。§2.2 的四个 null 出口里任何一条都会让当时的 `accepts = isSupported && readSafely { readPackageFqName != null }` 变 false——四个出口对用户是同一个表现（日志里的 `not registered`），这解释了为什么日志无法区分它们。
- **具体命中哪个出口无法回溯**：出口 1（类型/扩展名）与出口 3（空包名）被字节级证据排除（§2.1）；出口 2（`.cjo` 正在被 cjpm 写入/替换，读到截断内容）与出口 4（VFS 刷新瞬间 `!isValid`）都与 §1.1 的时间点（紧跟 `UnindexedFilesScanner - Reason: On project open`）和 §1.2 的第三个现象（同一个 `std.io.cjo` 出现两个 file id、`Last Action: Reload From Disk`）一致，且原始日志已随沙箱重建丢失（§11）。
- **关闭方式**：改动 7 之后 `accepts` 只做类型级判断（`getStubBuilder().isSupported(file)`），不读内容，四个出口与 `accepts` 无关；改动 1 把工厂的 `checkNotNull` 换成降级 provider，并把四个出口各留一条 warn。因此这两类瞬态现场现在都表现为“打开 `.cjo` 得到空文本/诊断占位 + 日志一行”，不再抛异常，下次能从日志区分是 `!isValid` 还是头部读失败。
- **残留行为（与 Kotlin 同形）**：启动瞬间 `!isValid` 的那个标签页会停在空文本上，直到文件重载或标签页重新打开；Kotlin `KotlinMetadataDecompiler` 在 metadata 读不出时同样返回空 PSI 文件，行为一致，不额外做重试。
- **回归覆盖**：`CangJieMetadataStubBuilderTest.invalidFileIsRejectedWithoutHeaderRead`（`!isValid` 不触发头部读）、
  同类的 broken header 用例，以及 IDE 侧 `CangJieDecompiledFileViewProviderFactoryTest`
  （空内容 `.cjo` 与 `!isValid` `.cjo` 直接打工厂入口，只降级、不抛异常）。
- **沙箱读数未做**：§9.1/4/5 需要在重建后的沙箱里新建带 SDK 的工程并恢复 `.cjo` 标签页，依赖 GUI 操作，
  本轮未做；等价入口（`PsiManager.findFile`、文档文本、stub 非空）由 `CjWorkspaceModelSyncStdlibTest` 的重平台用例覆盖。

---

## 3. 与 Kotlin 的对位（只保留核实成立的部分）

| 环节 | Kotlin K2（`external/kotlin`） | 仓颉现状 | 核实 |
|---|---|---|---|
| `accepts()` | `stubBuilder.isSupported(file)` | `getStubBuilder().hasStub(file)` = `isSupported && hasMetadata` | 成立 |
| 底层 `readFile` | `(virtualFile, content)`，纯字节，无 Project | `(virtualFile, content, project)`，要 project 非空并经三个 project service | 成立 |
| 宿主覆盖“内容级读法”的接点 | `KotlinBuiltInDecompilationInterceptor`（`K2IdeBuiltiInSupport.kt`）：`KotlinBuiltInMetadataStubBuilder.readFile` = `interceptor.readFile(...) ?: BuiltInDefinitionFile.read(...)`。Kotlin 需要它是因为 `.kotlin_builtins` 是 classpath 资源、没有 `VirtualFile` | 不适用：仓颉 `.cjo` 就是 SDK 磁盘文件，宿主实现本身就是 `CangJieBuiltInMetadataStubBuilder` 这个子类覆写 | — |
| `createFileViewProvider` 内唯一一次内容级检查失败 | 返回 `null` 文件 | 返回 `null` 文件 | 成立 |
| stub 构建是否引用 session | `KotlinClsStubBuilder`、`ClsStubBuilderComponents` 零 `session` 引用 | `CjoDeclarationLoader` 构造 `CfirDeserializationContext(moduleData = ...)`；stub 路径读 7 个 session 组件：`languageVersionSettings`、`implicitSystemAnnotations`、`cjmpSettings`、`cjmpLoadDiagnostics`（可空）、`cangjieScopeProvider`（硬需求）、`interopSettings`、`cfirAbiPolicy`（后两个有默认值） | 成立 |
| builtins 来源 | 插件 classpath（`getResourceAsStream`），与项目结构无关 | SDK 磁盘目录，根来自 `BuiltinsVirtualFileProvider` → SDK 设置 | 成立 |
| builtins 模块 scope | `KaBuiltinsModuleImpl.baseContentScope` 同样调 `BuiltinsVirtualFileProvider.createBuiltinsScope` | `CaBuiltinsModuleImpl.baseContentScope` 形状相同 | 同形，**不是**“Kotlin 不依赖项目结构”的反例 |
| builtins 失效事件 | `KaBuiltinsSessionFactory` 的注释明确：libraries/builtins 不能被 out-of-block 修改影响；走 `KaModuleStateModificationEvent` / `KaGlobalModuleStateModificationEvent` | 我们的 `LLCfirSessionCacheStorageInvalidator.kt:69-97` 同一套；IDE 侧一处发布都没有 | 成立 |
| `.kotlin_builtins` 的 `fileType.fileViewProviderFactory` | **生产 XML 不注册**（`analysis-api-impl-base.xml:54` 只对 KJSM 注册）；只在测试 registrar 里以代码方式注册 | 生产 XML 注册 | 成立 |
| `filetype.decompiler` | Kotlin 生产 XML 不注册 | 注册了 `CjoBinaryFileDecompiler` | 成立 |
| “工厂 null-safe 对位 Kotlin” | 无对应物（Kotlin 不注册该工厂） | 值得做，但不是对位 | 结论保留，比喻删除 |
| builtins stub 索引回边 | `LLFirBuiltinsSessionFactory`（`external/kotlin/analysis/low-level-api-fir/.../LLFirBuiltinsSessionFactory.kt:45-47`）同样是 `ConcurrentHashMap` + `getOrPut`、同样无重入保护；差别只在 Kotlin 的 builtins 不经 stub 索引 | 我们的 builtins session 构造期建 `LLCangJieStubBasedLibrarySymbolProvider`（读 stub 索引），因此多出 S4 那条 owner 解析回边 | 仓颉特有；不适用 Kotlin |
| 找不到 decompiler | `KotlinStandaloneIndexBuilder`（`analysis-api-standalone-base`）`?: return null` | 工厂 `checkNotNull` 抛 | 成立 |

仓颉多出来的约束是真的：`CfirDeserializationContext` 的 `implicitSystemAnnotations` 与 `languageVersionSettings` 取自 `moduleData.session`；`CjoFullIdResolver` 依赖 `CjoManager` 的搜索根；`.cjo` 的 `cjoVersion` 是格式版本，缺版本即拒收。但这些约束“只影响声明的 owner”，第一版文档把它们的影响面误判到了 `accepts`。

---

## 4. 定位到的结构问题

### S1 文件类型层的工厂用异常表达“不支持”

`checkNotNull` 抛在平台调用链里（`FileManagerImpl.createFileViewProvider` 直接调我们的工厂，异常无 catch）。`checkNotNull` 自 `88b8bccbc` 就在，不是回归。这是问题 2（阻塞编辑器恢复标签页）与诊断困难的共同来源。

### S2 `accepts` 每次同步读整份 `.cjo`

`hasStub` → `hasMetadata` → `CjoBinaryFileReader.readPackageFqName` → `binaryFile.contentsToByteArray()`。实测 `std.core.cjo` 1.97 MB、`std.unittest.cjo` 1.39 MB、`std.collection.cjo` 1.09 MB。每次打开 `.cjo` 至少读两次：`CjoFileDecompilers.find` 里的 `accepts` 一次，`CangJieMetadataDecompiler.createFileViewProvider:43` 里的 `hasStub` 一次（与实现个数无关）。平台每次 `acceptsFile`（`rebuildStubTree`）也读一次。`accepts = isSupported` 后这些读全部消失。

### S3 builtins session 把搜索根烤死，IDE 没有失效发布方

`LLCfirBuiltinsSessionFactory.createBuiltinsSession`（`:113-139`）在建 session 时同步调 `createBuiltinsSymbolProvider`，里面立刻取 builtins 范围并拼搜索根。SDK 目录当时没进 VFS 就得到空根，此后 session 一直空。

失效机制齐，缺发布方：

- `LLCfirSessionInvalidationService.LLCangJieModificationEventListener` 已注册（`cangjie-low-level-api-cfir.xml:51`），`invalidate` 对 `KotlinModuleStateModificationEvent(CaBuiltinsModule)` 与 `KotlinGlobalModuleStateModificationEvent` 都走 `invalidateAll(includeLibraryModules = true)`（`:75`、`:90`）。
- `CaModule.publishModuleStateModificationEvent` 与 `Project.publishGlobalModuleStateModificationEvent` 已定义（`modification/utils.kt:23,39`）。
- 全仓搜这两个发布函数，调用者只有 `analysis-test-framework` 的 `ModificationEventDirectives`。IDE 侧一处都没有。`CangJieIdeOutOfBlockModificationService` 发的是 `publishGlobalSourceOutOfBlockModificationEvent`，invalidator 对它走 `includeLibraryModules = false`（`:96`）。

结论：修复点是补发 `publishGlobalModuleStateModificationEvent`（写动作内），不是调 invalidator。

### S4 builtins 范围被项目结构、符号提供者、解析范围三处触发

- `CaIdeCandidateCollector.collectBuiltinsCandidates`（`:26-33`）用 `virtualFile in builtinsModule.contentScope`；`CaIdeBuiltinsModule.contentScope`（`:26-27`）调 `createBuiltinsScope` → `getBuiltinVirtualFiles` → `iterateChildrenRecursively`。
- **同线程重入的循环边是 owner 解析**：`getBuiltinsSession`（`:81-86`）用 `getOrPut` 包装 `CachedValuesManager.createCachedValue`；建 session 过程中（`:130-132` 构造 `createBuiltinsSymbolProvider`）若触发某个 builtins `.cjo` 的 stub 构建，`CjoFileStubBuilder` → `CjoDeclarationLoader` → `readFile` → `DecompiledCjoModuleDataProvider.getModuleData`（`:35-36` 对 `CaBuiltinsModule` 走 `getBuiltinsSession(targetPlatform).moduleData`）会同线程重入 `getBuiltinsSession`。这条边**不是**经 `CfirPackageMemberScope.kt:42` 的默认实参 `symbolProvider = session.symbolProvider`：反序列化路径在 `AbstractCfirDeserializedSymbolProvider.kt:194` 调 `getPackageMemberScope(packageFqName, this, session, scopeSession)`，`CfirCangJieScopeProvider.kt:96` 用第三个位置实参把 provider 显式传入自己，默认实参不求值。
- IDE 侧 `CaIdeBuiltinsModule.contentScope` 经候选收集在 `.cjo` 元素的 use-site module 选择中被读到（`CangJieProjectStructureProvider.getModule` → `CaIdeCandidateCollector`），这是另一条从 PSI 回到 builtins 范围的边。
- `CaBaseResolutionScope.buildSearchScope`（`:63-74`）对每个不含 builtins 的可分析模块集都会 `add(createBuiltinsScope(project))`；`CaBaseResolutionScopeProvider` 在 IDE 侧被 `CaIdeProjectStructureState.kt:92` 直接实例化，在 standalone 侧被 `CaStandaloneProjectStructure.kt:81` 实例化。这是与 Kotlin `KaBaseResolutionScopeProvider` 同形的对位代码，**不是死代码**。
- IDE 唯一 `CaContentScopeRefiner` 是 `CaIdeResolveScopeEnlargerBridge`，只为 source module 取 `CangJieResolveScopeEnlarger.enlargeScope(EMPTY, openapiModule)`，而该 EP 无任何实现注册，结果恒为空。

第一版文档写“创建 builtins session 只有两条路径”，低估了 `CaBaseResolutionScope` 这一条。

### S5 provider 既是根来源又是文件枚举

`BuiltinsVirtualFileProviderBaseImpl.getBuiltinVirtualFiles`（`:73-87`）每次 `flatMapTo(::collectBuiltinFiles)`，目录场景下 `iterateChildrenRecursively` 访问器恒返回 `true`、无 `checkCanceled`（`:98-109`）。`CheckCanceledEvent` 的类初始化错误就在这条路上。生产调用点 7 个：主仓 5 个（`CaBaseResolutionScope:83`、`CaBuiltinsModuleImpl:34`、`DecompiledBinaryIndexImpl:230`、`LLStubOriginLibrarySymbolProviderFactory:54`、`LLBinaryOriginLibrarySymbolProviderFactory:84`），IDE 2 个（`CaIdeBuiltinsModule:27`、`BuiltinsDecompiledDocumentRefresher:23`）。测试 7 个文件（IDE 两个：`CangJieCompiledFilesHighlightingTest:780`、`CangJieReferenceBindingTest:315`），测试 fixture 1 个（`CaSourceModuleImpl:196`）。

### S6 搜索根折叠多处拷贝

`DecompiledPackageDataFinder.toBuiltinsSearchRoot`（`:156-165`）、`LLBinaryOriginLibrarySymbolProviderFactory.toBuiltinsSearchRoot`（`:107-116`）、`DecompiledCjoRepository`（`:46-64` 的 `libraryRootPathString` / `builtinsRootPathString` / `cjoManager`）各自拼一次；测试 fixture `BuiltinsVirtualFileProviderTestImpl` 又一份。

### S7 IDE 与主仓两份 `.cjo` 描述符：源码级重复、运行期互斥

`intellij-ide/modules/ide/base/.../cangjie-decompiled.xml` 在源码层整条重注册了主仓描述符的 8 项非 provider 内容：扩展点声明（`:3-7`）、`CangJieBuiltInDecompiler`（`:10`）、`filetype.stubBuilder`（`:14-16`）、`filetype.decompiler`（`:17-19`）、`fileType.fileViewProviderFactory`（`:20-22`）、`stubElementTypeHolder`（`:23-25`，`CjStubElementTypes`，`externalIdPrefix="cangjie."`，**主仓任何 XML 都没有对应项，IDE 独有**）、三个 projectService（`:29-35`）；加上 `:26-28` 覆盖 `BuiltinsVirtualFileProvider` 实现。

**运行期互斥**：IDE 只加载自己的 `META-INF/plugin.xml`，其 include 链不含主仓的 `cangjie-analysis-decompiled.xml`（`cangjie-ide-analysis-api-cfir.xml:3-6` 注释明示）。所以 IDE 里 `cjoFileDecompiler` 恰一个实现、`BuiltinsVirtualFileProvider` 绑定 IDE 实现；主仓测试容器加载主仓描述符，绑定 CLI 实现。两边各自是自洽的。

真实的问题是**漂移**：主仓新增一个注册项时 IDE 描述符不会自动跟上（已经发生过：主仓有 `filetype.stubBuilder`，IDE 描述符 5-11 的历史版本没有）。修法是加约束测试（改动 13），不是删注册——删掉会让 IDE 里 `.cjo` 整条链失去 EP、stubBuilder、decompiler、view provider 工厂与三个 projectService。

唯一的残余风险：主仓描述符物理上在插件类路径上（`decompiled-1.1.1.jar`）；按静态判断平台不扫描它，但若判断有误第二个 `cjoFileDecompiler` 会出现，改动 1 的 `extensionList.size` 日志会暴露。

### S8 过期编译产物被 Git 跟踪

`git ls-files` 里有 **120** 个 `out/` 下的文件被跟踪：`out/production` 82（9 份描述符 + `llvm-interop/llvm-interop-api` 44 = 43 个 class + 1 个 `kotlin_module` + `compiler/arguments` 29 = 28 个 class + 1 个 `kotlin_module`），非 production 38（`compiler/codegen/out/test/resources` 33、`chir/chir-tree/out/test/resources` 2、`cfir/cfir-serialization/out/test/resources/cjo-sdk/README.md` 1、`.workbuddy/tmp/cjmp_probe/out` 2）。`out/test/resources` 是 IntelliJ 布局的旧副本（`project-tests-convention` 的 `projectDefault()` 测试资源只认 `testResources`），取消跟踪对 Gradle 无影响。`.gitignore:12-14` 已覆盖 `bin/`（跟踪数 0），未覆盖 `out/`。可一并清掉的日志：5 个 `hs_err_pid*.log`、`cfir/checkers/checkers-component-generator/compile.log`、`tmp/decompiled-compile.log`；`.workbuddy/memory/*.md` 是工具记忆目录，不动。

### S9 索引洞（启动时 owner 不可用导致 `.cjo` 无 stub 条目）

平台侧（2025.3，javap 反汇编）：

- `BinaryFileStubBuilder` 只有 `acceptsFile`、`buildStubTree(FileContent)`、`getStubVersion`。没有从字节就地重建的入口。
- `StubTreeBuilder.buildStubTree` 对 binary 只调 builder 的 `buildStubTree` 再 `handleStubBuilderException`。
- `StubUpdatingIndex` 不读 `acceptsFile`。
- `CoreStubTreeLoader.readOrBuild(Project, VirtualFile, PsiFile)` = `FileContentImpl.createByFile` → `StubTreeBuilder.buildStubTree`，**不读索引**；`rebuildStubTree` 才走 `FileType.isBinary` → `BinaryFileStubBuilders.forFileType(...).acceptsFile`。
- `FileManagerImpl.createFileViewProvider` 零处引用 `BinaryFileStubBuilders`；`isDefault` 分支不是原因。

`CangJieMetadataStubBuilder.buildFileStub`（`:79-82`）在 `readFile` 返回 null 时返回 null。所以索引里没有该文件的条目，直到 `rebuildStubTree`、VFS 变化或显式重建索引。受影响的是符号侧（STUBS 声明提供器读 stub 索引、goto、引用绑定、`CaIdeScopeCangJieFileCollector` 收集）；编辑器文本路径每次就地构建，不受影响。Kotlin 侧行为等价：`KotlinStandaloneIndexBuilder.findSharedDecompiledFile(...) ?: continue`，无 decompiler 时静默跳过该文件。

### S10 非 IDE 容器的索引缓存退化为常量

`DecompiledBinaryIndexImpl.refreshIfNeeded`（`:286-292`）与 `DecompiledPackageDataFinder.refreshIfNeeded`（`:170-174`）用 `getService(CaModificationTracker)?.modificationCount ?: 0L` 做键。`DecompiledPackageDataFinder.repositoryFor`（`:134-140`）的 `RepositoryKey(moduleKey, roots, builtinsRoots)` 已把根并入键。剩余缺口是 `DecompiledBinaryIndexImpl.builtinsIndexes`（只按修改计数清）和 provider 枚举（无缓存）。standalone 描述符注册了 `CaModuleProvider` 与 `CaModificationTracker`（`cangjie-analysis-api-standalone.xml:33,41`），测试容器只装 `CaStandalonePlatformState`（`CjoCompiledTestEnvironment.kt:157`），不注册后者。

---

## 5. 目标架构

不变：CFIR 反序列化层（`CfirDeserializationContext`、`CfirDeclDeserializer`、`CfirTypeDeserializer`、`CjoFullIdResolver`）保持 session 绑定与 eager 类型物化。

IDE 仓 HEAD `f4c373c9`（2026-09-30，“库 `.cjo` 包名改取自 binary index 已解析的 header”）已把 `CaIdePackageProviderFactory.collectCompiledPackageNames` 改为 `getLibraryPackages` / `getBuiltinsPackages` 取索引里已解析的 header，不再逐文件重读字节（`CaIdePackageProviderFactory.kt:99-116`）。这与 S2 同向，方案范围不含它；跑沙箱与验收时以这一版为基线。

要改的边界：

```
┌─ 文件类型层（静态） ────────────────────────────────────────────┐
│ 工厂：找不到 decompiler → 空 view provider，不再抛异常          │
│ createFileViewProvider：唯一一次 hasStub 检查，失败 → null 文件  │
│   （hasStub = isSupported && hasMetadata，与 Kotlin 同形，不动）  │
│ accepts：与 Kotlin 一致，isSupported（证据到位前不落地）         │
└─────────────────────────────────────────────────────────────────┘
┌─ owner 绑定层（状态相关，best-effort） ──────────────────────────┐
│ CjoModuleDataProvider：结构内 → 模块 moduleData                 │
│                        结构外 → DetachedCjoModuleData           │
│                        始终不抛异常，找不到返回 null            │
└─────────────────────────────────────────────────────────────────┘
┌─ 根发现层（宿主相关） ──────────────────────────────────────────┐
│ 根解析与枚举分离：根来自 provider，枚举结果按根 + 修改计数缓存   │
│ 搜索根折叠归一到一个函数                                         │
└─────────────────────────────────────────────────────────────────┘
```

`DetachedCjoModuleData : CfirModuleData` 放 `cfir/cfir-common`（`CfirModuleData` 是 public abstract，9 个抽象成员 `name` / `dependencies` / `refinementDependencies` / `allRefinementDependencies` / `targetPlatform` / `platform` / `isCommon` / `session` / `stableModuleName` 都可平凡实现；`capabilities` 与 `areRedeclarationsEquivalent` 有默认实现）。`DetachedCjoSession : CfirSession` 需要同时看到 `CfirLanguageSettingsComponent`（`cfir-tree`）与 `CfirCangJieScopeProvider`（`cfir-providers`），落 `cfir/providers` 是安全的一处（`cfir-providers/build.gradle.kts:8` 已 `api(project(":cfir:cfir-tree"))`）；落地时确认对 `cfir-common` 的传递可见性。`DetachedCjoModuleDataProvider` 实现放 `analysis/decompiled`，与 `DecompiledCjoModuleDataProvider` 同处。不能放进 `decompiler-to-file-stubs`：`low-level-api-cfir/build.gradle.kts:35` 已依赖它，反向加边成环。

不继承 `LLCfirModuleData` 的两个理由：它的 `session` 返回 `LLCfirSession`（`LLCfirModuleData.kt:97-99`），而 `LLCfirSession`（`:47-63`）要求 `caModule: CaModule`、`builtinTypes: CfirBuiltinTypes` 并实现 `getScopeSession()`，结构外的 `.cjo` 没有任何 `CaModule` 可给；而且 `LLCfirModuleData.session` 的实现是 `boundSession?.let { it as LLCfirSession } ?: LLCfirSessionCache.getSession(...)`，把非 `LLCfirSession` 绑上去会在第一次读 `session` 时抛 `ClassCastException`，不是静默回落。直接实现 `CfirModuleData` 后 `session` 返回 `CfirSession`，整条 fallback 路径不存在。

`DetachedCjoSession` 的注册清单（跑通 `CjoDeclarationLoader` 只需两个必需组件，其余走默认或可空）：

| 组件 | 访问器 | 缺失时 |
|---|---|---|
| `CfirLanguageSettingsComponent`（经 `languageVersionSettings`，`CfirLanguageSettingsComponent.kt:102-104,134-135`） | `sessionComponentAccessor()` | `error(...)` |
| `CfirCangJieScopeProvider`（`CfirSessionProviderExtensions.kt:40`） | `sessionComponentAccessor()` | `error(...)` |
| `implicitSystemAnnotations` | `...WithDefault`（`DefaultCfirImplicitSystemAnnotationsProvider`） | 用默认 |
| `cjmpSettings` | `...WithDefault`（`CfirCjmpSettings.kt:94-95`） | 用默认 |
| `interopSettings` | `...WithDefault`（`CfirInteropSettings.kt:51-52`） | 用默认 |
| `cfirAbiPolicy` | `...WithDefault`（`CfirAbiPolicy.kt:39-41`） | 用默认 |
| `cjmpLoadDiagnostics` | `nullableSessionComponentAccessor()`（`CfirCjmpLoadDiagnostics.kt:65-66`） | null |

`symbolProvider` 不需要注册：反序列化路径显式传 provider（见 S4 第二条）。若有人在半成品 session 上读 `session.symbolProvider`，`ArrayMapOwner.kt:92-95` 的 `getValue` 会 `error("No ... in array owner")`——在 stub 路径被 `CjoFileStubBuilder.kt:27-36` 的 `runCatching` 吞成 `forInvalid`，在分析路径直接抛。

---

## 6. 改动清单

### P0 不依赖根因，可直接做

| # | 文件 | 改动 |
|---|---|---|
| 1 | `.../CjoBinaryFileReader.kt:25`；`.../CangJieDecompiledFileViewProviderFactory.kt:27-30` | 两层诊断 + null-safe：读取层把 `runCatching { }.getOrNull()` 改为 `getOrElse { logger.warn(Throwable 类名与 message); null }`，并为 `isCjoBinaryFile` false、`fullPkgName` 空两个出口各记一条；工厂层 `checkNotNull` 改为返回空 `CangJieDecompiledFileViewProvider`，记录 `isValid`、`fileType.name`、`length`、`extensionList.size`、头读返回值。既是修复也是 §2.3 的取证手段 |
| 2 | `.../CjoBinaryFileDecompiler.kt:23-31` | 同样 null-safe；`.cjo` 打开失败时返回占位文本而非让异常冒到 `LoadTextUtil` |
| 3 | `intellij-ide/modules/ide/project/.../CjProjectsServiceImpl.kt:279-283`；`CjWorkspaceModelSync.kt:268` | 在 builtins/library 根列表**实际变化时**，于 `workspaceModel.update { }` 返回之后、写动作内补发 `publishGlobalModuleStateModificationEvent()`：`invokeAndWaitIfNeeded { runWriteAction { … } }`（`publishModificationEvent` 带 `ThreadingAssertions.assertWriteAccess()`；`CangJieProjectSettingsService.kt:106` 的 `setProjectSdkId` 既可能来自 EDT 设置页也可能来自后台，而 `workspaceModel.update {}` 是挂起事务不能被写动作包住）。`syncProject` 每次项目打开和依赖变化都跑，事件会走 `invalidateAll(includeLibraryModules = true)` 丢掉全部 session，所以必须先比对根列表再发。随后 `BuiltinsDecompiledDocumentRefresher.refresh` |
| 4 | `git rm -r --cached` 全部 120 个 `out/` 下跟踪文件（`out/production` 82：9 份描述符 + llvm-interop 44 + arguments 29；非 production 38：codegen 33 + chir-tree 2 + cjo-sdk README 1 + `.workbuddy/tmp` 2），`.gitignore` 加 `out/`（S8） |
| 5 | `decompiler-to-file-stubs/.../DecompiledCjoRepository.kt`（新函数放此处）、`DecompiledPackageDataFinder.kt:156-165`、`LLBinaryOriginLibrarySymbolProviderFactory.kt:107-116` | 调用方直接调（`low-level-api-cfir/build.gradle.kts:35` 已依赖该模块）；测试 fixture `BuiltinsVirtualFileProviderTestImpl.toBuiltinRoot` 是 VirtualFile→根的反向折叠，保留但不算重复项 |

### P1 证据到位后再定

| # | 文件 | 改动 |
|---|---|---|
| 6 | `.../CangJieMetadataDecompiler.kt:20` | `accepts` 改为 `getStubBuilder().isSupported(file)`。理由是性能与分层（S2），**不是**根因修复。落地前需 §2.3 的日志读数确认 `accepts` 是崩溃点；若读数指向别处，仍应做，因为每次读 1.1–2.0 MB × 2 次实现 × 每次 `acceptsFile` |
| 7 | `cfir/providers`（`DetachedCjoSession`）、`cfir/cfir-common`（`DetachedCjoModuleData`）、`analysis/decompiled/.../DecompiledCjoModuleDataProvider.kt` | 按 §5 的注册清单只注册 `CfirLanguageSettingsComponent` 与 `CfirCangJieScopeProvider`；`DetachedCjoModuleDataProvider` 与 `DecompiledCjoModuleDataProvider` 同处，按“结构内优先、结构外 detached”选择 |
| 8 | `.../CangJieBuiltInMetadataStubBuilder.kt:41-65` | `readFile` 的 owner 解析对结构外文件降级为 detached；结构外 `.cjo` 的 `CjoManager` 用“文件所在目录 + builtins 根”构造，补索引洞（S9） |

### P2 重入与缓存

| # | 文件 | 改动 |
|---|---|---|
| 9 | `.../BuiltinsVirtualFileProvider.kt:73-109` | 枚举结果按“根列表 + `CaModificationTracker`”缓存；`iterateChildrenRecursively` 加 `ProgressManager.checkCanceled` |
| 10 | `intellij-ide/.../CaIdeCandidateCollector.kt:26-33` | builtins 候选改为根路径包含判断，不走 `contentScope` |
| 11 | `analysis/low-level-api-cfir/.../LLCfirBuiltinsSessionFactory.kt:59,81-86,130-140`；`symbolProviders/LLCangJieStubBasedLibrarySymbolProvider.kt:97-100,107` | 拆成两个提交：**(12a) 断开构造期求值**：`LLCangJieStubBasedLibrarySymbolProvider` 的 `declarationProvider`（`:97-100` 构造期即 `project.createDeclarationProvider(scope, contextualModule = session.caModule)`）与 `symbolNamesProvider`（`:107` 挂在它上面）改为首次查询再建——需要一个 lazy 代理组件（本仓只有 `CfirLazyDeclarationResolver`，无现成的 lazy symbol provider），工作量按半天估；同时删掉 `createBuiltinsSession:120` 的 `registerIdeComponents` 第三个实参（`builtinsModule.contentScope`，`CaIdeBuiltinsModule.contentScope` 每次访问重新遍历 SDK 目录；`registerIdeComponents` 的 `annotationSearchScope` 形参在 `sessionFactoryHelpers.kt:41-77` 函数体内未被使用）。**(12b) 重入守卫**：保留 `getOrPut`，加 `ThreadLocal` 标记；命中时返回**半成品 session**，前提是回调方（`readFile` 的 owner 解析）只碰 `:120-129` 已注册的组件（`registerIdeComponents` / `registerCommonComponents` / `registerModuleData` / `CfirCangJieScopeProvider`），而 `CfirSymbolProvider` 在 `:135-136` 最后才注册；半成品上读 `session.symbolProvider` 会 `error(...)`（`ArrayMapOwner.kt:92-95`），需加断言。**不要换成 `computeIfAbsent`**：`builtinsSessions` 是 `ConcurrentHashMap`（`:59`），映射函数内经 `createBuiltinsSession` → `getBuiltinsModule` 再次访问同一 map 会抛 `IllegalStateException: Recursive update`（Kotlin 侧同样是 `ConcurrentHashMap` + `getOrPut`，无先例）。12b 依赖 12a |
| 12 | `analysis/decompiled/.../DecompiledBinaryIndexImpl.kt:228-231,286-292` | `builtinsIndexes` 键加入根列表摘要；`refreshIfNeeded` 缺服务时不再退化为常量（S10） |

### P3 测试

| # | 位置 | 覆盖 |
|---|---|---|
| 13 | `intellij-ide/modules/test-support`（新，test 源集已有 `testImplementation(project(":modules:ide:base"))`） | **读类路径资源**而不是解包 jar：`META-INF/cangjie-decompiled.xml`（来自 ide/base 资源）与 `META-INF/analysis-api/cangjie-analysis-decompiled.xml`（来自 `:analysis:decompiled`）都在 test 类路径上；递归解析 IDE 侧全部 `META-INF/*.xml` 的 include 链，断言不存在对 `cangjie-analysis-decompiled.xml` 的引用、`cjoFileDecompiler` 与 `BuiltinsVirtualFileProvider` 各只有一个注册、两份描述符注册集合相同（除 provider 实现与 IDE 独有的 `stubElementTypeHolder`）——防漂移。（主仓描述符是否在 test 类路径上，落地时以实际依赖确认） |
| 14 | `decompiler-to-psi/test/.../CangJieMetadataDecompilerAcceptanceTest.kt`（新） | `accepts` 在有无 project、有无 SDK 下返回值恒为 `isSupported` |
| 15 | `decompiler-to-file-stubs/test/.../CangJieMetadataStubBuilderTest.kt`（新） | `hasMetadata` 在 `project = null` 与真实 project 下结果相同；`!isValid` 时 `readSafely` 的行为 |
| 16 | `intellij-ide/.../ide/decompiled/CangJieDecompiledStartupTest.kt`（新） | 打开项目时已有 `.cjo` 标签，恢复不抛 `IllegalStateException`；折叠状态恢复路径同样不抛 |
| 17 | `decompiler-to-file-stubs/test/.../CangJieMetadataStubBuilderDetachedTest.kt`（新） | 结构外 `.cjo`（`findOwningModule` 为 null）时 `readFile` 返回 `Compatible(detached)` 而非 null；`DetachedCjoModuleData.session` 读到的就是绑定的 `DetachedCjoSession`。这是改动 7/8 唯一的验收点 |
| 18 | `intellij-ide/modules/test-support/src/test/.../CjWorkspaceModelSyncStdlibTest.kt:287` 扩展 | 在 `testProjectSdkConfigPublishesChangeEventsOnlyWhenSdkIdChanges` 旁加一条：SDK 变化且根列表改变时发布全局模块状态事件、根列表不变时不发；事件到达后 builtins session 被丢弃重建 |
| 19 | `analysis/stubs` 现有 | 46 份 `.stubs.txt` + 46 份 `.decompiled.text.cj` = 92 份 golden 不变；变了即回归 |

`CangJieCompiledFilesHighlightingTest.testStdlibAstCjoPlaceholderDocumentReloadsAfterToolchainRegistration`（`:503-560`）已覆盖“SDK 未就绪 → `forInvalid` 占位 → 注册后重载”，占位文本来自 `CjDecompiledFile.readOrBuildCompiledStub`（`:136-148`）的 `forInvalid`，`buildDecompiledText` 对 `CangJieFileStubKind.Invalid` 直接返回 `errorMessage`（`decompiledTextBuilder.kt:187-190`）。第 17 项只需补“项目打开时已有标签”的场景。

---

## 7. 改动 → 修复对象 → 预期效果

| 改动 | 修复的问题 | 触发场景 | 预期效果 |
|---|---|---|---|
| 1 读取层 + 工厂层诊断、null-safe | S1、§2.3 | 头部读失败 / `!isValid` | 不再抛 `IllegalStateException`；读取层打印 Throwable；工厂层记录 `isValid`、`extensionList.size`、头读返回值 |
| 2 decompiler null-safe + 占位文案同源 | S1 | 同上 | 标签页打开失败时显示占位文本；文案与 `forInvalid` 路径共用 `CangJieCompiledFileErrors` 里的新常量，测试按同一常量断言 |
| 3 补发事件 | S3 | SDK 切换、cjpm 依赖变化 | builtins session 被丢弃重建，搜索根不再烤死 |
| 4 清理过期产物 | S8 | 构建 | 运行类路径不带过期描述符 |
| 5 折叠归一 | S6 | 三处调用 | 一处逻辑；测试 fixture 复用 |
| 6 `accepts = isSupported` | S2 | 每次 `find` / `acceptsFile` | 消除 1.1–2.0 MB × 2 的同步读；与 Kotlin 一致 |
| 7 detached session / module data | S9 | 结构外 `.cjo` | 索引有条目；不拉起真实 session；不碰 `LLCfirSession` 构造约束 |
| 8 结构外 owner 解析降级 | S9 | 结构外 `.cjo` | `readFile` 返回 `Compatible(detached)` 而非 null；跨包解析可用 |
| 9 枚举缓存 | S5 | 高频调用 | 避免每次类查找都遍历 SDK 目录；`checkCanceled` 恢复 |
| 10 候选按根判断 | S4 | `.cjo` stub 构建、PSI 选 module | 断开 `.cjo` stub → builtins session → 声明提供器 → `.cjo` stub 的重入环 |
| 11 断开构造期求值（12a）+ 重入守卫（12b） | S3、S4 | 建 session | 建 session 不遍历 SDK 目录、不触碰 `.cjo` stub；半成品 session 的用途加断言 |
| 12 索引键 | S10 | 根变化 | 索引随根失效 |

依赖关系：P0 五组互不依赖，可一次提交。P1 第 6 组依赖 §2.3 的日志读数，第 7、8 组依赖第 6 组。P2 第 9–12 组依赖 3、4 的结果；12b 依赖 12a。第 19 项 92 份 golden 不变是全部改动正确性的判据；第 17 项是 detached 路径唯一的验收点。

---

## 8. 最终效果

| 场景 | 现状 | 全部改完后 |
|---|---|---|
| IDE 启动恢复 SDK `.cjo` 标签 | `IllegalStateException`，日志 105 次 | 不再抛异常；根因修好后正常反编译，否则显示带诊断的占位 |
| IDE 启动、项目 SDK 尚未配置 | 同上 | `forInvalid` 占位（已有），SDK 配置后经事件补发与重载恢复 |
| 打开不属于任何模块的 `.cjo` | `accepts` 为真（头读得到），工厂正常建 view provider；`readFile` → `findOwningModule` 为 null → `buildFileStub` 返回 null → `CjDecompiledFile.readOrBuildCompiledStub:148` 给 `forInvalid` 占位（`CangJieCompiledFilesHighlightingTest.isDecompilerFailurePlaceholder` 认的就是“Could not decompile the file” + “Please report an issue”） | 反编译文本（detached owner），不拉起真实 session |
| `.cjo` 编译于更高版本 `cjc` | 占位 | 占位（`NEWER_VERSION_DECOMPILE_ERROR`），行为不变 |
| 标准库符号解析 | SDK 晚于 builtins session 创建时长期解析不出 | 根随事件失效重建，符号解析恢复 |
| 任意一次源码分析 | 隐式拉起 builtins session，遍历一次 SDK 目录 | 拉起但不遍历；根缓存命中 |
| `.cjo` 打开 / 反编译期间的类初始化异常 | `NoClassDefFoundError: CheckCanceledEvent` | 无（重入环断开） |
| “Reload From Disk” 后索引告警 | `Indexing process should not rely on non-indexed file data` | 随 detached stub 稳定消失；若仍在，是 `dropPsiCaches` 与索引并发，需单独看 |
| 索引里的 `.cjo` stub | 启动时结构未就绪的文件无条目 | 全部有条目；92 份 golden 不变 |

不变：`.cjo` 仍是只读 binary 文件类型；`filetype.decompiler` 与 `fileType.fileViewProviderFactory` 继续注册；`cjoFileDecompiler` 扩展点保留。

---

## 9. 验证

1. P0 落地后重跑沙箱，`idea.log` 中 `not registered` 归零，且新增的诊断行能区分 `isValid=false` / 头部读 null / `extensionList` 为空。
2. 单元测试 14、15、17 通过；IDE 测试 16、18 通过；IDE 打包约束 13 通过；`CangJieCompiledFilesHighlightingTest` 保持绿。
3. `:analysis:decompiled:*:test`、`:analysis:stubs:test`（92 份 golden）全绿且快照未变；`:cfir:cfir-serialization:test` 全绿。
4. 沙箱 `runIde`：`NoClassDefFoundError: CheckCanceledEvent`、`Indexing process should not rely on non-indexed file data` 归零。
5. 索引后 `std.core.cjo` 的 stub 树非空：打开并跳转到 `println` 成功。

---

## 10. 风险与回滚

| 风险 | 缓解 | 回滚 |
|---|---|---|
| 改动 6 在根因未定位时落地，若 `accepts` 不是崩溃点 | 单独提交，注明理由是性能与分层 | 单文件回滚 |
| 改动 3 补发事件在根列表未变时也触发全局失效，代价高于预期 | 先比对根列表再发；测试 19 覆盖“不变不发” | 撤掉两处补发 |
| 改动 7/8 detached 产出的 stub 与真实模块 session 不同 | 第 19 项 golden 不变即证明 | 限制 detached 只用于 `readFile` 判空，不用于 `buildFileStub` |
| 改动 11b 用 `computeIfAbsent` 造成递归更新崩溃 | 方案已排除该写法；代码评审检查重入保护 | 保留 `getOrPut` 不加保护 |

| `CangJieStubVersions.BUILTIN_STUB_VERSION = SOURCE + 4`（`:76`），binary 与 source 共用版本 | 改动只改产出条件不改形状 | 若形状变化，按 `:33-35` 的记录格式递增并写明原因 |

每组改动一个提交。

## 11. 未验证项

- 崩溃的具体出口（§2.2 的 2 还是 4）：原始日志已随沙箱重建丢失，无法回溯；机制已定案并关闭（§2.4）。
- 5-9-25 `NoClassDefFoundError` 栈中 `CaIdeCandidateCollector` 帧的完整重入环，来自单次读取，未二次复核。S4 的代码路径可独立验证。
- `PackageFormat` 类在 2.1.4 那次沙箱构建的 `lib/` 里是否存在（沙箱已重建）。当前构建已确认存在。
- 平台描述符加载器探测的路径集合未用 javap 复核（IU 2025.3 的 `PluginDescriptorLoader` 提取后无字节码输出）；按静态判断它只探测 `META-INF/plugin.xml` 与 jar 目录布局，不扫类路径上的分析描述符。若判断有误，`extensionList.size` 会是 2，改动 1 的日志会暴露。

## 12. 关键代码索引

| 关注点 | 位置 |
|---|---|
| `accepts` 覆写与 `hasStub` | `CangJieBuiltInMetadataStubBuilder.kt:41-42`、`CangJieMetadataStubBuilder.kt:40,45-48,53,56,79-82,119-127` |
| 头部读 | `CjoBinaryFileReader.kt:23-29,37-47,55-58` |
| 工厂 / decompiler | `CangJieDecompiledFileViewProviderFactory.kt:21-31`、`CjoBinaryFileDecompiler.kt:16-31` |
| owner 解析 | `DecompiledPackageDataFinder.kt:58-61,80-105,134-140,152-164,170-174`、`DecompiledCjoModuleDataProvider.kt:22-40` |
| builtins 索引 | `DecompiledBinaryIndexImpl.kt:195-212,228-240,286-292` |
| 反序列化需要的 session 组件 | `CfirDeserializationContext.kt:30-41`、`CfirDeclDeserializer.kt:220,222,1190,1239,1284,1326,1374,2223` |
| 跨包解析 | `CjoFullIdResolver.kt:36,119,165-172`、`AbstractCfirDeserializedSymbolProvider.kt:85-100,172-197,244-270` |
| builtins session | `LLCfirBuiltinsSessionFactory.kt:59,81-86,102-105,113-121`；`sessionFactoryHelpers.kt:41-77`；所有 session 依赖它：`LLCfirAbstractSessionFactory.kt:125,217,314,410,467` |
| builtins 符号提供者 | `LLStubOriginLibrarySymbolProviderFactory.kt:49-57`（IDE）、`LLBinaryOriginLibrarySymbolProviderFactory.kt:83-115`（CLI） |
| 解析范围 | `CaBaseResolutionScope.kt:63-74,82-84`、`CaIdeProjectStructureState.kt:92` |
| 失效机制 | `LLCfirSessionInvalidationService.kt:27-32,46-50`、`LLCfirSessionCacheStorageInvalidator.kt:69-97,186-193`、`modification/utils.kt:23,39` |
| IDE 描述符 | `intellij-ide/.../cangjie-decompiled.xml:1-37`（IDE 独有 `stubElementTypeHolder` 在 `:23-25`）、`cangjie-ide-analysis-api-cfir.xml:2-11`、`org.cangnova.cangjie.ide.base.xml:12` |
| 测试 | `CangJieCompiledFilesHighlightingTest.kt:503-560`、`CjoBinaryFileReaderTest.kt`、`CjoCompiledTestEnvironment.kt:133-160` |

## 13. 平台机制核实记录

| 事实 | 核实方式 | 对方案的影响 |
|---|---|---|
| `BinaryFileStubBuilder` 无从字节重建入口 | `javap -p intellij.platform.core.jar` | S9 成立：null 是一次性的 |
| `StubTreeBuilder` 对 binary 只调 `buildStubTree` | `javap -c`（`core.impl.jar`） | 同上 |
| `StubUpdatingIndex` 不读 `acceptsFile` | `javap -c`（`editor.ex.jar`） | 索引期每个 `.cjo` 都进 `readFile` |
| `CoreStubTreeLoader.readOrBuild` 不读索引；`rebuildStubTree` 才读 `acceptsFile` | `javap -c`（`core.impl.jar`） | 编辑器文本路径就地构建，索引洞只影响符号侧 |
| `FileManagerImpl.createFileViewProvider` 零处引用 `BinaryFileStubBuilders`；链路 `getFileType → getLanguageForPsi → FileTypeFileViewProviders → 工厂` | `javap -c`（IU 2025.3 与 2026.2） | 排除默认项目分支 |
| `FileTypeIndexingHint` 是唯一读 `acceptsFile` 的索引层类型 | `grep -rl acceptsFile` 全 `lib/*.jar` | 我们不受影响 |
| `KaBuiltinsModuleImpl.baseContentScope` 与仓颉同形；`KaBaseResolutionScopeProvider` 同形 | 源码 | 解析范围那条腿是忠实对位，不是偏离 |
| IDE 打包产物 `intellij-cangjie.base.jar` 的 23 个 `META-INF/*.xml` 无一引用 `cangjie-analysis-decompiled.xml`；`cangjie-ide-analysis-api-cfir.xml` 只 include 5 个分析描述符 | `unzip -l` + 逐个 `unzip -p` 扫 jar | 两份描述符运行期互斥，S7 改写为“源码级重复、运行期互斥” |
| 2025.3 平台 jar 在 Gradle transforms 缓存 `idea-253.29346.50-win` | 文件系统 | javap 的 classpath |

## 14. 落地约束

- **空 view provider 与占位文本不是同一件事。** `CangJieDecompiledFileViewProvider` 的 `content` lazy 块（`:37-46`）取 `psiFile?.text ?: ""`，`getContents()`（`:63`）返回 `content.get()`。工厂无 decompiler 时只能给空内容；占位文本走 `createFile` → `forInvalid` → `buildDecompiledText`。
- **修改计数缺失时索引缓存退化为常量。** 见 S10；缓存键要并入根列表。
- **SDK 变化时结构修改计数先于文档刷新。** `CjProjectsServiceImpl` SDK 监听（`:279-283`）顺序是 `incOutOfBlockModificationCount()` → `refresh()` → `refreshProject()`；`CjWorkspaceModelSync.syncProject`（`:268`）内部也 bump 计数。按修改计数做键的缓存会在刷新前失效重算。
- **占位文案必须同源。** 现状占位来自 `CjDecompiledFile.readOrBuildCompiledStub:136-148` 的 `forInvalid("// Could not decompile the file: … / Please report an issue: …")`，`CangJieCompiledFilesHighlightingTest.isDecompilerFailurePlaceholder:775` 按这两句断言；`CjFileStubBuilder.kt:86` 的反向识别只匹配 `CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR`，不会误判。改动 1 的空 view provider 与改动 2 的 decompiler 文本都会产出占位，应在 `CangJieCompiledFileErrors` 新增一个常量并让两处同源引用。
- **PCE 必须原样抛出。** `readSafely`（`CangJieMetadataStubBuilder.kt:119-127`）只捕 `IOException`，`ProcessCanceledException` 会穿到工厂；改动 1 的工厂 null-safe 分支不能吞它，读取层与工厂层都要先 `throw` 掉 `ControlFlowException` / `CancellationException`。

- **`dropPsiCaches` 不改修改计数。** `BuiltinsDecompiledDocumentRefresher:26` 只调 `PsiManager.dropPsiCaches()`。
- **`CfirModuleData.bindSession` 是一次性的**（`:81-86`）。detached 必须是 per-session 实例，随 `CjoManager` 重建。
- **`LLCfirModuleData.session` 有 fallback**（`:97-99`）。detached 必须显式绑定并加断言。
- **`hasMetadata` 每次读整份文件**（1.1–2.0 MB）。改动 6 后 `accepts` 不再读；`readFile` 仍读整份，靠 `VirtualFile` 内容缓存，不另造通道。
- **stub 构建异常是 per-file `forInvalid`**（`CjoFileStubBuilder.kt:27-36`，位于 `decompiler-to-stubs`）。CFIR 层任何异常对用户是一行注释。第 17 项按“不返回 null”断言。
- **CFIR 符号身份不含 session。** `CfirClassLikeSymbol` 以 `classId` 为 lookup tag（`CfirBasedSymbol.kt:110-130`），`CfirBasedSymbol` 未覆写 `equals`。同一 `.cjo` 在两条路径物化出两个实例是既有性质。
- **`CjoSearchPath` 的“键 → 根字符串”在四处生产实现**（`DecompiledCjoRepository:56`、`CfirSessionFactoryContextUtils.kt:53`、`MacroArtifactResolver.kt:290`、`LLBinaryOriginLibrarySymbolProviderFactory:92`）；测试 fixture `BuiltinsVirtualFileProviderTestImpl` 有自己的 `toBuiltinRoot`，不走 `CjoSearchPath`。改动 4 归一折叠，根协议后续可统一，本轮不动。
