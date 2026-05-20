package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.unwrap
import org.cangnova.cangjie.analysis.api.components.CaExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.withPsiValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.utils.errors.unexpectedElementError
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.declarations.CfirPackageDirective
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjOperationExpression
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import org.cangnova.cangjie.psi.CjPrefixExpression
import org.cangnova.cangjie.psi.CjReturnExpression
import org.cangnova.cangjie.psi.CjStringTemplateExpression
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.psiUtil.getOutermostParenthesizerOrThis
import org.cangnova.cangjie.psi.stubs.ConstantValueKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * 对齐 Kotlin `CaCfirExpressionTypeProvider` 的组件落位。
 *
 * 这里只负责把 CFIR 已经求出的表达式/声明返回类型投影到公开 `CaType`，
 * 不混入类型关系、类型构造等其他职责。
 */
internal class CaCfirExpressionTypeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionTypeProvider, CaCfirSessionComponent {
    @OptIn(CaImplementationDetail::class)
    override val CjExpression.expressionType: CaType?
        get() = with(this@CaCfirExpressionTypeProvider as CaBaseSessionComponent<CaCfirSession>) {
            this@expressionType.withPsiValidityAssertion {
                val cfir = this@expressionType.unwrap().getOrBuildCfir(resolutionFacade) ?: return null
                return try {
                    getCjExpressionType(this@expressionType, cfir)
                } catch (e: Exception) {
                    rethrowExceptionWithDetails("Exception during resolving ${this@expressionType::class.simpleName}", e) {
                        withPsiEntry("expression", this@expressionType)
                        withCfirEntry("cfir", cfir)
                    }
                }
            }
        }

    override val PsiElement.expectedType: CaType?
        get() = with(this@CaCfirExpressionTypeProvider as CaBaseSessionComponent<CaCfirSession>) {
            this@expectedType.withPsiValidityAssertion {
                val unwrapped = unwrapExpectedTypeTarget()
                getExpectedTypeOfFunctionParameter(unwrapped)
                    ?: getExpectedTypeByReturnExpression(unwrapped)
            }
        }

    private fun getCjExpressionType(expression: CjExpression, cfir: CfirElement): CaType? = when (cfir) {
        is CfirFunctionCall -> cfir.resolvedType.asCaType()
        is CfirSuperReceiverExpression -> cfir.resolvedType.asCaType()
        is CfirAssignment -> {
            if (cfir.lValue.psi == expression) {
                cfir.lValue.resolvedType.asCaType()
            } else {
                analysisSession.cfirSession.builtinTypes.unitType.asCaType()
            }
        }
        is CfirExpression -> cfir.resolvedType.asCaType()
        is CfirNamedReference -> cfir.getCorrespondingTypeIfPossible()?.asCaType()
        is CfirStatement -> analysisSession.cfirSession.builtinTypes.unitType.asCaType()
        is CfirTypeRef, is CfirImport, is CfirPackageDirective, is CfirTypeParameterRef -> null
        else -> null
    }

    @OptIn(CaImplementationDetail::class)
    override val CjCallableDeclaration.returnType: CaType?
        get() = with(this@CaCfirExpressionTypeProvider as CaBaseSessionComponent<CaCfirSession>) {
            this@returnType.withPsiValidityAssertion {
                this@returnType.inferReturnTypeByPsi()?.let { return it }
                val cfirDeclaration = this@returnType.resolveToCfirSymbol(
                    resolutionFacade = resolutionFacade,
                    phase = CfirResolvePhase.TYPES,
                ).cfir

                return when (cfirDeclaration) {
                    is CfirCallableDeclaration -> cfirDeclaration.symbol.resolvedReturnType.asCaType()
                    else -> unexpectedElementError<CfirElement>(cfirDeclaration)
                }
            }
        }

    /**
     * 当前先补齐 analysis 测试已经依赖的两个稳定入口：
     * 1. 函数调用参数位置的期望类型；
     * 2. return 表达式的期望类型。
     *
     * 其余 expected-type 场景后续继续按 Kotlin 对位补齐。
     */
    private fun getExpectedTypeOfFunctionParameter(element: PsiElement): CaType? {
        val argumentExpression = element as? CjExpression ?: return null
        val valueArgument = argumentExpression.parent as? CjValueArgument ?: return null
        val callExpression = valueArgument.parent?.parent as? CjCallExpression ?: return null
        val call = with(analysisSession) { callExpression.resolveToCall() }?.successfulFunctionCallOrNull() ?: return null
        return call.valueArgumentMapping[argumentExpression]?.returnType
    }

