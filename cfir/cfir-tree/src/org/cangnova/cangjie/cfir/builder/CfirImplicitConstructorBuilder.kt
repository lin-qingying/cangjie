package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.builder.buildPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.builder.CfirFunctionBuilder
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Native CreateDefaultCtor的完整raw节点工厂。PSI、LT及宏后的声明准备共用模型拥有者，
 * 避免providers反向依赖parser；资格由声明准备owner决定，不在工厂按名字或source猜测。
 * 空Block是真实默认构造正文，CFG及返回Type由正常TYPES/STATUS/BODY阶段产生。
 */
fun buildImplicitPrimaryConstructor(
    symbol: CfirConstructorSymbol,
    source: CjSourceElement?,
    moduleData: CfirModuleData,
    isLocal: Boolean,
    dispatchReceiverType: ConeSimpleCangJieType?,
    typeParameters: List<CfirTypeParameterRef>,
): CfirConstructor = buildPrimaryConstructor {
    this.symbol = symbol
    this.typeParameters.addAll(typeParameters)
    initializeImplicitConstructor(source, moduleData, isLocal, dispatchReceiverType, CfirDeclarationOrigin.Synthetic.ImplicitConstructor)
}.also { it.exportFacts = it.exportFacts.copy(isCompilerAdded = true, isImplicitlyAdded = true) }

/** TYPES/CJMP 后置补入使用普通实例 init；不是带主构造参数语法的 primary constructor。 */
fun buildImplicitDefaultConstructor(
    symbol: CfirConstructorSymbol,
    source: CjSourceElement?,
    moduleData: CfirModuleData,
    isLocal: Boolean,
    dispatchReceiverType: ConeSimpleCangJieType,
    typeParameters: List<CfirTypeParameterRef>,
): CfirConstructor = buildConstructor {
    this.symbol = symbol
    this.typeParameters.addAll(typeParameters)
    initializeImplicitConstructor(source, moduleData, isLocal, dispatchReceiverType, CfirDeclarationOrigin.ImplicitDefault)
}.also { it.exportFacts = it.exportFacts.copy(isCompilerAdded = true, isImplicitlyAdded = true) }

/** 两种生成入口共享完整 skeleton；source/body/状态与阶段分别初始化。 */
private fun CfirFunctionBuilder.initializeImplicitConstructor(
    source: CjSourceElement?,
    moduleData: CfirModuleData,
    isLocal: Boolean,
    dispatchReceiverType: ConeSimpleCangJieType?,
    origin: CfirDeclarationOrigin,
) {
    this.source = source
    this.moduleData = moduleData
    this.isLocal = isLocal
    this.dispatchReceiverType = dispatchReceiverType
    resolvePhase = CfirResolvePhase.RAW_CFIR
    this.origin = origin
    attributes = CfirDeclarationAttributes.EMPTY
    status = CfirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
    returnTypeRef = buildImplicitTypeRef()
    body = buildBlock { this.source = source }
}
