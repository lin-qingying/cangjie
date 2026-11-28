# 宏语义展开：新宿主接入指南

> 目标读者：需要把「宏展开后的语义视图」接入 Analysis API 的宿主（IDE 插件、LSP、批处理工具、测试容器）。
> 相关设计：`docs/plans/macro-expansion-in-analysis.md`。

## 1. 分工

宏语义展开被拆成三层，宿主只负责最外层：

| 层 | 位置 | 职责 |
|---|---|---|
| 引擎 SPI | `macro:macro-common`（`org.cangnova.cangjie.macro.expansion`） | `MacroSemanticExpansionEngine` 与宿主无关的取值类型（`SdkHandle` / `ModuleRoots` / `MacroExpansionRevision` / `Barrier` / `Outcome`） |
| 编排框架 | `analysis:macro-expansion` | 触发、in-flight 去重、模块级串行、revision/失败记录、pre-macro 构建、展开、快照发布与重试 |
| 宿主环境 | 各宿主 | 提供 SDK / roots / revision / 读访问 / 写屏障 / 编译器配置与三个回调 |

硬性边界：

- `compiler:frontend` 只实现引擎 SPI，**不依赖** `analysis:*`；
- `analysis:macro-expansion` **不依赖** `compiler:frontend`，也不依赖任何宿主的工具链类型；
- 宿主**不得**决定「何时构造、何时发布、何时失效」，这些属于框架。

## 2. 最小接入：实现一个 environment

```kotlin
class MyMacroExpansionEnvironment(private val project: Project) : MacroSemanticExpansionEnvironment {
    override fun sdk(module: CaSourceModule): SdkHandle? =
        myToolchain.sdkFor(module)?.let { sdk ->
            SdkHandle(
                id = sdk.id,
                homePath = sdk.homePath,
                environment = sdk.processEnvironment,   // 供 cjc / LSPMacroServer 使用
                stdlibPath = sdk.stdlibPath,            // 参与 classpath 组装
            )
        }

    override fun roots(module: CaSourceModule): ModuleRoots =
        ModuleRoots(
            sourceRoots = module.psiRoots.mapNotNull { it.virtualFile?.path },
            classpathRoots = module.allDirectDependencies
                .filterIsInstance<CaLibraryModule>()
                .flatMap { it.binaryRoots.mapNotNull { root -> root.virtualFile?.path } },
        )

    override fun revision(module: CaSourceModule): MacroExpansionRevision =
        MacroExpansionRevision(
            moduleKey = module.moduleDescription,
            psiModificationCount = myModificationTracker.psiCount(),
            projectRootModificationCount = myModificationTracker.rootsCount(),
            compilerSettingsModificationCount = myModificationTracker.settingsCount(),
            sdkKey = myToolchain.sdkFor(module)?.id,
            // 宏 artifact 内容指纹；无法计算时保留默认占位值。
        )

    override fun readAccess(): MacroConstructionReadAccess = MacroConstructionReadAccess.Direct

    override fun barrier(): MacroExpansionBarrier = MyBarrier

    override fun compilerConfiguration(
        module: CaSourceModule,
        roots: ModuleRoots,
        sdk: SdkHandle?,
    ): CompilerConfiguration = buildCompilerConfiguration(module, roots, sdk)

    override fun installFragmentParser(module: CaSourceModule, configuration: CompilerConfiguration) {
        installMyFragmentParser(module, configuration)
    }

    override fun engine(): MacroSemanticExpansionEngine = myEngine   // 见第 3 节

    override fun onConstructionStarted(session: LLCfirResolvableModuleSession) = Unit

    override fun onPublished(session: LLCfirResolvableModuleSession, files: Set<VirtualFile>) {
        // files 为空表示复用了其它会话已发布的快照：仍然必须结束本轮刷新屏障。
        myDaemonRefresher.refresh(session, files)
        myDaemonRefresher.finishConstruction(session)
    }

    override fun onAbandoned(session: LLCfirResolvableModuleSession, outcome: MacroExpansionOutcome) {
        myDaemonRefresher.finishConstruction(session)
    }

    private object MyBarrier : MacroExpansionBarrier {
        override fun <T> publish(block: () -> T): T? {
            // 唯一允许触碰写访问的地方；返回 null 表示发布被放弃（框架按输入已变处理）。
            var result: T? = null
            ApplicationManager.getApplication().invokeAndWait(
                { result = WriteAction.compute<T?, RuntimeException> { block() } },
                ModalityState.defaultModalityState(),
            )
            return result
        }
    }
}
```

