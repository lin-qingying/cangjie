package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey

/** `@Annotation.target` 元数据的发布状态；失败状态不能被当作合法目标集合消费。 */
public enum class CfirAnnotationTargetResolutionStatus {
    /** 当前声明不是注解声明。 */
    NOT_ANNOTATION,

    /** 注解没有显式 target，官方语义是允许全部目标。 */
    ALL_TARGETS,

    /** 显式 target 已完整解析为 AnnotationKind 枚举常量。 */
    RESOLVED,

    /** 显式 target 存在非法、未解析或错误类型元素。 */
    INVALID,
}

/** 非 FFI 内置注解的声明语义；与宏 slot/provenance 注册表分离。 */
public data class CfirDeclarationAnnotationInfo(
    val isAnnotation: Boolean = false,
    val annotationTargets: Set<CangjieAnnotationTarget> = CangjieAnnotationTarget.entries.toSet(),
    val annotationTargetResolutionStatus: CfirAnnotationTargetResolutionStatus =
        if (isAnnotation) CfirAnnotationTargetResolutionStatus.RESOLVED
        else CfirAnnotationTargetResolutionStatus.NOT_ANNOTATION,
    val isIntrinsic: Boolean = false,
    val isConstSafe: Boolean = false,
    val isMockSupported: Boolean = false,
    val attributes: List<String> = emptyList(),
    val overflowStrategy: CangjieOverflowStrategy? = null,
    /** CJO ClassInfo.runtimeVisible for a serialized annotation declaration. */
    val runtimeVisible: Boolean = false,
)

/** [CfirDeclaration.annotationInfo] 扩展属性使用的声明数据键。 */
private object CfirDeclarationAnnotationInfoKey : CfirDeclarationDataKey()

/** annotation owner 发布的不可变语义快照，复制声明 attributes 时完整保留。 */
public var CfirDeclaration.annotationInfo: CfirDeclarationAnnotationInfo? by
    CfirDeclarationDataRegistry.data(CfirDeclarationAnnotationInfoKey)
