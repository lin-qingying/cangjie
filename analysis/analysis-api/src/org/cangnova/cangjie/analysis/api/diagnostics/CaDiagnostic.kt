package org.cangnova.cangjie.analysis.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import kotlin.reflect.KClass

/**
 * 诊断严重级别。
 */
enum class CaSeverity {
    ERROR,
    WARNING,
    INFO,
}

/**
 * Analysis API 诊断的统一抽象。
 */
interface CaDiagnostic : CaLifetimeOwner {
    /** 诊断包装类型的运行时类。 */
    val diagnosticClass: KClass<*>

    /** 诊断工厂名称。 */
    val factoryName: String

    /** 诊断严重级别。 */
    val severity: CaSeverity

    /** 诊断默认消息文本。 */
    val defaultMessage: String
}

/**
 * 带 PSI 位置信息的诊断。
 */
interface CaDiagnosticWithPsi<out PSI : PsiElement> : CaDiagnostic {
    /** 诊断关联的 PSI 元素。 */
    val psi: PSI

    /** 诊断在文件中的文本范围。 */
    val textRanges: Collection<TextRange>

    /** 更精确的诊断包装类型。 */
    override val diagnosticClass: KClass<out CaDiagnosticWithPsi<PSI>>
}

/**
 * 返回包含工厂名的默认诊断消息。
 */
fun CaDiagnostic.getDefaultMessageWithFactoryName(): String {
    return "[$factoryName] $defaultMessage"
}