实现要点：

- `revision()` 必须**可比较**（`equals`）；框架只做相等比较，不等即失效旧快照并丢弃旧结果。
  至少覆盖 PSI、roots、编译器设置、SDK、宏 artifact 指纹。
- `barrier()` 是唯一写访问入口。IDE 使用 `invokeAndWait + WriteAction`；
  headless 的 `MockApplication` 容器经实测也可用同一形态（见
  `lsp/test/.../MacroExpansionHostContainerProbeTest`）。
- `compilerConfiguration()` 负责把 `SdkHandle` 投影为宏 SDK 环境、执行器与标准库 classpath，
  并把 `MacroConstructionReadAccess` 写进配置；`installFragmentParser()` 负责宿主相关的片段解析。
- 回调契约：一次构造恰好一次 `onConstructionStarted`，随后**要么**一次 `onPublished`，
  **要么**一次 `onAbandoned`。取消 / 失败也走 `onAbandoned`，宿主不能无限等待。
- `ProcessCanceledException` 由框架原样重抛，宿主回调只负责收尾。

## 3. 引擎从哪里来

引擎实现由 `compiler:frontend` 提供：

```kotlin
override fun engine(): MacroSemanticExpansionEngine = FrontendMacroSemanticExpansionEngine()
```

没有可用执行器时返回 `MacroSemanticExpansionEngine.Unavailable`。框架会把它当作
「本次构造无法发布」，保持实时视图并记录失败 revision，不会无限重试。

## 4. 注册

### 4.1 扩展点

`analysis:macro-expansion` 在自己的插件描述符里声明
`org.cangnova.cangjie.macroExpansionEnvironment`（`area="IDEA_PROJECT"`、`dynamic="true"`），
并注册 `MacroSemanticSessionConfigurator` 到 `llCfirSessionConfigurator`。

宿主只需注册 environment：

```xml
<extensions defaultExtensionNs="org.cangnova.cangjie">
    <macroExpansionEnvironment implementation="com.example.MyMacroExpansionEnvironment"/>
</extensions>
```

没有注册 environment 的容器保持原行为：不触发构造、不发布快照。

### 4.2 headless 容器

- 需要把 `cangjie-analysis-api-cfir.xml` 的 include 链（含 `cangjie-macro-expansion.xml`）加载到容器；
  `analysis:analysis-api-standalone` 的扩展点白名单已包含
  `org.cangnova.cangjie.llCfirSessionConfigurator` / `org.cangnova.cangjie.macroExpansionEnvironment`。
- environment 可以编程注册（扩展点是 dynamic 的）：

```kotlin
MacroSemanticExpansionEnvironment.registerExtension(project, MyMacroExpansionEnvironment(project))
```

### 4.3 测试 / 批处理

`analysis:macro-expansion` 的 testFixtures 提供：

- `FakeMacroSemanticExpansionEnvironment`：所有取值可编程、回调只计数；
- `FakeMacroSemanticExpansionEngine`：调用计数与结果注入。

```kotlin
val environment = FakeMacroSemanticExpansionEnvironment().apply {
    revisionProvider = { module -> currentRevisionFor(module) }
    engineProvider = { fakeEngine }
    barrier = MacroExpansionBarrier { it() }   // 直接执行
}
environment.register(project)

val service = MacroSemanticExpansionService.getInstance(project)
service.request(session)
service.awaitIdle(module)      // 测试等待构造结束
```

## 5. 宿主要实现什么、不要实现什么

必须实现：

