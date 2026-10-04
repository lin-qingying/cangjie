package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.cfir.utils.unwrap
import org.cangnova.cangjie.analysis.api.components.CaExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.withPsiValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaEnumConstructorCall
import org.cangnova.cangjie.analysis.api.resolution.CaFunctionCall
import org.cangnova.cangjie.analysis.api.resolution.calls
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
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjDeclarationWithInitializer
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjContainerNode
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjIfExpression
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.psi.CjWhileExpressionBase
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
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
    /**
     * 延迟取得当前 CFIR Analysis session，类型查询通过该 session 访问解析门面和类型构建器。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionTypeProvider, CaCfirSessionComponent {
    @OptIn(CaImplementationDetail::class)
    /**
     * 返回表达式在当前解析上下文中的实际类型。
     */
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

    /**
     * 返回 PSI 位置在当前上下文中被期望的类型。
     *
     * 覆盖的场景按「约束来源是否确定」排列：
     * 1. 调用实参与 return（沿用既有入口）；
     * 2. 显式类型的初始化器、赋值右侧、命名参数默认值；
     * 3. 普通布尔条件与带 `else` 的 `if` 分支、`match` 分支块；
     * 4. 不完整调用：从本次解析保留的候选中恢复，全部候选一致时才给出确定类型。
     *
     * 省略类型的初始化器、没有外部约束的分支、以及声明前/块非末项一律返回 `null`：
     * 仓颉在这些位置按“最小公共父类型”求值，调用方不能把某个兄弟表达式的实际类型
     * 当作该位置的期望类型。
     */
    override val PsiElement.expectedType: CaType?
        get() = with(this@CaCfirExpressionTypeProvider as CaBaseSessionComponent<CaCfirSession>) {
            this@expectedType.withPsiValidityAssertion {
                val unwrapped = unwrapExpectedTypeTarget()
                getExpectedTypeOfFunctionParameter(unwrapped)
                    ?: getExpectedTypeByReturnExpression(unwrapped)
                    ?: getExpectedTypeByDeclaration(unwrapped)
                    ?: getExpectedTypeByAssignment(unwrapped)
                    ?: getExpectedTypeByCondition(unwrapped)
                    ?: getExpectedTypeByBranchResult(unwrapped)
                    ?: getExpectedTypeOfUnresolvedCall(unwrapped)
            }
        }

    /**
     * 将已构建的 CFIR 元素转换为公开表达式类型。
     */
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
    /**
     * 返回 callable 声明的公开返回类型。
     */
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

    /**
     * 当元素是显式声明类型的初始化器或命名参数默认值时，返回声明类型。
 *
     * 未标注类型的声明属于“由初始化器推导”，不能反过来把初始化器类型当作期望类型。
 */
    private fun getExpectedTypeByDeclaration(element: PsiElement): CaType? {
        val expression = element as? CjExpression ?: return null
        return when (val parent = expression.parent) {
            is CjDeclarationWithInitializer -> parent.declaredTypeOrNull()
            is CjParameter ->
                if (parent.isNamed && parent.defaultValue === expression) {
                    with(analysisSession) { parent.typeReference?.type }
                } else {
                    null
                }

            else -> null
        }
    }

