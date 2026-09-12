package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.isInsideFailedArgumentMapping
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

/**
 * callee 或参数形态已无效时结束未分析实参的状态，对位官方 setFuncArgsInvalidTy。
 *
 * 不进行名字查找、类型推断或正文解析。显式类型引用应已由类型解析阶段处理；
 * 其诊断和已知类型保留。有已知错误类型时再传播到剩余隐式类型及表达式；无论是否
 * 有该类型，未分析 lambda 都记录参数映射已失败，避免追加“缺少类型注解”等派生错误。
 */
internal object CfirUnanalyzedCallArgumentTransformer : CfirTransformer<ConeErrorType?>() {
    override fun <E : CfirElement> transformElement(element: E, data: ConeErrorType?): E {
        if (element is CfirAnonymousFunction && element.typeRef.coneTypeOrNull == null) {
            element.isInsideFailedArgumentMapping = true
        }
        element.transformChildren(this, data)
        if (data != null && element is CfirExpression && element.coneTypeOrNull == null) {
            element.replaceConeTypeOrNull(data)
        }
        return element
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: ConeErrorType?): CfirTypeRef {
        if (data == null) return implicitTypeRef
        return buildErrorTypeRef {
            source = implicitTypeRef.source
            coneType = data
            diagnostic = data.diagnostic
            delegatedTypeRef = implicitTypeRef
        }
    }
}
