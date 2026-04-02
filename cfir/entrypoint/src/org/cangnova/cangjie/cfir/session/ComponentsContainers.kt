package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.transformers.CfirPhaseResolverRegistry
import org.cangnova.cangjie.cfir.resolve.transformers.MacroExpandAction
import org.cangnova.cangjie.cfir.resolve.transformers.registerResolveProcessors
import org.cangnova.cangjie.cfir.CfirEnumMatchTrackerComponent
import org.cangnova.cangjie.cfir.CfirImportTrackerComponent
import org.cangnova.cangjie.cfir.CfirLookupTrackerComponent
import org.cangnova.cangjie.cfir.CfirNameConflictsTracker
import org.cangnova.cangjie.cfir.IncrementalPassThroughEnumMatchTrackerComponent
import org.cangnova.cangjie.cfir.IncrementalPassThroughImportTrackerComponent
import org.cangnova.cangjie.cfir.IncrementalPassThroughLookupTrackerComponent
import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.SourcesToPathsMapper
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.analysis.diagnostics.registerDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.nameConflictsTracker
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.calls.visibility.CfirModuleVisibilityChecker
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendRuleQueryServiceImpl
import org.cangnova.cangjie.cfir.session.services.CfirExtendRuleQueryService
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.sourcesToPathsMapper
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.analysis.CheckersComponent
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirNameConflictsTrackerImpl
import org.cangnova.cangjie.cfir.analysis.nullableCheckersComponent
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolverImpl
import org.cangnova.cangjie.cfir.extensions.CfirExtensionService
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.CfirCliExceptionHandler
import org.cangnova.cangjie.cfir.CfirExceptionHandler
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.incremental.components.EnumMatchTracker
import org.cangnova.cangjie.incremental.components.ICFileMappingTracker
import org.cangnova.cangjie.incremental.components.ImportTracker
import org.cangnova.cangjie.incremental.components.LookupTracker

// ==================================== 通用组件 ====================================

/**
 * 注册通用 session 组件（所有会话类型共享）。
 *
 * 对齐 K2 的 registerCommonComponents，但移除了仓颉不需要的组件：
 * - 无 SyntheticFunctionInterfaceProvider（仓颉函数类型用 FuncTy 直接表示）
 * - 无 isMetadataCompilation（仓颉没有独立的 metadata 编译模式）
 * - 无 Java 互操作组件
 *
 * @param languageVersionSettings 语言版本配置，控制语言特性开关与版本兼容性
 */
@OptIn(SessionConfiguration::class)
fun CfirSession.registerCommonComponents(languageVersionSettings: LanguageVersionSettings) {
    // 注册语言版本设置组件，供编译器各阶段查询当前语言版本及特性开关
    register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(languageVersionSettings))
    // 注册内置类型组件，提供 Int、Bool、String 等基础类型的符号定义
    register(CfirBuiltinTypes::class, CfirBuiltinTypes())
    // 注册扩展服务组件，管理编译器插件注册的扩展点（checker、生成器等）
    register(CfirExtensionService::class, CfirExtensionService())
}

/**
 * 注册 CLI 编译器公共组件（在扩展配置之前调用）。
 *
 * 对齐 K2 的 registerCliCompilerAndCommonComponents。
 * 目前直接委托给 [registerCommonComponents]，CLI 特有组件可在此追加。
 *
 * 调用时机：扩展点（插件）配置完成之前。
 *
 * @param languageVersionSettings 语言版本配置
 */
@OptIn(SessionConfiguration::class)
fun CfirSession.registerCliCompilerAndCommonComponents(languageVersionSettings: LanguageVersionSettings) {
    registerCommonComponents(languageVersionSettings)


    register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
    register(CfirExceptionHandler::class, CfirCliExceptionHandler)
}

/**
 * 注册扩展配置完成后才能初始化的组件。
 *
 * 对齐 K2 的 registerCommonComponentsAfterExtensionsAreConfigured。
 * 调用时机：所有编译器插件/扩展点完成注册之后。
 *
 * 当前为空实现，后续可在此注册依赖扩展点信息的服务
 * （例如需要知道已注册了哪些 checker 才能初始化的组件）。
 */
@OptIn(SessionConfiguration::class)
fun CfirSession.registerCommonComponentsAfterExtensionsAreConfigured() {
    // 预留：依赖扩展注册结果的组件在此注册
}

// ==================================== 解析组件 ====================================

