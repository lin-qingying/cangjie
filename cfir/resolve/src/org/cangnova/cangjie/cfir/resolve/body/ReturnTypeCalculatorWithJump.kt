package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.scopes.deferredCallableCopyReturnType
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef

/**
 * 带 designated resolve“跳转”能力的返回类型计算器。
 * 它服务于 IMPLICIT_TYPES 阶段：当目标声明尚未算出返回类型时，
 * 会临时跳到该声明执行 designated resolve，再读取其返回类型。
 * 通过 [implicitBodyResolveComputationSession] 的状态机避免递归依赖。
 * 参考 K2 `ReturnTypeCalculatorWithJump`。
 */
class ReturnTypeCalculatorWithJump(
    private val session: CfirSession,
    private val scopeSession: ScopeSession,
    private val implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
) : ReturnTypeCalculator() {
    override val callableCopyTypeCalculator: CallableCopyTypeCalculator = CallableCopyTypeCalculatorWithJump()

    override fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? {
        if (declaration.isLocal) {
            return ReturnTypeCalculatorForFullBodyResolve.Default.tryCalculateReturnType(declaration)
        }

        if (declaration is CfirValueParameter && declaration.returnTypeRef is CfirImplicitTypeRef) {
            declaration.replaceReturnTypeRef(
                buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported: implicit value parameter type",
                        DiagnosticKind.InferenceError,
                    )
                }
            )
        }

        // 1. 已有显式解析类型，直接返回
        val typeRef = extractReturnTypeRef(declaration)
        if (typeRef is CfirResolvedTypeRef) {
            return typeRef
        }

        declaration.attributes.deferredCallableCopyReturnType?.let {
            (callableCopyTypeCalculator.computeReturnType(declaration) as? CfirResolvedTypeRef)?.let { resolvedTypeRef ->
                return resolvedTypeRef
            }
        }

        // 2. 不是隐式类型，无法继续推断
        if (typeRef !is CfirImplicitTypeRef) {
            return null
        }

        // 3. 需要推断，先检查当前计算状态
        val symbol = extractSymbol(declaration) ?: return null
        return when (val status = implicitBodyResolveComputationSession.getStatus(symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> {
                extractResolvedTypeRef(status.transformedDeclaration)
                    ?: resolvedTypeRefFromType(typeRef, status.resolvedType)
            }
            is CfirImplicitBodyResolveComputationStatus.Computing -> {
                // 递归依赖时返回错误类型
                resolvedTypeRefFromType(typeRef, ConeErrorType(ConeSimpleDiagnostic("recursive implicit type")))
            }
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> {
                // 瑙﹀彂 designated resolve
                resolveDesignated(declaration)
            }
        }
    }

    /** 触发 designated resolve，并计算目标声明的返回类型。 */
    private fun resolveDesignated(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        val typeRef = extractReturnTypeRef(declaration)
        val symbol = extractSymbol(declaration)
            ?: return resolvedTypeRefFromType(typeRef, ConeErrorType(ConeSimpleDiagnostic("no symbol for declaration")))

        val result = implicitBodyResolveComputationSession.compute(symbol) {
            // 鍒涘缓 designated transformer 瑙ｆ瀽姝ゅ０鏄?
            val designatedTransformer = CfirDesignatedBodyResolveTransformer(
                designation = declaration,
                session = session,
                scopeSession = scopeSession,
                implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
                returnTypeCalculator = this,
            )
            // 找到声明所在文件并执行转换
            val file = findContainingFile(declaration)
            if (file != null) {
                designatedTransformer.transformFile(
                    file,
                    ResolutionMode.ContextIndependent,
                )
            }
            // 返回转换后的声明
            declaration
        }
        return extractResolvedTypeRef(result)
            ?: resolvedTypeRefFromType(typeRef, ConeErrorType(ConeSimpleDiagnostic("failed to resolve implicit type")))
    }

    private fun extractReturnTypeRef(declaration: CfirCallableDeclaration): CfirTypeRef = declaration.returnTypeRef

    private fun extractSymbol(declaration: CfirCallableDeclaration) =
        declaration.symbol

    private fun extractResolvedTypeRef(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? {
        val typeRef = extractReturnTypeRef(declaration)
        return typeRef as? CfirResolvedTypeRef
    }

    private fun computeReturnTypeRef(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? {
        return extractResolvedTypeRef(declaration) ?: resolveDesignated(declaration)
    }

    private fun resolvedTypeRefFromType(prototype: CfirTypeRef?, type: ConeCangJieType): CfirResolvedTypeRef {
        return buildResolvedTypeRef {
            source = prototype?.source
            coneType = type
            delegatedTypeRef = prototype
        }
    }

    /** 查找声明所在文件。 */
    private fun findContainingFile(declaration: CfirCallableDeclaration): CfirFile? {
        return session.symbolProvider.getContainingFile(declaration.symbol)
    }

    private inner class CallableCopyTypeCalculatorWithJump : CallableCopyTypeCalculator.DeferredCallableCopyTypeCalculator() {
        override fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef? {
            return this@ReturnTypeCalculatorWithJump.computeReturnTypeRef(this)
        }
    }
}