    private fun getExpectedTypeByReturnExpression(element: PsiElement): CaType? {
        val returnedExpression = element as? CjExpression ?: return null
        val returnExpression = returnedExpression.parent as? CjReturnExpression ?: return null
        val ownerFunction = returnExpression.getStrictParentOfType<CjNamedFunction>() ?: return null
        return ownerFunction.returnType
    }

    /**
     * 对齐 Kotlin FIR provider 的做法，在进入完整符号解析前先用 PSI 做一层便宜推断。
     *
     * 这层推断只处理“语法上即可确定”的场景，避免把声明返回类型查询退化成总是触发完整解析。
     */
    private fun CjCallableDeclaration.inferReturnTypeByPsi(): CaType? {
        val declaration = this as? CjDeclarationWithBody ?: return null
        if (declaration !is CjNamedFunction) return null
        if (declaration.hasDeclaredReturnType()) return null

        if (declaration.hasBlockBody()) {
            return analysisSession.cfirSession.builtinTypes.unitType.asCaType()
        }

        val singleExpression = declaration.initializer ?: declaration.bodyExpression ?: return null
        return inferExpressionTypeByPsi(singleExpression)
    }

    /**
     * 只处理无需语义解析即可稳定确定的字面量类型。
     */
    private fun inferExpressionTypeByPsi(expression: CjExpression): CaType? = when (expression) {
        is CjStringTemplateExpression -> analysisSession.buildClassType(StdlibClassIds.String)
        is CjConstantExpression -> inferConstantTypeByPsi(expression)
        else -> null
    }

    private fun inferConstantTypeByPsi(expression: CjConstantExpression): CaType? {
        val constantKind = expression.stub?.kind() ?: when (expression.node.elementType) {
            CjStubElementTypes.BOOLEAN_CONSTANT -> ConstantValueKind.BOOLEAN_CONSTANT
            CjStubElementTypes.FLOAT_CONSTANT -> ConstantValueKind.FLOAT_CONSTANT
            CjStubElementTypes.RUNE_CONSTANT -> ConstantValueKind.RUNE_CONSTANT
            CjStubElementTypes.CHARACTER_BYTE_CONSTANT -> ConstantValueKind.CHARACTER_BYTE_CONSTANT
            CjStubElementTypes.INTEGER_CONSTANT -> ConstantValueKind.INTEGER_CONSTANT
            CjStubElementTypes.UNIT_CONSTANT -> ConstantValueKind.UNIT_CONSTANT
            else -> null
        }

        val builtinTypes = analysisSession.cfirSession.builtinTypes
        return when (constantKind) {
            ConstantValueKind.BOOLEAN_CONSTANT -> builtinTypes.boolType.asCaType()
            ConstantValueKind.FLOAT_CONSTANT -> builtinTypes.float64Type.asCaType()
            ConstantValueKind.RUNE_CONSTANT, ConstantValueKind.CHARACTER_BYTE_CONSTANT -> builtinTypes.runeType.asCaType()
            ConstantValueKind.INTEGER_CONSTANT -> builtinTypes.int64Type.asCaType()
            ConstantValueKind.UNIT_CONSTANT -> builtinTypes.unitType.asCaType()
            null -> null
        }
    }

    /**
     * 仅当名字引用最终属于“取值表达式”时，才返回对应类型。
     *
     * 这样可以避免把普通函数名本身错误地暴露成某个伪类型，同时保留函数值变量、
     * 属性访问等真正有值语义的场景。
     */
    private fun CfirNamedReference.getCorrespondingTypeIfPossible() =
        findOuterNamedAccessExpression()?.resolvedType

    private fun CfirNamedReference.findOuterNamedAccessExpression(): CfirExpression? {
        val referenceExpression = psi as? CjExpression ?: return null
        val outerExpression = referenceExpression.getOutermostParenthesizerOrThis().parent as? CjElement ?: return null

        return when (val outerCfirElement = outerExpression.getOrBuildCfir(resolutionFacade)) {
            is CfirAssignment -> outerCfirElement.lValue
            is CfirNamedAccessExpression -> outerCfirElement
            is CfirOptionalChainExpression -> outerCfirElement.expression as? CfirNamedAccessExpression
            else -> null
        }
    }

    private inline fun <reified T : PsiElement> PsiElement.getStrictParentOfType(): T? {
        var current = parent
        while (current != null) {
            if (current is T) return current
            current = current.parent
        }
        return null
    }

    private fun PsiElement.unwrapExpectedTypeTarget(): PsiElement = when (this) {
        is CjParenthesizedExpression -> expression ?: this
        is CjPrefixExpression -> baseExpression ?: this
        else -> this
    }
}
