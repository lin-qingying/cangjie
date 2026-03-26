package org.cangnova.cangjie.cfir.entrypoint.session

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.analysis.CheckersComponent
import org.cangnova.cangjie.cfir.analysis.checkers.LanguageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckers
import org.cangnova.cangjie.cfir.analysis.checkersComponent
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticsContainer
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.analysis.diagnostics.registerDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.analysis.diagnostics.registeredDiagnosticFactoriesStorageOrNull
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.extensions.CfirExtensionService
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.reflect.KClass

/**
 * 对齐 Kotlin 的 `org.jetbrains.kotlin.fir.session.FirSessionConfigurator`。
 *
 * 该配置器负责：
 * 1. 收集扩展注册批次；
 * 2. 向会话注入 checker 与自定义组件；
 * 3. 在 `configure()` 时一次性应用到会话。
 */
class CfirSessionConfigurator(
    /** 当前待配置的会话。 */
    private val session: CfirSession,
) {
    /** 已登记的扩展批次列表。 */
    private val registeredExtensions: MutableList<BunchOfRegisteredExtensions> =
        mutableListOf(BunchOfRegisteredExtensions.empty())

    /** 追加一批扩展注册。 */
    fun registerExtensions(extensions: BunchOfRegisteredExtensions) {
        registeredExtensions += extensions
    }

    /** 追加单个扩展注册器（便捷重载）。 */
    fun registerExtensions(registrar: CfirExtensionRegistrar) {
        registerExtensions(registrar.configure())
    }

    /** 注册表达式 checker。 */
    fun useCheckers(checkers: ExpressionCheckers) {
        session.checkersComponent.register(checkers)

    }

    /** 注册声明 checker。 */
    fun useCheckers(checkers: DeclarationCheckers) {
        session  .checkersComponent.register(checkers)

    }
    /**
     * Must only be used in CLI compiler mode.
     */
    @OptIn(SessionConfiguration::class)
    fun useCheckers(checkers: LanguageVersionSettingsCheckers) {
        session.checkersComponent.register(checkers)
    }
    /** 注册类型 checker。 */
    fun useCheckers(checkers: TypeCheckers) {
        session.checkersComponent.register(checkers)

    }

    /** 向会话注册任意自定义组件。 */
    fun registerComponent(
        componentKey: KClass<out CfirSessionComponent>,
        componentValue: CfirSessionComponent,
    ) {
        session.register(componentKey, componentValue)
    }

    /** 注册诊断容器，延迟到 `configure()` 一次写入。 */
    fun registerDiagnosticContainers(vararg diagnosticContainers: CjDiagnosticsContainer) {
        registerExtensions(
            BunchOfRegisteredExtensions(
                registrars = emptyList(),
                diagnosticsContainers = diagnosticContainers.toList(),
            ),
        )
    }

    /** 将已收集的配置真正应用到会话。 */
    fun configure() {
        val merged = registeredExtensions.reduce(BunchOfRegisteredExtensions::plus)

        // 扩展服务不存在时自动创建，保证可重复调用。
        val extensionService = session.ensureExtensionService()
        extensionService.registerAll(merged.registrars)

        // 仅源码会话记录诊断工厂容器，保持与 Kotlin 侧语义一致。
        if (session.kind == CfirSession.Kind.Source && merged.diagnosticsContainers.isNotEmpty()) {
            session.ensureDiagnosticFactoriesStorage()
                .registerDiagnosticContainers(merged.diagnosticsContainers)
        }
    }
}

/**
 * 扩展注册批次。
 *
 * 对齐 Kotlin 的 `BunchOfRegisteredExtensions`。
 */
class BunchOfRegisteredExtensions(
    /** 本批次扩展注册器。 */
    val registrars: List<CfirExtensionRegistrar>,
    /** 本批次诊断容器。 */
    val diagnosticsContainers: List<CjDiagnosticsContainer>,
) {
    /** 空批次构造。 */
    companion object {
        fun empty(): BunchOfRegisteredExtensions = BunchOfRegisteredExtensions(
            registrars = emptyList(),
            diagnosticsContainers = emptyList(),
        )
    }

    /** 批次合并。 */
    operator fun plus(other: BunchOfRegisteredExtensions): BunchOfRegisteredExtensions {
        return BunchOfRegisteredExtensions(
            registrars = registrars + other.registrars,
            diagnosticsContainers = diagnosticsContainers + other.diagnosticsContainers,
        )
    }
}

/** 将单个扩展注册器转换为一个批次。 */
fun CfirExtensionRegistrar.configure(): BunchOfRegisteredExtensions = BunchOfRegisteredExtensions(
    registrars = listOf(this),
    diagnosticsContainers = emptyList(),
)

/** 扩展服务可空访问器。 */
private val CfirSession.extensionServiceOrNull: CfirExtensionService?
    by CfirSession.nullableSessionComponentAccessor()

/** checker 组件可空访问器。 */
private val CfirSession.checkersComponentOrNull: CheckersComponent?
    by CfirSession.nullableSessionComponentAccessor()

/** 确保会话中存在扩展服务。 */
private fun CfirSession.ensureExtensionService(): CfirExtensionService {
    val existing = extensionServiceOrNull
    if (existing != null) return existing
    return CfirExtensionService().also { register(CfirExtensionService::class, it) }
}

/** 确保会话中存在 checker 组件。 */
private fun CfirSession.ensureCheckersComponent(): CheckersComponent {
    val existing = checkersComponentOrNull
    if (existing != null) return existing
    return CheckersComponent().also { register(CheckersComponent::class, it) }
}

/** 确保会话中存在诊断工厂存储。 */
private fun CfirSession.ensureDiagnosticFactoriesStorage(): CjRegisteredDiagnosticFactoriesStorage {
    val existing = registeredDiagnosticFactoriesStorageOrNull
    if (existing != null) return existing

    val created = CjRegisteredDiagnosticFactoriesStorage()
    registerDiagnosticFactoriesStorage(created)
    return created
}
