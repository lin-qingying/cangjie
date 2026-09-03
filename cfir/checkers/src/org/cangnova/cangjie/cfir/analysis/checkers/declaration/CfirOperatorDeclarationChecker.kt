package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.modifierByToken
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorString
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * operator 函数声明签名检查器。
 *
 * 该检查器验证普通 unary/binary operator 的参数个数、`operator set` 的下标赋值协议，
 * 以及 primitive 内建 operator 不能在 extend 中被重新定义的声明级规则。
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
        if (declaration.isSourceDeclaration) {
            checkBuiltinPrimitiveOperatorOverload(declaration)
        }

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
     * primitive 类型已有语言内建 operator；普通源码 extend 不能声明同签名 operator。
     *
     * 对齐官方 `TypeCheckerImpl::CheckOperatorOverloadFunc`：
     * - 命中内建一元/二元签名时报 built-in overload 诊断；
     * - 若声明返回类型与内建返回类型不一致，再按函数名报告 `RETURN_TYPE_INCOMPATIBLE`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBuiltinPrimitiveOperatorOverload(declaration: CfirNamedFunction) {
        val containingExtend = context.findClosestDeclaration<CfirExtend>() ?: return
        val receiverType = BuiltinPrimitiveOperators.normalizePrimitiveOperand(
            containingExtend.extendedTypeRef.coneTypeOrNull,
        ) ?: return
        val argumentTypes = declaration.valueParameters.map { parameter ->
            BuiltinPrimitiveOperators.normalizePrimitiveOperand(parameter.returnTypeRef.coneTypeOrNull) ?: return
        }
        val builtinMatch = BuiltinPrimitiveOperators.resolve(
            name = declaration.name,
            receiverType = receiverType,
            argumentTypes = argumentTypes,
        ) ?: return

        val operatorName = declaration.name.asOperatorString()
        val receiverTypeName = receiverType.kind.typeName
        val diagnosticSource = declaration.builtinOperatorOverloadDiagnosticSource()
        if (argumentTypes.isEmpty()) {
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR,
                a = operatorName,
                b = receiverTypeName,
            )
        } else if (!context.containingFileSymbol.isStdCoreFile()) {
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR,
                a = operatorName,
                b = receiverTypeName,
                c = argumentTypes.single().kind.typeName,
            )
        }

        val actualReturnType = context.returnTypeCalculator.tryCalculateReturnType(declaration).coneType
        if (actualReturnType is ConeErrorType) return
        if (AbstractTypeChecker.equalTypes(context.session.typeContext, actualReturnType, builtinMatch.returnType)) return

        reporter.reportOn(
            source = declaration.functionNameDiagnosticSource(),
            factory = CfirErrors.RETURN_TYPE_INCOMPATIBLE,
            a = declaration.name,
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
                source = declaration.subscriptAssignmentReturnDiagnosticSource() ?: diagnosticSource,
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

    /**
     * 内建 operator overload 诊断按官方落在声明起始修饰符；
     * 本项目 range policy 使用完整 token 而不是官方单字符位置。
     */
    private fun CfirNamedFunction.builtinOperatorOverloadDiagnosticSource() =
        source?.realSourceModifiers()?.modifierByToken(CjTokens.PUBLIC_KEYWORD)?.source
            ?: operatorDiagnosticSource()

    /**
     * std.core 内部声明内建二元 operator 时不触发“重载内建 operator”诊断。
     */
    private fun CfirFileSymbol?.isStdCoreFile(): Boolean =
        this?.takeIf { it.isBound }
            ?.cfir
            ?.packageDirective
            ?.packageFqName == StandardNames.STD_CORE_PACKAGE_FQ_NAME
}
