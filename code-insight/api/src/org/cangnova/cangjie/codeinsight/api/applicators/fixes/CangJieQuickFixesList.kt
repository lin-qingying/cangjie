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
    private val quickFixes: Map<KClass<out CaDiagnosticWithPsi<*>>, List<CangJieQuickFixFactory<*>>>,
) {
    fun CaSession.canProduceQuickFixesFor(diagnostic: CaDiagnosticWithPsi<*>): Boolean =
        quickFixes[diagnostic.diagnosticClass]?.isNotEmpty() == true

    fun CaSession.getQuickFixesFor(diagnostic: CaDiagnosticWithPsi<*>): List<IntentionAction> =
        getQuickFixesWithCatchingFor(diagnostic).mapTo(mutableListOf()) { it.getOrThrow() }

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

    class ComputingQuickFixesError(message: String, cause: Throwable) : IllegalStateException(message, cause)

    companion object {
        @OptIn(ForCangJieQuickFixesListBuilder::class)
        fun createCombined(registrars: List<CangJieQuickFixesList>): CangJieQuickFixesList {
            val allQuickFixes = registrars.map { it.quickFixes }.merge()
            return CangJieQuickFixesList(allQuickFixes)
        }

        fun createCombined(vararg registrars: CangJieQuickFixesList): CangJieQuickFixesList =
            createCombined(registrars.toList())
    }
}

/**
 * CangJie K2 quick-fix 列表构建器。
 */
class CangJieQuickFixesListBuilder private constructor() {
    private val quickFixes = LinkedHashMap<
        KClass<out CaDiagnosticWithPsi<*>>,
        MutableList<CangJieQuickFixFactory<out CaDiagnosticWithPsi<*>>>,
        >()

    inline fun <reified DIAGNOSTIC : CaDiagnosticWithPsi<*>> registerFactory(
        factory: CangJieQuickFixFactory<DIAGNOSTIC>,
    ) {
        registerFactory(DIAGNOSTIC::class, factory)
    }

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

    @OptIn(ForCangJieQuickFixesListBuilder::class)
    private fun build(): CangJieQuickFixesList = CangJieQuickFixesList(quickFixes)

    companion object {
        fun registerQuickFixes(init: CangJieQuickFixesListBuilder.() -> Unit): CangJieQuickFixesList =
            CangJieQuickFixesListBuilder().apply(init).build()
    }
}

private fun <K, V> List<Map<K, List<V>>>.merge(): Map<K, List<V>> {
    return flatMap { it.entries }
        .groupingBy { it.key }
        .aggregate<Map.Entry<K, List<V>>, K, MutableList<V>> { _, accumulator, element, _ ->
            val list = accumulator ?: mutableListOf()
            list.addAll(element.value)
            list
        }
}

@RequiresOptIn
annotation class ForCangJieQuickFixesListBuilder