/**
 * 注册完整的符号解析（resolve）所需组件。
 *
 * 这是编译器 resolve 流水线的核心初始化入口，负责：
 *  1. 创建并注册阶段解析器注册表（[CfirPhaseResolverRegistry]）
 *  2. 初始化诊断上报器
 *  3. 注册核心 resolve 服务（类型解析、import 绑定、父类图等）
 *  4. 按需注册增量编译追踪器（lookup / enumMatch / import）
 *  5. 注册诊断工厂存储
 *  6. 将所有 resolve 阶段的 Processor 注册到 registry
 *
 * @param diagnosticFactoriesStorage 诊断工厂存储，持有所有已注册的诊断类型定义
 * @param lookupTracker 符号查找追踪器，用于增量编译时记录符号依赖关系；为 null 时禁用
 * @param enumMatchTracker 枚举匹配追踪器，用于增量编译时追踪 when 表达式对枚举的依赖；为 null 时禁用
 * @param importTracker import 追踪器，用于增量编译时记录文件的 import 依赖；为 null 时禁用
 * @param fileMappingTracker 源文件路径映射追踪器，配合 lookupTracker 使用；为 null 时禁用
 */
@OptIn(SessionConfiguration::class)
fun CfirSession.registerResolveComponents(
    diagnosticFactoriesStorage: CjRegisteredDiagnosticFactoriesStorage,
    macroExpandAction: MacroExpandAction? = null,
    lookupTracker: LookupTracker? = null,
    enumMatchTracker: EnumMatchTracker? = null,
    importTracker: ImportTracker? = null,
    fileMappingTracker: ICFileMappingTracker? = null,
) {
    // ── Step 1：创建阶段解析器注册表 ──────────────────────────────────────
    // CfirPhaseResolverRegistry 维护"解析阶段 → Processor"的映射，
    // 驱动 resolve 流水线按阶段顺序执行。
    val registry = CfirPhaseResolverRegistry()
    register(CfirPhaseResolverRegistry::class, registry)

    // ── Step 2：初始化诊断上报器 ──────────────────────────────────────────
    // 若 session 中已有诊断上报器（如测试环境注入的 mock），则复用；
    // 否则创建默认的 CfirDiagnosticCollector 并注册。
    val diagnosticReporter = diagnosticReporterOrNull ?: CfirDiagnosticCollector().also(::registerDiagnosticReporter)

    // ── Step 3：注册核心 resolve 服务 ─────────────────────────────────────
    registerCoreResolveServices(diagnosticReporter)
    register(TypeComponents::class, TypeComponents(this))
    register(InferenceComponents::class, InferenceComponents(this))

    // 注册源文件路径映射服务，用于将 CjSourceElement 转换为文件系统路径
    register(SourcesToPathsMapper::class, SourcesToPathsMapper())

    // ── Step 4：按需注册增量编译追踪器 ────────────────────────────────────

    if (lookupTracker != null) {
        // 将源码元素转换为文件路径的 lambda，供追踪器上报时使用
        val sourceToFilePath: (CjSourceElement) -> String? = { source ->
            sourcesToPathsMapper.getSourceFilePath(source)
        }
        // IncrementalPassThroughLookupTrackerComponent 是对外部 LookupTracker 的适配器，
        // 将编译器内部的符号查找事件转发给增量编译框架
        register(
            CfirLookupTrackerComponent::class,
            IncrementalPassThroughLookupTrackerComponent(
                lookupTracker = lookupTracker,
                fileMappingTracker = fileMappingTracker,   // 文件路径映射，用于跨模块依赖追踪
                sourceToFilePath = sourceToFilePath,
            ),
        )
    }

    if (enumMatchTracker != null) {
        // 追踪 when 表达式对枚举类的完整性检查依赖，
        // 当枚举新增/删除成员时可精确触发受影响文件的重新编译
        register(
            CfirEnumMatchTrackerComponent::class,
            IncrementalPassThroughEnumMatchTrackerComponent(enumMatchTracker),
        )
    }

    if (importTracker != null) {
        // 追踪每个文件的 import 依赖，当被 import 的符号变化时
        // 可精确找到所有需要重新编译的文件
        register(
            CfirImportTrackerComponent::class,
            IncrementalPassThroughImportTrackerComponent(importTracker),
        )
    }

    // ── Step 5：注册诊断工厂存储 ──────────────────────────────────────────
    // 诊断工厂存储持有所有已注册的诊断类型（错误码、消息模板等），
    // checker 和 resolve 阶段通过它创建具体的诊断实例
    registerDiagnosticFactoriesStorage(diagnosticFactoriesStorage)

    // ── Step 6：将各 resolve 阶段的 Processor 注册到 registry ─────────────
    // registerResolveProcessors 会依次注册 IMPORTS、SUPER_TYPES、TYPES 等
    // 各阶段对应的 Processor，完成 resolve 流水线的装配
    registerResolveProcessors(registry, diagnosticReporter, this, ScopeSession(), macroExpandAction)
}

