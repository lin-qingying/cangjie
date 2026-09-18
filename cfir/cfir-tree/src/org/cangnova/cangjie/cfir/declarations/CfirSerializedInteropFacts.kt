package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.annotations.CangjieCallingConvention
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.session.CfirInteropTarget

/**
 * Binary-only source facts which are not represented by [CfirAnnotation].
 *
 * The CJO format stores some interop information in declaration attributes or
 * `FuncInfo` rather than in `Anno` entries.  Keeping those facts on the
 * declaration lets the common interop producer reconstruct the same snapshot
 * as source CFIR without making the deserializer or an Analysis API consumer
 * guess from a short annotation name.
 */
public data class CfirSerializedInteropFacts(
    /** `@C` was materialized as an explicit C ABI declaration attribute. */
    val hasExplicitC: Boolean = false,
    /** A serialized `STD_CALL` declaration attribute was present. */
    val callingConvention: CangjieCallingConvention? = null,
    /** `FuncInfo.overflowPolicy` restored from the official CJO declaration info. */
    val overflowStrategy: CangjieOverflowStrategy? = null,
    /** `Attribute::INTRINSIC` restored from the official CJO declaration. */
    val isIntrinsic: Boolean = false,
    /** `FuncInfo.isFastNative` was set by the official AST writer. */
    val isFastNative: Boolean = false,
    /** Official AST attributes materialized from Java FFI and CJMapping. */
    val isJavaMirror: Boolean = false,
    val isJavaMirrorSubtype: Boolean = false,
    val hasJavaDefault: Boolean = false,
    val isJavaMirrorSyntheticWrapper: Boolean = false,
    val isJavaApplication: Boolean = false,
    val isJavaExtension: Boolean = false,
    val isJavaCjMapping: Boolean = false,
    val isJavaInterfaceForward: Boolean = false,
    val isJavaInterfaceDefault: Boolean = false,
    /** Official AST attributes materialized from Objective-C FFI and CJMapping. */
    val isObjCMirror: Boolean = false,
    val isObjCMirrorSubtype: Boolean = false,
    val isObjCInit: Boolean = false,
    val isObjCOptional: Boolean = false,
    val isObjCMirrorSyntheticWrapper: Boolean = false,
    val isObjCCjMapping: Boolean = false,
    val isObjCInterfaceForward: Boolean = false,
    /** Official `AnnoKind_TestRegistration` is restored as this attribute by ASTLoader. */
    val attributeNames: List<String> = emptyList(),
    /** `null` means this CJO declaration has no serialized CJMapping attribute. */
    val cjmpTarget: CfirInteropTarget? = null,
)

/** [CfirDeclaration.serializedInteropFacts] 扩展属性使用的声明数据键。 */
private object CfirSerializedInteropFactsKey : CfirDeclarationDataKey()

/** Serialized interop facts attached by the CJO declaration owner. */
public var CfirDeclaration.serializedInteropFacts: CfirSerializedInteropFacts? by
    CfirDeclarationDataRegistry.data(CfirSerializedInteropFactsKey)

/** 原始 CJO Decl.attributes 位图快照；保留未被 status/interop 投影消费的官方位。 */
public data class CfirSerializedDeclarationAttributes(
    val words: List<ULong>,
)

private object CfirSerializedDeclarationAttributesKey : CfirDeclarationDataKey()

/** CJO reader 在声明发布时写入的完整 attributes snapshot。 */
public var CfirDeclaration.serializedDeclarationAttributes: CfirSerializedDeclarationAttributes? by
    CfirDeclarationDataRegistry.data(CfirSerializedDeclarationAttributesKey)
