package org.cangjie.cfir.diagnostics

import org.cangjie.cfir.common.CfirSourceElement

/**
 * 诊断严重程度。
 */
enum class CfirDiagnosticSeverity {
    ERROR,
    WARNING,
    INFO,
}

/**
 * 诊断数据模型接口。
 */
interface CfirDiagnostic {
    val severity: CfirDiagnosticSeverity
    val message: String
    val source: CfirSourceElement?
    val factoryName: String
}

/**
 * 简单诊断实现。
 */
data class CfirSimpleDiagnostic(
    override val severity: CfirDiagnosticSeverity,
    override val message: String,
    override val source: CfirSourceElement? = null,
    override val factoryName: String,
) : CfirDiagnostic

/**
 * 诊断工厂。
 */
class CfirDiagnosticFactory(
    val name: String,
    val defaultSeverity: CfirDiagnosticSeverity,
) {
    fun on(source: CfirSourceElement?, message: String): CfirDiagnostic =
        CfirSimpleDiagnostic(
            severity = defaultSeverity,
            message = message,
            source = source,
            factoryName = name,
        )
}
