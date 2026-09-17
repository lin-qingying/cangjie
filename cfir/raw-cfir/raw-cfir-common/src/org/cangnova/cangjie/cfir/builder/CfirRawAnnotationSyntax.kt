package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.expressions.builder.CfirAnnotationCallBuilder

/** PSI 与 LightTree 共享的 raw 注解语法规范化；身份和目标由后续 resolver 发布。 */
public fun CfirAnnotationCallBuilder.initializeRawAnnotationSyntax(
    compileTimeVisible: Boolean,
    sourceName: String? = null,
    sourceModuleName: String,
) {
    isCompileTimeVisible = compileTimeVisible
    forcedCustom = compileTimeVisible
    annotationSourceName = sourceName
    this.sourceModuleName = sourceModuleName
    annotationKind = null
    annotationOrigin = null
    annotationIdentity = null
}