- 取值：SDK、roots、revision、编译器配置、片段 parser；
- 屏障：写访问边界；
- 回调：开始 / 发布 / 放弃（含 daemon 刷新与收尾）。

不要实现：

- 复刻「无宏」判断（唯一入口是 `MacroSemanticSnapshotContract`）；
- 自行调度 / 重试 / 去重 / 记录 revision；
- 自行发布或失效快照（只有框架在 `barrier()` 内调用
  `publishMacroSemanticSnapshot`）。

## 6. 当前接入状态

| 宿主 | 状态 |
|---|---|
| IntelliJ IDE | 已接入：`IntelliJMacroSemanticExpansionEnvironment` |
| LSP | 结构接线 + 前端装配完成：`LspMacroSemanticExpansionEnvironment`、`LspMacroExpansionOptions`、`frontendMacroExpansionOptions(sdk)`（引擎=FrontendMacroSemanticExpansionEngine，SDK 全部来自宿主传入的 `SdkHandle`）。产品壳只需提供 `SdkHandle` 并传入 `CangjieLspServerOptions.macroExpansion`。测试覆盖：未注册/已注册、引擎与屏障委托、真实工作区 roots/revision、前端配置装配（未启动真实执行器） |
| Analysis 测试容器 | 扩展点白名单已放行，testFixtures 已提供 fake environment / engine；尚未有 Analysis API 用例实际接入 |

## LSPMacroServer 启动契约（实测）

- 可执行文件：`<sdk>/tools/bin/LSPMacroServer(.exe)`；
- argv：
  1. 子进程读句柄（Windows HANDLE / Unix fd，十进制）；
  2. 子进程写句柄；
  3. `parallel`：`0/1`；
  4. **cjc 所在目录 `<sdk>/bin`**（不是 `tools/bin`；服务端用 `<dir>/../runtime/lib/<platform>` 定位运行时库）；
  5. Unix 额外传父进程 PID；
- 工作目录：服务端 exe 所在目录 `<sdk>/tools/bin`；
- 环境：父进程环境 + `CANGJIE_HOME=<sdk>`；Unix 还需
  `LD_LIBRARY_PATH=<sdk>/runtime/lib/<platform>:<sdk>/lib/<platform>`（macOS 用 `DYLD_LIBRARY_PATH`）；
- 协议：8 字节小端长度前缀 + FlatBuffers；
- Windows 现状：按上述参数启动，真实 IDE/测试仍以 `0xC0000005` 退出
  （WER：`msvcrt.dll+0x62f1e`）；独立探针在 argv[4]=`<sdk>/bin` 时可存活，
  与真实启动的剩余差异尚未收敛。

## 外部宏包的 demand

annotation 宏（`@Foo`）只有落在以下集合之一时才会触发 artifact preparation：

1. 同项目源码中的 `macro package`；
2. 官方 stdlib 宏包（`std.deriving` 等）；
3. 宿主通过 `MacroSemanticExpansionEnvironment.externalMacroPackages(module)` 显式返回的宏包。

如果宿主消费的是依赖里的预编译宏包（cjpm 依赖、SDK 外部宏包），必须实现该方法返回其
`FqName` 集合；LSP 侧通过 `LspMacroExpansionOptions.externalMacroPackagesProvider` 注入。
否则 raw 构建会把未解析的 `@Foo` 回落成 `<当前包>.Foo`，`externalPackageDemand=null`，
表现为「快照已发布但没有宏生成声明」。

## references 与宏生成声明

`textDocument/references` 的目标键优先从当前位置的 `CjReferenceExpression` 直接解析
`CaSymbol.callableId`（见 `AnalysisApiLspSemanticSupport.referenceTargetKey`），
因此宏生成函数的使用点可以正常匹配。

行为：

- `definition` 会回到宏调用来源（如 `@Make`），因为生成声明没有物理 PSI；
- `hover` 请求可返回，但生成函数没有 cdoc，内容可能为空；
- `references` 返回生成函数的使用点（同文件与跨文件），协议测试见
  `LspMacroExpansionProtocolIntegrationTest`。