/**
     * 当元素是赋值表达式的右值时，返回左值的声明类型。
 *
     * 左值类型不可用或本身是错误类型时不给出期望类型，避免把坏类型传播成候选过滤条件。
 */
    private fun getExpectedTypeByAssignment(element: PsiElement): CaType? {
        val right = element as? CjExpression ?: return null
        val assignment = right.parent as? CjBinaryExpression ?: return null
        if (assignment.operationToken != CjTokens.EQ || assignment.right !== right) return null
        val left = assignment.left ?: return null
        return left.expressionType?.takeUnless { it.isErrorOrUnresolved }
    }

    /**
     * 当元素是普通 `if`/`while` 条件时，返回 `Bool`。
     *
     * `let` 模式的初始化值有自己的类型，不能因为它出现在条件位置上就当成 `Bool`。
     */
    private fun getExpectedTypeByCondition(element: PsiElement): CaType? {
        val condition = element as? CjExpression ?: return null
        // 条件常挂在 CjContainerNode 之下，必须穿过容器取到真正的 if/while 宿主再比对槽位。
        val host = condition.skipToNonContainerParent()
        val declared = when (host) {
            is CjIfExpression -> host.condition
            is CjWhileExpressionBase -> host.condition
            else -> null
        }
        if (declared !== condition) return null
        return analysisSession.cfirSession.builtinTypes.boolType.asCaType()
    }

    /**
     * 当元素是块的最后一项或分支体时，沿用外层表达式的期望类型。
     *
     * `if` 必须带 `else` 才继承整个 `if` 的外部约束（无 `else` 时按 `Unit` 处理）；
     * `match` 各分支块继承 `match` 的外部约束。块的非末项不参与类型推导，因此不传播。
     */
    private fun getExpectedTypeByBranchResult(element: PsiElement): CaType? {
        var current: PsiElement = element
        while (true) {
            val owner = current.skipToNonContainerParent() ?: return null
            when (owner) {
                // 只有块的末项参与该块的结果类型，非末项不传播任何约束。
                is CjBlockExpression -> {
                    val statements = owner.statements
                    if (statements.isEmpty() || statements.last() !== current) return null
                    current = owner
                }

                // `if` 只有带 `else` 时才继承外部约束；否则整个表达式按 Unit 处理。
                is CjIfExpression -> {
                    val branches = listOfNotNull(owner.then, owner.`else`)
                    if (owner.`else` == null || branches.none { it === current }) return null
                    current = owner
                }

                // 各分支块继承 `match` 的外部约束。
                is CjMatchEntry -> if (owner.body !== current) return null else current = owner

                is CjMatchExpression -> current = owner

                is CjDeclarationWithInitializer -> return owner.declaredTypeOrNull()

                is CjParameter ->
                    return if (owner.isNamed && owner.defaultValue === current) {
                        with(analysisSession) { owner.typeReference?.type }
                    } else {
                        null
                    }

                is CjReturnExpression ->
                    return owner.getStrictParentOfType<CjNamedFunction>()?.returnType

                else -> return null
            }
        }
    }

    /**
     * 从未成功解析的调用中恢复可确定的实参期望类型。
     *
     * 只使用本次解析已保留并公开的候选（`CaCallInfo.calls`，含错误调用的 `candidateCalls`），
     * 不枚举全部词法候选。任一候选缺少目标实参映射、映射类型未确定，
     * 或候选之间约束不一致时返回 `null`：此时不能任取第一个候选的类型。
     */
    private fun getExpectedTypeOfUnresolvedCall(element: PsiElement): CaType? {
        val argument = element as? CjExpression ?: return null
        val valueArgument = argument.parent as? CjValueArgument ?: return null
        val callExpression = valueArgument.parent?.parent as? CjCallExpression ?: return null
        val callInfo = with(analysisSession) { callExpression.resolveToCall() } ?: return null
        val calls = callInfo.calls
        if (calls.isEmpty()) return null

        val recovered = calls.map { call: CaCall -> recoveredArgumentType(call, argument) }
        if (recovered.any { it == null || it.isErrorOrUnresolved }) return null
        val types = recovered.filterNotNull()
        val first = types.first()
        return first.takeIf { candidate -> types.drop(1).all { with(analysisSession) { candidate.semanticallyEquals(it) } } }
    }

    /**
     * 取出单个调用候选为该实参映射到的参数类型。
     */
    private fun recoveredArgumentType(call: CaCall, argument: CjExpression): CaType? = when (call) {
        is CaFunctionCall<*> -> call.valueArgumentMapping[argument]?.returnType
        is CaEnumConstructorCall<*> -> call.payloadArgumentMapping[argument]
        else -> null
    }

    /**
     * 判断类型是否含错误类型或未解类型；这些类型不得作为期望类型传播。
     */
    private val CaType.isErrorOrUnresolved: Boolean
        get() {
            val cfirType = (this as? CaCfirType)?.coneType ?: return true
            // 未定的类型变量/存根同样不能作为期望类型：它们随时可能被替换成别的类型。
            return cfirType.containsErrorType() ||
                cfirType.contains { it is ConeTypeVariableType || it is ConeStubType }
        }

    /**
     * 穿过只做结构分组的容器节点，返回真正承载语义的父节点。
     *
     * `if` 的分支体、条件等经常挂在 `CjContainerNode` 之下；不跳过容器就会把这些
     * 槽位误判成没有宿主，从而漏掉确定存在的期望类型。
     */
    private fun PsiElement.skipToNonContainerParent(): PsiElement? {
        var current: PsiElement = this
        while (true) {
            current = current.parent ?: return null
            if (current !is CjContainerNode) return current
        }
    }

    /**
     * 取得带初始化器声明的显式类型引用。
     */
    private fun CjDeclarationWithInitializer.declaredTypeOrNull(): CaType? =
        (this as? CjCallableDeclaration)?.typeReference?.let { typeReference ->
            with(analysisSession) { typeReference.type }
        }

    /**
     * 当元素是 return 表达式的返回值时，返回所属函数的返回类型。
     */
    private fun getExpectedTypeByReturnExpression(element: PsiElement): CaType? {
        val returnedExpression = element as? CjExpression ?: return null
        val returnExpression = returnedExpression.parent as? CjReturnExpression ?: return null
        val ownerFunction = returnExpression.getStrictParentOfType<CjNamedFunction>() ?: return null
        return ownerFunction.returnType
    }

    /**
     * 在进入完整符号解析前先用 PSI 做一层便宜推断。
     *
     * 仓颉的具名函数体一律是块体（没有 Kotlin `func f() = expr` 的表达式体形式），
     * 省略返回类型表示“由函数体推导”，而不是 Kotlin 的“块体即 Unit”。
     * 因此这里只对语法上即可确定的单表达式函数体（字面量、字符串模板）给出答案；
     * 其余情况返回 null，交给下面的 CFIR 路径做真正的返回类型推导。
     */
    private fun CjCallableDeclaration.inferReturnTypeByPsi(): CaType? {
        val declaration = this as? CjDeclarationWithBody ?: return null
        if (declaration !is CjNamedFunction) return null
        if (declaration.hasDeclaredReturnType()) return null

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

    /**
     * 基于常量表达式的 token/stub 种类快速推断内建类型。
     */
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
            ConstantValueKind.RUNE_CONSTANT -> builtinTypes.runeType.asCaType()
            // 官方 RUNE_BYTE 定型为 UInt8（GetNumLitTypeKind），不得并入 Rune。
            ConstantValueKind.CHARACTER_BYTE_CONSTANT -> builtinTypes.uint8Type.asCaType()
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

    /**
     * 查找名字引用外层真正承载取值语义的 CFIR 表达式。
     */
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

    /**
     * 从当前 PSI 的父级开始查找指定类型的严格父节点。
     */
    private inline fun <reified T : PsiElement> PsiElement.getStrictParentOfType(): T? {
        var current = parent
        while (current != null) {
            if (current is T) return current
            current = current.parent
        }
        return null
    }

    /**
     * 去除不改变期望类型目标的轻量 PSI 包装。
     */
    private fun PsiElement.unwrapExpectedTypeTarget(): PsiElement = when (this) {
        is CjParenthesizedExpression -> expression ?: this
        is CjPrefixExpression -> baseExpression ?: this
        else -> this
    }
}
