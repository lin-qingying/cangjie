package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey

/** 非 FFI 内置注解的声明语义；与宏 slot/provenance 注册表分离。 */
public data class CfirDeclarationAnnotationInfo(
    val isAnnotation: Boolean = false,
    val annotationTargets: Set<CangjieAnnotationTarget> = CangjieAnnotationTarget.entries.toSet(),
    val isIntrinsic: Boolean = false,
    val isConstSafe: Boolean = false,
    val isMockSupported: Boolean = false,
    val attributes: List<String> = emptyList(),
    val overflowStrategy: CangjieOverflowStrategy? = null,
    /** CJO ClassInfo.runtimeVisible for a serialized annotation declaration. */
    val runtimeVisible: Boolean = false,
)

private object CfirDeclarationAnnotationInfoKey : CfirDeclarationDataKey()

/** annotation owner 发布的不可变语义快照，复制声明 attributes 时完整保留。 */
public var CfirDeclaration.annotationInfo: CfirDeclarationAnnotationInfo? by
    CfirDeclarationDataRegistry.data(CfirDeclarationAnnotationInfoKey)
