package org.cangnova.cangjie.analysis.api.impl.base.components

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaIdeApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.getModule
import org.cangnova.cangjie.analysis.api.impl.base.CaBaseSession
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseIllegalPsiException.Companion.allowIllegalPsiAccess
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.util.withCaModuleEntry
import org.cangnova.cangjie.utils.exceptions.CangJieIllegalArgumentExceptionWithAttachments
import org.cangnova.cangjie.utils.exceptions.buildAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * 校验 receiver PSI 后在当前 session component 生命周期内执行 [builder]。
 */
@CaImplementationDetail
context(component: CaBaseSessionComponent<S>)
@JvmName("withPsiValidityAssertionAsReceiver")
inline fun <S : CaSession, R> PsiElement?.withPsiValidityAssertion(builder: () -> R): R = component.withValidityAssertion {
    this?.checkValidity()
    builder()
}

/**
 * 校验单个 PSI 后在当前 session component 生命周期内执行 [builder]。
 */
@CaImplementationDetail
context(component: CaBaseSessionComponent<S>)
inline fun <S : CaSession, R> withPsiValidityAssertion(
    element: PsiElement?,
    builder: () -> R,
): R = element.withPsiValidityAssertion(builder)

/**
 * 校验多个 PSI 后在当前 session component 生命周期内执行 [builder]。
 */
@CaImplementationDetail
context(component: CaBaseSessionComponent<S>)
inline fun <S : CaSession, R> withPsiValidityAssertion(
    vararg elements: PsiElement?,
    builder: () -> R,
): R = component.withValidityAssertion {
    for (element in elements) {
        element?.checkValidity()
    }

    builder()
}

/**
 * 校验 PSI 集合后在当前 session component 生命周期内执行 [builder]。
 */
@CaImplementationDetail
context(component: CaBaseSessionComponent<S>)
inline fun <S : CaSession, R> withPsiValidityAssertion(
    elements: Iterable<PsiElement?>,
    builder: () -> R,
): R = component.withValidityAssertion {
    for (element in elements) {
        element?.checkValidity()
    }

    builder()
}

/**
 * 校验当前 PSI 是否可在当前 session 中分析。
 */
@CaImplementationDetail
context(component: CaBaseSessionComponent<S>)
fun <S : CaSession> PsiElement.checkValidity() {
    val session = component.analysisSession
    val canBeAnalyzed = if (session is CaBaseSession) {
        session.canBeAnalysedImpl(this)
    } else {
        // This `else` branch is a temporal workaround for the swift-export which creates its own CaSession implementation
        // It has to be removed after the swift-export dropped this API violation
        with(session) {
            canBeAnalysed()
        }
    }

    if (!canBeAnalyzed && Registry.`is`("cangjie.analysis.validate.psi.input", true) && !allowIllegalPsiAccess.get()) {
        throw CaBaseIllegalPsiException.create(session, this)
    }
}

/**
 * PSI 与当前 use-site session 不匹配时抛出的 Analysis API 输入错误。
 */
@CaImplementationDetail
class CaBaseIllegalPsiException private constructor(
    useSiteModule: CaModule,
    psiModule: CaModule,
    psi: PsiElement,
) : CangJieIllegalArgumentExceptionWithAttachments(
    "The element cannot be analyzed in the context of the current session.\n" +
            "The call site should be adjusted according to ${CaSession::class.simpleName} KDoc.\n" +
            "Use site module class: ${useSiteModule::class.simpleName}\n" +
            "PSI module class: ${psiModule::class.simpleName}\n" +
            "PSI element class: ${psi::class.simpleName}",
) {
    init {
        buildAttachment("info.txt") {
            withCaModuleEntry("useSiteModule", useSiteModule)
            withCaModuleEntry("psiModule", psiModule)

            runCatching {
                withPsiEntry("psi", psi)
            }.exceptionOrNull()?.let {
                withEntry("psiException", it.stackTraceToString())
            }
        }
    }

    companion object {
        /**
         * 根据 session 与非法 PSI 构造异常，并附带 use-site/psi 模块信息。
         */
        fun create(session: CaSession, psi: PsiElement):   CaBaseIllegalPsiException = with(session) {
            val psiModule = getModule(psi)
            CaBaseIllegalPsiException(useSiteModule, psiModule, psi)
        }

        /**
         * This is a temporary solution to allow accessing PSI elements from unrelated [CaSession] to allow usages for old places
         * and forbit incorrect behavior in new places.
         */
        @CaImplementationDetail
        @CaIdeApi
        @Suppress("unused")
        fun <T> allowIllegalPsiAccess(action: () -> T): T {
            val old =  allowIllegalPsiAccess.get()
             allowIllegalPsiAccess.set(true)
            return try {
                action()
            } finally {
                 allowIllegalPsiAccess.set(old)
            }
        }
    }
}

/**
 * 允许旧调用点临时跳过非法 PSI 访问检查的线程局部开关。
 */
private val allowIllegalPsiAccess = ThreadLocal.withInitial { false }
