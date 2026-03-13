package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 跳转式返回类型计算器。
 *
 * 用于 IMPLICIT_TYPES 阶段，当遇到尚未计算返回类型的声明时，
 * 会"跳转"到该声明进行 designated resolve，计算其返回类型。
 *
 * 递归保护：通过 [implicitBodyResolveComputationSession] 的状态机检测递归，
 * Computing 状态下返回 [ConeErrorType]。
 *
 * 参考 K2 ReturnTypeCalculatorWithJump。
 */
class CfirReturnTypeCalculatorWithJump(
    private val session: org.cangnova.cangjie.cfir.session.CfirSession,
    private val scopeSession: CfirScopeSession,
    private val implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
) : CfirReturnTypeCalculator {

    override fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangjieType? {
        // 1. 已有显式解析类型 → 直接返回
        val typeRef = extractReturnTypeRef(declaration) ?: return null
        if (typeRef is CfirResolvedTypeRef) {
            return typeRef.coneType
        }

        // 2. 非隐式类型 → 无法推断
        if (typeRef !is CfirImplicitTypeRef) {
            return null
        }

        // 3. 需要推断 — 检查计算状态
        val symbol = extractSymbol(declaration) ?: return null
        return when (val status = implicitBodyResolveComputationSession.getStatus(symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> {
                status.resolvedType
            }
            is CfirImplicitBodyResolveComputationStatus.Computing -> {
                // 递归依赖 → 返回错误类型
                ConeErrorType("recursive implicit type")
            }
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> {
                // 触发 designated resolve
                resolveDesignated(declaration)
            }
        }
    }

    /** 触发 designated resolve 以计算声明的返回类型 */
    private fun resolveDesignated(declaration: CfirCallableDeclaration): ConeCangjieType {
        val symbol = extractSymbol(declaration) ?: return ConeErrorType("no symbol for declaration")

        val result = implicitBodyResolveComputationSession.compute(symbol) {
            // 创建 designated transformer 解析此声明
            val designatedTransformer = CfirDesignatedBodyResolveTransformer(
                designation = declaration,
                session = session,
                scopeSession = scopeSession,
                implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
                returnTypeCalculator = this,
            )
            // 找到声明所在的文件并变换
            val file = findContainingFile(declaration)
            if (file != null) {
                designatedTransformer.transformFile(
                    file,
                    org.cangnova.cangjie.cfir.resolve.CfirResolutionMode.ContextIndependent,
                )
            }
            // 返回变换后的声明
            declaration
        }
        return extractResolvedType(result) ?: ConeErrorType("failed to resolve implicit type")
    }

    private fun extractReturnTypeRef(declaration: CfirCallableDeclaration): CfirTypeRef? = when (declaration) {
        is CfirFunction -> declaration.returnTypeRef
        is CfirProperty -> declaration.returnTypeRef
        is CfirVariable -> declaration.returnTypeRef
        else -> null
    }

    private fun extractSymbol(declaration: CfirCallableDeclaration) =
        declaration.symbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>

    private fun extractResolvedType(declaration: CfirCallableDeclaration): ConeCangjieType? {
        val typeRef = extractReturnTypeRef(declaration)
        return if (typeRef is CfirResolvedTypeRef) typeRef.coneType else null
    }

    /** 查找声明所在的文件（向上遍历符号表） */
    private fun findContainingFile(declaration: CfirCallableDeclaration): org.cangnova.cangjie.cfir.declarations.CfirFile? {
        // Phase 3 简化：designated resolve 直接在 declaration 上操作
        // 完整实现需要 file → (class)? → declaration 路径
        return null
    }
}
