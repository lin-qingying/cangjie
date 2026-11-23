package org.cangnova.cangjie.codeinsight.api.classic.quickfixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import kotlin.reflect.KClass

/**
 * 以诊断 PSI 为输入创建传统 IntentionAction 的 factory。
 *
 * 调用具体 factory 前会验证 PSI 类型和适用性，使注册时声明的 PSI 契约在运行期也有明确检查。
 */
abstract class QuickFixesPsiBasedFactory<PSI : PsiElement>(
    private val classTag: KClass<PSI>,
    private val suitabilityChecker: PsiElementSuitabilityChecker<PSI>,
) : QuickFixFactory {
    /** 验证并创建 quick fix。 */
    fun createQuickFix(psiElement: PsiElement): List<IntentionAction> {
        checkIfPsiElementIsSupported(psiElement)
        @Suppress("UNCHECKED_CAST")
        return doCreateQuickFix(psiElement as PSI)
    }

    private fun checkIfPsiElementIsSupported(psiElement: PsiElement) {
        if (!classTag.java.isInstance(psiElement)) {
            throw InvalidPsiElementTypeException(
                expectedPsiType = psiElement::class,
                actualPsiType = classTag,
                factoryName = this::class.toString(),
            )
        }

        @Suppress("UNCHECKED_CAST")
        if (!suitabilityChecker.isSupported(psiElement as PSI)) {
            throw UnsupportedPsiElementException(psiElement, this::class.toString())
        }
    }

    protected abstract fun doCreateQuickFix(psiElement: PSI): List<IntentionAction>
}

/** 创建带 PSI 类型标签和适用性规则的 quick-fix factory。 */
inline fun <reified PSI : PsiElement> quickFixesPsiBasedFactory(
    suitabilityChecker: PsiElementSuitabilityChecker<PSI> = PsiElementSuitabilityCheckers.ALWAYS_SUITABLE,
    crossinline createQuickFix: (PSI) -> List<IntentionAction>,
): QuickFixesPsiBasedFactory<PSI> =
    object : QuickFixesPsiBasedFactory<PSI>(PSI::class, suitabilityChecker) {
        override fun doCreateQuickFix(psiElement: PSI): List<IntentionAction> = createQuickFix(psiElement)
    }

/** 把一种 PSI factory 映射到另一种 PSI 类型，保留原 factory 的创建逻辑。 */
inline fun <reified PSI : PsiElement, reified PSI2 : PsiElement> QuickFixesPsiBasedFactory<PSI>.coMap(
    suitabilityChecker: PsiElementSuitabilityChecker<PSI2> = PsiElementSuitabilityCheckers.ALWAYS_SUITABLE,
    crossinline map: (PSI2) -> PSI?,
): QuickFixesPsiBasedFactory<PSI2> =
    quickFixesPsiBasedFactory(suitabilityChecker) { psiElement ->
        val mappedPsi = map(psiElement) ?: return@quickFixesPsiBasedFactory emptyList()
        createQuickFix(mappedPsi)
    }

class InvalidPsiElementTypeException(
    expectedPsiType: KClass<out PsiElement>,
    actualPsiType: KClass<out PsiElement>,
    factoryName: String,
) : Exception(
    "PsiElement with type " + expectedPsiType +
        " is expected but " + actualPsiType + " found for " + factoryName,
)

class UnsupportedPsiElementException(
    psiElement: PsiElement,
    factoryName: String,
) : Exception("PsiElement " + psiElement + " is unsupported for " + factoryName)
