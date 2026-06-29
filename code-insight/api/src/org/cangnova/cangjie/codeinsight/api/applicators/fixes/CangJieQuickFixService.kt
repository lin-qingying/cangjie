package org.cangnova.cangjie.codeinsight.api.applicators.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi

/**
 * CangJie K2 quick-fix 的统一入口服务。
 *
 * 该层只负责汇总 registrar 暴露出来的 quick-fix 列表，并在 analysis session 中
 * 把诊断分发给对应 factory，不承载任何具体业务修复逻辑。
 */
@Suppress("LightServiceMigrationCode")
class CangJieQuickFixService {
    companion object {
        /**
         * 获取 IDE application 级 quick-fix 服务实例。
         */
        @JvmStatic
        fun getInstance(): CangJieQuickFixService = service()
    }

    /** 普通 quick-fix 的聚合列表。 */
    private val list: CangJieQuickFixesList =
        CangJieQuickFixesList.createCombined(CangJieQuickFixRegistrar.allQuickFixesList())

    /** 延迟 quick-fix 的聚合列表，用于 HighlightInfo lazy action 链路。 */
    private val lazyList: CangJieQuickFixesList =
        CangJieQuickFixesList.createCombined(CangJieQuickFixRegistrar.allLazyQuickFixesList())

    /** import-on-the-fly quick-fix 的聚合列表。 */
    private val importOnTheFlyList: CangJieQuickFixesList =
        CangJieQuickFixesList.createCombined(CangJieQuickFixRegistrar.allImportOnTheFlyQuickFixList())

    /**
     * 在当前 analysis session 中为诊断创建普通 quick fix。
     */
    fun CaSession.getQuickFixesFor(diagnostic: CaDiagnosticWithPsi<*>): List<IntentionAction> =
        with(list) { getQuickFixesFor(diagnostic) }

    /**
     * 在当前 analysis session 中为诊断创建普通 quick fix，并保留单个 factory 的失败信息。
     */
    fun CaSession.getQuickFixesWithCatchingFor(diagnostic: CaDiagnosticWithPsi<*>): Sequence<Result<IntentionAction>> =
        with(list) { getQuickFixesWithCatchingFor(diagnostic) }

    /**
     * 判断诊断是否存在可延迟创建的 quick fix。
     */
    fun CaSession.canProduceLazyQuickFixesFor(diagnostic: CaDiagnosticWithPsi<*>): Boolean =
        with(lazyList) { canProduceQuickFixesFor(diagnostic) }

    /**
     * 在当前 analysis session 中为诊断创建延迟 quick fix。
     */
    fun CaSession.getLazyQuickFixesFor(diagnostic: CaDiagnosticWithPsi<*>): List<IntentionAction> =
        with(lazyList) { getQuickFixesFor(diagnostic) }

    /**
     * 预留与 Kotlin K2 一致的 import-on-the-fly 聚合口。
     *
     * 当前 CangJie IDE 侧尚未消费这条链，但 registrar 层的形状要先对齐，
     * 避免后续再回退到 K1 风格扩展点。
     */
    internal fun CaSession.getImportOnTheFlyQuickFixesFor(diagnostic: CaDiagnosticWithPsi<*>): List<IntentionAction> =
        with(importOnTheFlyList) { getQuickFixesFor(diagnostic) }
}

/**
 * CangJie K2 quick-fix 注册器。
 *
 * 具体模块只暴露 quick-fix 列表，不直接参与 visitor 或高亮链路。
 */
abstract class CangJieQuickFixRegistrar {
    /**
     * registrar 暴露的普通 quick-fix 列表。
     */
    protected abstract val list: CangJieQuickFixesList

    /**
     * 与 Kotlin K2 相同，lazy quick-fix 通过独立列表装配到 HighlightInfo。
     */
    protected open val lazyList: CangJieQuickFixesList = CangJieQuickFixesList.createCombined()

    /**
     * 与 Kotlin K2 相同，允许声明“可用于 import-on-the-fly”的修复集合。
     */
    protected open val importOnTheFlyList: CangJieQuickFixesList = CangJieQuickFixesList.createCombined()

    companion object {
        /** quick-fix registrar 的 IntelliJ 扩展点名称。 */
        private val EP_NAME: ExtensionPointName<CangJieQuickFixRegistrar> =
            ExtensionPointName.create("org.cangnova.cangjie.codeinsight.quickfix.registrar")

        /**
         * 收集所有已注册 registrar 的普通 quick-fix 列表。
         */
        fun allQuickFixesList(): List<CangJieQuickFixesList> =
            EP_NAME.extensionList.map { it.list }

        /**
         * 收集所有已注册 registrar 的延迟 quick-fix 列表。
         */
        fun allLazyQuickFixesList(): List<CangJieQuickFixesList> =
            EP_NAME.extensionList.map { it.lazyList }

        /**
         * 收集所有已注册 registrar 的 import-on-the-fly quick-fix 列表。
         */
        fun allImportOnTheFlyQuickFixList(): List<CangJieQuickFixesList> =
            EP_NAME.extensionList.map { it.importOnTheFlyList }
    }
}