/**
 * 注册仅在 CLI 编译器模式下需要的 resolve 组件。
 *
 * 与 [registerResolveComponents] 配合使用，补充注册 CLI 特有的组件：
 * - [CfirNameConflictsTracker]：检测同作用域内的命名冲突（如重复声明）
 * - [CheckersComponent]：语义检查器容器，承载所有声明级别的 checker
 *
 * 使用幂等检查（判断是否已注册）确保多次调用不会重复注册，
 * 适配测试框架中可能提前注入自定义实现的场景。
 */
@OptIn(SessionConfiguration::class)
fun CfirSession.registerCliCompilerOnlyResolveComponents() {
    // 仅在尚未注册时才创建默认实现，允许外部（如测试）预先注入自定义实现
    if (nameConflictsTracker == null) {
        register(CfirNameConflictsTracker::class, CfirNameConflictsTrackerImpl())
    }
    if (nullableCheckersComponent == null) {
        register(CheckersComponent::class, CheckersComponent())
    }
}

/**
 * 注册当前编译模块的元数据。
 *
 * [CfirModuleData] 包含模块名称、依赖模块列表、平台信息等，
 * 是 resolve 阶段跨模块符号查找的基础。
 * 通常在 session 初始化早期、resolve 组件注册之前调用。
 *
 * @param moduleData 当前模块的元数据描述对象
 */
@OptIn(SessionConfiguration::class)
fun CfirSession.registerModuleData(moduleData: CfirModuleData) {
    register(CfirModuleData::class, moduleData)
}

/**
 * 注册核心 resolve 服务（内部公共方法，被多个注册函数复用）。
 *
 * 包含 resolve 流水线正常运转所必需的最小服务集合：
 *
 * | 服务 | 职责 |
 * |------|------|
 * | [CfirLazyDeclarationResolver] | 按需（懒加载）触发声明的各阶段解析，避免循环依赖 |
 * | [CfirImportBindingStore] | 缓存 import 解析结果，供后续符号查找复用 |
 * | [CfirSuperTypeGraphStore] | 维护父类型图，支持继承关系查询和循环继承检测 |
 * | [CfirTypeResolver] | 将类型引用（FqName/FirTypeRef）解析为具体的 ConeType |
 *
 * @param diagnosticReporter 诊断上报器，部分服务在初始化时需要用于上报错误
 */
private fun CfirSession.registerCoreResolveServices(
    diagnosticReporter: CfirDiagnosticReporter,
) {
    // 懒加载声明解析器：协调各 Processor 按正确顺序解析声明，
    // 通过懒加载机制打破符号解析中的循环依赖
    register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)

    // import 绑定存储：持久化每个文件的 import 解析结果
    // （由 CfirImportResolveTransformer 写入，由类型解析阶段读取）
    register(CfirImportBindingStore::class, CfirImportBindingStore())

    // 父类型图存储：记录类型继承关系，用于：
    //  1. 检测循环继承（A extends B extends A）
    //  2. 父类成员的继承与覆盖解析
    val superTypeGraphStore = CfirSuperTypeGraphStore()
    register(CfirSuperTypeGraphStore::class, superTypeGraphStore)
    register(CfirDirectSupertypeProvider::class, superTypeGraphStore)
    register(CfirModuleVisibilityChecker::class, CfirModuleVisibilityChecker.Standard(this))

    val extendIndexStore = CfirExtendIndexStore()
    register(CfirExtendIndexStore::class, extendIndexStore)
    register(CfirExtendRuleQueryService::class, CfirExtendRuleQueryServiceImpl(extendIndexStore))

    // 类型解析器：将 CFIR 树中的类型引用解析为 ConeType（CFIR 的内部类型表示）
    register(CfirTypeResolver::class, CfirTypeResolverImpl(this))

}
