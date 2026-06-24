package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.entrypoint.session.CfirSessionConfigurator
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.allowAnyScriptsInSourceRoots
import org.cangnova.cangjie.config.dontSortSourceFiles
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform

/**
 * 已创建的源码 session 及其负责分析的源文件集合。
 *
 * @property session 源文件集合对应的 CFIR 源码会话。
 * @property files 分配给该 session 的源文件列表。
 */
data class SessionWithSources<F>(
    val session: CfirSession,
    val files: List<F>,
)

/**
 * 平台无关的 session 构造工具。
 *
 * 该工具对齐 Kotlin `SessionConstructionUtils` 的单模块流程：
 * 1. 创建共享库 session；
 * 2. 创建绑定依赖模块数据的普通库 session；
 * 3. 根据普通源码/脚本源码拆分创建源码 session。
 */
object CfirSessionConstructionUtils {
    /**
     * 准备当前编译批次需要的源码 session。
     *
     * 当配置允许任意脚本位于源码根时，普通源码和脚本源码会被拆分到两个 session；脚本 session
     * 通过 depends-on 依赖普通源码 session，保持脚本可见普通源码声明。
     *
     * @param files 待编译的源文件列表。
     * @param configuration 编译配置，提供文件排序、脚本策略和目标平台。
     * @param rootModuleName 根源码模块名称。
     * @param dependencyList CLI 依赖模块列表。
     * @param createSharedLibrarySession 共享库 session 构造回调。
     * @param createLibrarySession 普通库 session 构造回调，接收共享库 session。
     * @param createSourceSession 源码 session 构造器。
     * @param isScript 判断源文件是否为脚本文件的谓词。
     * @return 按普通源码、脚本源码顺序返回的 session 与源文件配对列表。
     */
    fun <F> prepareSessions(
        files: List<F>,
        configuration: CompilerConfiguration,
        rootModuleName: Name,
        dependencyList: DependencyListForCliModule,
        createSharedLibrarySession: () -> CfirSession,
        createLibrarySession: (sharedLibrarySession: CfirSession) -> CfirSession,
        createSourceSession: CfirSessionProducer<F>,
        isScript: (F) -> Boolean = { false },
    ): List<SessionWithSources<F>> {
        val orderedFiles = if (configuration.dontSortSourceFiles) files else files.sortedBy { it.toString() }
        val (scriptFiles, nonScriptFiles) = if (configuration.allowAnyScriptsInSourceRoots) {
            orderedFiles.partition(isScript)
        } else {
            emptyList<F>() to orderedFiles
        }
        val targetPlatform = configuration.targetPlatform ?: CangJiePlatforms.defaultCangJiePlatform
        val sessionConfigurator: CfirSessionConfigurator.() -> Unit = {}

        val sharedSession = createSharedLibrarySession()
        createLibrarySession(sharedSession)

        val nonScriptSession = createSingleSession(
            files = nonScriptFiles,
            rootModuleName = rootModuleName,
            dependencyList = dependencyList,
            targetPlatform = targetPlatform,
            sessionConfigurator = sessionConfigurator,
            sourceSessionProducer = createSourceSession,
        )

        if (scriptFiles.isEmpty()) return listOf(nonScriptSession)

        val scriptsSession = createSingleSession(
            files = scriptFiles,
            rootModuleName = Name.identifier("${rootModuleName.asString()}-scripts"),
            dependencyList = DependencyListForCliModule(
                regularDependencies = dependencyList.regularDependencies,
                dependsOnDependencies = dependencyList.dependsOnDependencies + nonScriptSession.session.moduleData,
                moduleDataProvider = dependencyList.moduleDataProvider,
            ),
            targetPlatform = targetPlatform,
            sessionConfigurator = sessionConfigurator,
            sourceSessionProducer = createSourceSession,
        )

        return listOf(
            nonScriptSession,
            scriptsSession,
        )
    }

    /**
     * 创建单个源码 session。
     *
     * 该方法负责构造 [CfirSourceModuleData]，把 regular/depends-on 依赖写入模块数据，并调用
     * [sourceSessionProducer] 完成实际 session 初始化。
     */
    private fun <F> createSingleSession(
        files: List<F>,
        rootModuleName: Name,
        dependencyList: DependencyListForCliModule,
        targetPlatform: TargetPlatform,
        sessionConfigurator: CfirSessionConfigurator.() -> Unit,
        sourceSessionProducer: CfirSessionProducer<F>,
    ): SessionWithSources<F> {
        val sourceModuleData = CfirSourceModuleData(
            name = rootModuleName,
            dependencies = dependencyList.regularDependencies,
            refinementDependencies = dependencyList.dependsOnDependencies,
            targetPlatform = targetPlatform,
            platform = CfirPlatform.DEFAULT,
        )

        val sourceSession = sourceSessionProducer.createSession(
            files = files,
            moduleData = sourceModuleData,
            isForLeafHmppModule = false,
            sessionConfigurator = sessionConfigurator,
        )

        return SessionWithSources(sourceSession, files)
    }
}

/**
 * 源码 session 构造回调接口。
 *
 * 该接口把平台无关的模块数据准备逻辑与具体 session factory 解耦，使 PSI、LightTree 或测试
 * facade 可以复用同一套 session construction 规则。
 */
fun interface CfirSessionProducer<F> {
    /**
     * 创建源码 session。
     *
     * @param files 分配给当前 session 的源文件。
     * @param moduleData 当前源码模块数据。
     * @param isForLeafHmppModule HMPP 对齐保留参数；本仓库内始终为 `false`。
     * @param sessionConfigurator 追加到源码 session 初始化过程中的配置回调。
     */
    fun createSession(
        files: List<F>,
        moduleData: CfirModuleData,
        isForLeafHmppModule: Boolean,
        sessionConfigurator: CfirSessionConfigurator.() -> Unit,
    ): CfirSession
}
