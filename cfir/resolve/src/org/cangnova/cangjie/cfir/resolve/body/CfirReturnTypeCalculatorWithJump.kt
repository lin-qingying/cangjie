package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 带 designated resolve“跳转”能力的返回类型计算器。
 * 它服务于 IMPLICIT_TYPES 阶段：当目标声明尚未算出返回类型时，
 * 会临时跳到该声明执行 designated resolve，再读取其返回类型。
 * 通过 [implicitBodyResolveComputationSession] 的状态机避免递归依赖。
 * 参考 K2 `ReturnTypeCalculatorWithJump`。
 */
class CfirReturnTypeCalculatorWithJump(
    private val session: org.cangnova.cangjie.cfir.session.CfirSession,
    private val scopeSession: ScopeSession,
    private val implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
) : CfirReturnTypeCalculator {

    override fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangJieType? {
        // 1. 已有显式解析类型，直接返回
        val typeRef = extractReturnTypeRef(declaration) ?: return null
        if (typeRef is CfirResolvedTypeRef) {
            return typeRef.coneType
        }

        // 2. 不是隐式类型，无法继续推断
        if (typeRef !is CfirImplicitTypeRef) {
            return null
        }

        // 3. 需要推断，先检查当前计算状态
        val symbol = extractSymbol(declaration) ?: return null
        return when (val status = implicitBodyResolveComputationSession.getStatus(symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> {
                status.resolvedType
            }
            is CfirImplicitBodyResolveComputationStatus.Computing -> {
                // 递归依赖时返回错误类型
                ConeErrorType("recursive implicit type")
            }
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> {
                // 瑙﹀彂 designated resolve
                resolveDesignated(declaration)
            }
        }
    }

    /** 触发 designated resolve，并计算目标声明的返回类型。 */
    private fun resolveDesignated(declaration: CfirCallableDeclaration): ConeCangJieType {
        val symbol = extractSymbol(declaration) ?: return ConeErrorType("no symbol for declaration")

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
                    org.cangnova.cangjie.cfir.resolve.CfirResolutionMode.ContextIndependent,
                )
            }
            // 返回转换后的声明
            declaration
        }
        return extractResolvedType(result) ?: ConeErrorType("failed to resolve implicit type")
    }

    private fun extractReturnTypeRef(declaration: CfirCallableDeclaration): CfirTypeRef? = when (declaration) {
        is CfirFunction -> declaration.returnTypeRef
        is CfirProperty -> declaration.returnTypeRef
        is CfirFieldVariable -> declaration.returnTypeRef
        is CfirPatternVariable -> declaration.returnTypeRef
        else -> null
    }

    private fun extractSymbol(declaration: CfirCallableDeclaration) =
        declaration.symbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>

    private fun extractResolvedType(declaration: CfirCallableDeclaration): ConeCangJieType? {
        val typeRef = extractReturnTypeRef(declaration)
        return if (typeRef is CfirResolvedTypeRef) typeRef.coneType else null
    }

    /** 查找声明所在文件。 */
    private fun findContainingFile(declaration: CfirCallableDeclaration): org.cangnova.cangjie.cfir.declarations.CfirFile? {
        // Phase 3 暂不实现完整的 file -> class -> declaration 路径查找
        return null
    }
}

