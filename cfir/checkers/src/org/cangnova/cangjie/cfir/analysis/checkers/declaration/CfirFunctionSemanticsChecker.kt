package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjOperationName
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjPsiSourceElement

/**
 * 函数语义检查器（Function 分组）
 *
 * 检查 static 函数重载冲突（同名函数不能混合 static 和 non-static）。
 * 对齐 C++ TypeChecker 中 sema_static_function_overload_conflicts 检查。
 */
object CfirFunctionOverloadChecker : CfirSimpleFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        checkStaticNonStaticOverloadConflict(declaration)
    }

    /**
     * 检查同名函数不能混合 static 和 non-static。
     *
     * 对齐 C++ DiagKind::sema_static_function_overload_conflicts:
     * 当同一个类/结构体/枚举中存在同名的 static 和 non-static 函数时报错。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticNonStaticOverloadConflict(function: CfirNamedFunction) {
        val ownerClassId = function.symbol.callableId.classId ?: return
        val owner = context.session.symbolProvider
            .getClassLikeSymbolByClassId(ownerClassId)?.cfir as? CfirClassLikeDeclaration ?: return

        val isStatic = function.status.isStatic
        val functionName = function.name

        // 在同一类型的声明中查找同名但 static 属性不同的函数
        val hasConflict = owner.declarations.any { sibling ->
            sibling is CfirNamedFunction &&
                    sibling !== function &&
                    sibling.name == functionName &&
                    sibling.status.isStatic != isStatic
        }

        if (hasConflict) {
            reporter.reportOn(
                source = function.source,
                factory = CfirErrors.STATIC_FUNCTION_OVERLOAD_CONFLICTS,
                a = functionName,
            )
        }
    }
}

/**
 * 函数声明状态合法性检查器。
 *
 * 对齐仓颉声明属性语义：
 * - `mut func` 只允许作为 struct 成员函数；
 * - `static` 函数不能同时承担 open / abstract / override / operator 这类实例分派语义。
 */
object CfirFunctionDeclarationStatusChecker : CfirSimpleFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        checkMutFunction(declaration)
        checkStaticFunctionStatus(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMutFunction(function: CfirNamedFunction) {
        if (!function.status.isMut) return
        if (!function.isLocal && context.closestContainingTypeDeclaration() is CfirStruct) return

        reporter.reportOn(
            source = function.nameDiagnosticSource(),
            factory = CfirErrors.MUT_ONLY_ON_FUNCTION,
            a = function.name,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticFunctionStatus(function: CfirNamedFunction) {
        if (!function.status.isStatic) return
        if (!function.status.isOpen &&
            !function.status.isAbstract &&
            !function.status.isOverride &&
            !function.status.isOperator
        ) {
            return
        }

        reporter.reportOn(
            source = function.nameDiagnosticSource(),
            factory = CfirErrors.STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE,
            a = function.name,
        )
    }

    private fun CheckerContext.closestContainingTypeDeclaration() =
        findClosestDeclaration<org.cangnova.cangjie.cfir.declarations.CfirDeclaration> { declaration ->
            declaration is CfirClassLikeDeclaration || declaration is CfirExtend
        }
}

/**
 * 函数返回类型推断检查器
 *
 * 对齐 C++ DiagKind::sema_unable_to_infer_return_type
 */
object CfirFunctionReturnTypeInferenceChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirFunction) {
        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is CfirErrorTypeRef && returnTypeRef.delegatedTypeRef == null) {
            if (declaration is CfirNamedFunction && declaration.body != null) {
                reporter.reportOn(
                    source = declaration.nameDiagnosticSource(),
                    factory = CfirErrors.UNABLE_TO_INFER_RETURN_TYPE,
                )
            }
        }
    }
}

private fun CfirNamedFunction.nameDiagnosticSource(): AbstractCjSourceElement? =
    source?.psi?.let { psi ->
        val functionPsi = when (psi) {
            is CjNamedFunction -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjNamedFunction::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjNamedFunction::class.java)
        }
        val nameElement = functionPsi?.nameIdentifier
            ?: functionPsi?.let { PsiTreeUtil.findChildOfType(it, CjOperationName::class.java) }
        nameElement?.toCjPsiSourceElement()
    }
        ?: (source as? CjSourceElement)?.findFunctionNameSource(name)
        ?: source

private fun CjSourceElement.findFunctionNameSource(name: org.cangnova.cangjie.name.Name): AbstractCjSourceElement? {
    val tokens = mutableListOf<LighterASTNode>()

    fun collectLeaves(node: LighterASTNode) {
        val children = treeStructure.children(node)
        if (children.isEmpty()) {
            tokens += node
            return
        }
        children.forEach(::collectLeaves)
    }

    collectLeaves(lighterASTNode)

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType != CjTokens.FUNC_KEYWORD) continue
        val nameToken = tokens.asSequence()
            .drop(index + 1)
            .firstOrNull { it.tokenType == CjTokens.IDENTIFIER && treeStructure.toString(it).toString() == name.asString() }
            ?: continue
        return CjOffsetsOnlySourceElement(
            startOffset = treeStructure.getStartOffset(nameToken),
            endOffset = treeStructure.getEndOffset(nameToken),
        )
    }

    return null
}

private fun com.intellij.util.diff.FlyweightCapableTreeStructure<LighterASTNode>.children(
    node: LighterASTNode,
): List<LighterASTNode> {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.filterNotNull().orEmpty()
}

/**
 * 默认参数限制检查器
 *
 * 对齐 C++ DiagKind::sema_cannot_have_default_param (Diags.cpp:414):
 * operator / foreign / open / abstract 函数不能有默认参数。
 */
object CfirDefaultParameterChecker : CfirSimpleFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        val hasDefaultParam = declaration.valueParameters.any { it.defaultValue != null }
        if (!hasDefaultParam) return

        val kind = when {
            declaration.status.isOperator -> "operator overloading"
            declaration.status.isForeign -> "foreign"
            declaration.status.isOpen -> "'open'"
            declaration.status.isAbstract -> "abstract"
            else -> return
        }
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.CANNOT_HAVE_DEFAULT_PARAM,
            a = kind,
        )
    }
}
