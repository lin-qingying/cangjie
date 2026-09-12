package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

/**
 * callee 已无效时结束未分析实参的类型状态，对位官方 setFuncArgsInvalidTy。
 *
 * 不进行名字查找、类型推断或正文解析。显式类型引用应已由类型解析阶段处理；
 * 其诊断和已知类型保留，剩余隐式类型及表达式继承 callee 错误，避免检查器误以为
 * lambda 已经尝试过参数推断而追加“缺少类型注解”等派生错误。
 */
internal object CfirUnanalyzedCallArgumentTransformer : CfirTransformer<ConeErrorType>() {
    override fun <E : CfirElement> transformElement(element: E, data: ConeErrorType): E {
        element.transformChildren(this, data)
        if (element is CfirExpression && element.coneTypeOrNull == null) {
            element.replaceConeTypeOrNull(data)
        }
        return element
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: ConeErrorType): CfirTypeRef =
        buildErrorTypeRef {
            source = implicitTypeRef.source
            coneType = data
            diagnostic = data.diagnostic
            delegatedTypeRef = implicitTypeRef
        }
}
