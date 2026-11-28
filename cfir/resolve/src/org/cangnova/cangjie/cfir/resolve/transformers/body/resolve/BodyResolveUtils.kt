package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostic.ConePostponedInferenceDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.resolvedType

/** 表达式当前解析出的结果类型，写入时同步更新表达式的 cone type。 */
internal inline var CfirExpression.resultType: ConeCangJieType
    get() = resolvedType
    set(type) {
        replaceConeTypeOrNull(type)
    }

/**
 * 对位 Kotlin `FirBlock.writeResultType`。
 */
fun CfirBlock.writeResultType(session: CfirSession) {
    val resultExpression = statements.lastOrNull() as? CfirExpression
    resultType = if (resultExpression == null) {
        session.builtinTypes.unitType
    } else {
        resultExpression.coneTypeOrNull ?: ConeErrorType(ConePostponedInferenceDiagnostic)
    }
}

/**
 * 结构化 pending 是尚未完成的类型状态，不是已经确定的源码错误。
 * unreported 包装不改变这一身份；不能按 reason 文本判断。
 */
internal val ConeErrorType.isPostponedInference: Boolean
    get() {
        var currentDiagnostic = diagnostic
        while (currentDiagnostic is ConeUnreportedDuplicateDiagnostic) {
            currentDiagnostic = currentDiagnostic.original
        }
        return currentDiagnostic === ConePostponedInferenceDiagnostic
    }

/** 按原顺序保留首个真实错误；没有真实错误时保留实际首个 pending，绝不把它发布成成功结果。 */
internal fun Sequence<ConeErrorType?>.firstSourceErrorOrPending(): ConeErrorType? {
    var pending: ConeErrorType? = null
    for (error in this) {
        if (error == null) continue
        if (!error.isPostponedInference) return error
        if (pending == null) pending = error
    }
    return pending
}

/**
 * 查找 block 子树中第一个已经确定的源码错误；pending 不截断子树或后续语句。
 *
 * 官方 SynBlock 把任一无效语句传播到整个 block，不能只观察尾声明的 Unit。
 * 命名函数仅检查声明头，正文保留独立检查边界；lambda 仍检查自己的正文。
 * 无真实错误时返回原 pending，FULL writer 仍必须完成该义务。
 */
internal fun CfirExpression.firstErrorTypeOrNull(): ConeErrorType? {
    var result: ConeErrorType? = null
    var pending: ConeErrorType? = null

    fun record(error: ConeErrorType?) {
        if (error == null) return
        if (error.isPostponedInference) {
            if (pending == null) pending = error
        } else {
            result = error
        }
    }

    fun recordElement(element: CfirElement) {
        if (element is CfirExpression) record(element.coneTypeOrNull as? ConeErrorType)
        if (result == null && element is CfirDiagnosticHolder) {
            record(ConeErrorType(ConeUnreportedDuplicateDiagnostic(element.diagnostic)))
        }
    }

    recordElement(this)
    if (result != null) return result
    acceptChildren(object : CfirVisitorVoid() {
        private var nestedFunctionBody: CfirBlock? = null

        override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
            if (result != null) return
            val previousBody = nestedFunctionBody
            nestedFunctionBody = namedFunction.body
            try {
                namedFunction.acceptChildren(this, null)
            } finally {
                nestedFunctionBody = previousBody
            }
        }

        override fun visitElement(element: CfirElement) {
            if (result != null || element === nestedFunctionBody) return
            recordElement(element)
            if (result == null) element.acceptChildren(this, null)
        }
    }, null)
    return result ?: pending
}
