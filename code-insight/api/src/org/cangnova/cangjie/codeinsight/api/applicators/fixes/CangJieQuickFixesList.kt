package org.cangnova.cangjie.codeinsight.api.applicators.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import kotlin.reflect.KClass

/**
 * 诊断到 quick-fix factory 的只读映射。
 */
class CangJieQuickFixesList @ForCangJieQuickFixesListBuilder constructor(
    /** 以 analysis diagnostic 运行时类型为键的 quick-fix factory 列表。 */
    private val quickFixes: Map<KClass<out CaDiagnosticWithPsi<*>>, List<CangJieQuickFixFactory<*>>>,
) {
    /**
     * 判断当前列表是否能为给定诊断产生 quick fix。
     */
    fun CaSession.canProduceQuickFixesFor(diagnostic: CaDiagnosticWithPsi<*>): Boolean =
        quickFixes[diagnostic.diagnosticClass]?.isNotEmpty() == true

    /**
     * 计算给定诊断的所有 quick fix。
     *
     * 该方法会把 factory 失败重新抛出，适用于调用方希望保留异常语义的路径。
     */
    fun CaSession.getQuickFixesFor(diagnostic: CaDiagnosticWithPsi<*>): List<IntentionAction> =
        getQuickFixesWithCatchingFor(diagnostic).mapTo(mutableListOf()) { it.getOrThrow() }

    /**
     * 计算给定诊断的所有 quick fix，并把单个 factory 的失败封装到 [Result] 中。
     *
     * [ProcessCanceledException] 仍按 IntelliJ 平台约定直接向外传播，其他异常会包装为
     * [ComputingQuickFixesError]，避免一个 factory 破坏整条 quick-fix 枚举链。
     */
    fun CaSession.getQuickFixesWithCatchingFor(diagnostic: CaDiagnosticWithPsi<*>): Sequence<Result<IntentionAction>> {
        val factories = quickFixes[diagnostic.diagnosticClass] ?: return emptySequence()

        return factories.asSequence()
            .map { @Suppress("UNCHECKED_CAST") (it as CangJieQuickFixFactory<CaDiagnosticWithPsi<*>>) }
            .map { factory ->
                with(factory) {
                    runCatching { createQuickFixes(diagnostic).map { it.asIntention() } }
                        .recoverCatching { throwable ->
                            when (throwable) {
                                is ProcessCanceledException -> throw throwable
                                else -> throw ComputingQuickFixesError("Error while creating quick-fixes by $factory", throwable)
                            }
                        }
                }
            }.flatMap { result ->
                result.fold(
                    onSuccess = { actions -> actions.map { Result.success(it) }.asSequence() },
                    onFailure = { sequenceOf(Result.failure(it)) },
                )
            }
    }

    /**
     * 表示 quick-fix factory 创建修复项时发生的非取消异常。
     */
    class ComputingQuickFixesError(message: String, cause: Throwable) : IllegalStateException(message, cause)

    companion object {
        /**
         * 合并多个 registrar 产出的 quick-fix 列表。
         */
        @OptIn(ForCangJieQuickFixesListBuilder::class)
        fun createCombined(registrars: List<CangJieQuickFixesList>): CangJieQuickFixesList {
            val allQuickFixes = registrars.map { it.quickFixes }.merge()
            return CangJieQuickFixesList(allQuickFixes)
        }

        /**
         * 以 vararg 形式合并多个 quick-fix 列表。
         */
        fun createCombined(vararg registrars: CangJieQuickFixesList): CangJieQuickFixesList =
            createCombined(registrars.toList())
    }
}

/**
 * CangJie K2 quick-fix 列表构建器。
 */
class CangJieQuickFixesListBuilder private constructor() {
    /** 构建中的诊断类型到 factory 列表映射，保持注册顺序。 */
    private val quickFixes = LinkedHashMap<
        KClass<out CaDiagnosticWithPsi<*>>,
        MutableList<CangJieQuickFixFactory<out CaDiagnosticWithPsi<*>>>,
        >()

    /**
     * 使用 reified 诊断类型注册 quick-fix factory。
     */
    inline fun <reified DIAGNOSTIC : CaDiagnosticWithPsi<*>> registerFactory(
        factory: CangJieQuickFixFactory<DIAGNOSTIC>,
    ) {
        registerFactory(DIAGNOSTIC::class, factory)
    }

    /**
     * 将 quick-fix factory 注册到指定诊断类型。
     *
     * 诊断类型必须是具体 diagnostic 子类，不能使用泛型基类，否则 analysis API
     * 上报的运行时类型不会命中该 factory。
     */
    fun <DIAGNOSTIC : CaDiagnosticWithPsi<*>> registerFactory(
        diagnosticClass: KClass<DIAGNOSTIC>,
        factory: CangJieQuickFixFactory<DIAGNOSTIC>,
    ) {
        if (diagnosticClass == CaDiagnosticWithPsi::class) {
            logger<CangJieQuickFixesListBuilder>().error(
                """
                Specific diagnostic class expected instead of generic ${CaDiagnosticWithPsi::class}.
                Factory registered this way would never be used.
                The registered factory class was: ${factory::class}.
                """.trimIndent()
            )
        }

        quickFixes.getOrPut(diagnosticClass) { mutableListOf() } += factory
    }

    /**
     * 冻结构建器并生成只读 quick-fix 列表。
     */
    @OptIn(ForCangJieQuickFixesListBuilder::class)
    private fun build(): CangJieQuickFixesList = CangJieQuickFixesList(quickFixes)

    companion object {
        /**
         * 执行注册 DSL 并返回构建完成的 quick-fix 列表。
         */
        fun registerQuickFixes(init: CangJieQuickFixesListBuilder.() -> Unit): CangJieQuickFixesList =
            CangJieQuickFixesListBuilder().apply(init).build()
    }
}

/**
 * 合并多个键到列表的映射，并保留每个键下元素的原始遍历顺序。
 */
private fun <K, V> List<Map<K, List<V>>>.merge(): Map<K, List<V>> {
    return flatMap { it.entries }
        .groupingBy { it.key }
        .aggregate<Map.Entry<K, List<V>>, K, MutableList<V>> { _, accumulator, element, _ ->
            val list = accumulator ?: mutableListOf()
            list.addAll(element.value)
            list
        }
}

/**
 * 限制 quick-fix 列表主构造器只能由 builder 或内部聚合逻辑直接调用。
 */
@RequiresOptIn
annotation class ForCangJieQuickFixesListBuilder
