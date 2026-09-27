package org.cangnova.cangjie.analysis.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import kotlin.reflect.KClass

/** Analysis API 中可导航的诊断相关位置。 */
interface CaDiagnosticRelatedInformation : CaLifetimeOwner {
    /** 对应工作区源码时可直接导航的 PSI；二进制来源可能没有 PSI。 */
    val psi: PsiElement?

    /** PSI 范围内的相对高亮区间；二进制来源没有文本范围。 */
    val textRange: TextRange?

    /** 二进制来源或外部源码的路径。 */
    val filePath: String?

    /** CJO 声明携带的原始源文件行号。 */
    val line: Int?

    /** CJO 声明携带的原始源文件列号。 */
    val column: Int?

    /** 该相关位置的说明。 */
    val message: String
}

/**
 * Analysis API 对外暴露的诊断根接口。
 *
 * - 表示由 CFIR/PSI 层产生、对调用方公开的一条语义诊断;
 * - 不暴露底层 reporter、参数槽位等实现细节,只保留稳定面;
 * - 受 [CaLifetimeOwner] 约束,不能逃逸出当前 analyze 块。
 *
 * 对齐 Kotlin Analysis API 的 `KaDiagnostic`。
 */
interface CaDiagnostic : CaLifetimeOwner {
    /**
     * 当前诊断的具体 KClass。
     *
     * 调用方据此把诊断 narrow 为子接口(如 [CaDiagnosticWithPsi]),
     * 进一步获取强类型字段。
     */
    val diagnosticClass: KClass<*>

    /** 诊断工厂名,用于跨实现统一识别同一诊断种类。 */
    val factoryName: String

    /** 诊断等级,见 [CaSeverity]。 */
    val severity: CaSeverity

    /** 默认渲染消息,未绑定任何渲染策略时使用。 */
    val defaultMessage: String

    /** 主诊断附带的 peer/source 位置；常规诊断为空。 */
    val relatedInformation: List<CaDiagnosticRelatedInformation>
        get() = emptyList()
}

/**
 * 返回带工厂名前缀的默认消息,常用于测试断言与日志,格式形如 `[FACTORY] message`。
 */
fun CaDiagnostic.getDefaultMessageWithFactoryName(): String {
    return "[$factoryName] $defaultMessage"
}
