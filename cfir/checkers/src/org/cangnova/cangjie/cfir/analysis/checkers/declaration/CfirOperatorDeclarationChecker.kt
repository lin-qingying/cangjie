package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.modifierByToken
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorString

/**
 * operator 函数声明签名检查器。
 *
 * 该检查器验证普通 unary/binary operator 的参数个数，以及 `operator set` 的下标赋值协议。
 */
object CfirOperatorDeclarationChecker : CfirSimpleFunctionChecker() {
    /**
     * 一元 operator 名称集合。
     */
    private val unaryOperatorNames: Set<Name> = setOf(
        OperatorNameConventions.NOT,
        OperatorNameConventions.UNARY_MINUS,
        OperatorNameConventions.UNARY_PLUS,
        OperatorNameConventions.INC,
        OperatorNameConventions.DEC,
    )

    /**
     * 需要专门签名规则的 operator 名称集合。
     */
    private val specialArityOperatorNames: Set<Name> = setOf(
        OperatorNameConventions.INVOKE,
        OperatorNameConventions.GET,
        OperatorNameConventions.SET,
    )

    /**
     * 二元 operator 名称集合。
     */
    private val binaryOperatorNames: Set<Name> =
        OperatorNameConventions.TOKENS_BY_OPERATOR_NAME.keys - unaryOperatorNames - specialArityOperatorNames

    /**
     * 检查 operator 函数的参数个数或 `set` 签名约束。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        if (!declaration.status.isOperator) return

        if (declaration.name == OperatorNameConventions.SET) {
            checkSubscriptAssignmentSignature(declaration)
            return
        }

        val expectedParameterCount = expectedParameterCount(declaration.name) ?: return
        val actualParameterCount = declaration.valueParameters.size
        if (actualParameterCount == expectedParameterCount) return

        val diagnosticSource = declaration.operatorDiagnosticSource() ?: return

        reporter.reportOn(
            source = diagnosticSource,
            factory = CfirErrors.INVALID_OPERATOR_PARAMETER_COUNT,
            a = declaration.name.asOperatorString(),
            b = expectedParameterCount.toString(),
            c = actualParameterCount.toString(),
        )
    }

    /**
     * 返回普通 operator 期望的位置参数个数。
     */
    private fun expectedParameterCount(name: Name): Int? = when (name) {
        in unaryOperatorNames -> 0
        in binaryOperatorNames -> 1
        else -> null
    }

    /**
     * `operator set` 表达下标赋值协议：
     * - 至少一个位置参数作为索引；
     * - 必须且只能有一个名为 `value` 的命名参数；
     * - 返回类型必须为 `Unit`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSubscriptAssignmentSignature(declaration: CfirNamedFunction) {
        val diagnosticSource = declaration.operatorDiagnosticSource() ?: return
        val positionalParameters = declaration.valueParameters.filter { !it.isNamed }
        val namedParameters = declaration.valueParameters.filter { it.isNamed }

        if (positionalParameters.isEmpty()) {
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM,
            )
        }

        val hasSingleValueParameter =
            namedParameters.size == 1 && namedParameters.single().name.asString() == "value"
        if (!hasSingleValueParameter) {
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.INVALID_SUBSCRIPT_ASSIGN_PARAMETER,
            )
        }

        val returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType
        if (returnType != null && !returnType.isUnit) {
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.INVALID_SUBSCRIPT_ASSIGN_RETURN,
            )
        }
    }

    /**
     * 获取 operator 诊断的源码范围。
     *
     * 显式 `operator` 修饰符存在时优先定位到修饰符，否则退回声明源码。
     */
    private fun CfirNamedFunction.operatorDiagnosticSource() =
        source?.realSourceModifiers()?.modifierByToken(CjTokens.OPERATOR_KEYWORD)?.source ?: source
}
