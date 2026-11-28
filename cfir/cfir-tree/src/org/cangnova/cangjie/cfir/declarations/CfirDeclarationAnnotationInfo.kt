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
    /** `@EnsurePreparedToMock` belongs to an annotation lambda, not its target. */
    val isEnsurePreparedToMock: Boolean = false,
    /** Official package/declaration mock capability (`MOCK_SUPPORTED`). */
    val isMockSupported: Boolean = false,
    val attributes: List<String> = emptyList(),
    val overflowStrategy: CangjieOverflowStrategy? = null,
    /** CJO ClassInfo.runtimeVisible for a serialized annotation declaration. */
    val runtimeVisible: Boolean = false,
)

/** [CfirDeclaration.annotationInfo] 扩展属性使用的声明数据键。 */
internal object CfirDeclarationAnnotationInfoKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy get() = org.cangnova.cangjie.cfir.CfirRawDeclarationDataPolicies.annotationInfo
}
internal object CfirOwnedDeclarationAnnotationInfoKey : CfirDeclarationDataKey()
internal object CfirSerializedAnnotationHeaderRegisteredKey : CfirDeclarationDataKey()

/** 完成产品和拥有复制共用字段 owner 的值冻结；raw 与 derived 分别保存，不能互相覆盖。 */
internal fun CfirDeclarationAnnotationInfo.frozenCopy(): CfirDeclarationAnnotationInfo = copy(
    annotationTargets = java.util.Collections.unmodifiableSet(LinkedHashSet(annotationTargets)),
    attributes = java.util.Collections.unmodifiableList(ArrayList(attributes)),
)

/** annotation owner 发布的不可变语义快照，复制声明 attributes 时完整保留。 */
private var CfirDeclaration.rawSerializedAnnotationHeaderInfo: CfirDeclarationAnnotationInfo? by
    CfirDeclarationDataRegistry.data(CfirDeclarationAnnotationInfoKey)
private var CfirDeclaration.serializedAnnotationHeaderRegistered: Boolean? by CfirDeclarationDataRegistry.data(CfirSerializedAnnotationHeaderRegisteredKey)
private var CfirDeclaration.ownedDerivedAnnotationInfo: CfirDeclarationAnnotationInfo? by CfirDeclarationDataRegistry.data(CfirOwnedDeclarationAnnotationInfoKey)

/** 保存事实只登记一次，包含真正的null；后续getter/再绑定只能核对原事实，不覆盖。 */
public var CfirDeclaration.serializedAnnotationHeaderInfo: CfirDeclarationAnnotationInfo?
    get() = rawSerializedAnnotationHeaderInfo
    set(value) {
        if (serializedAnnotationHeaderRegistered == true) {
            check(rawSerializedAnnotationHeaderInfo == value) { "Serialized annotation header facts cannot be replaced" }
            return
        }
        rawSerializedAnnotationHeaderInfo = value
        serializedAnnotationHeaderRegistered = true
    }

/** Library语义结果和普通注解同代读取；未选header仅持有原保存事实，不冒充完成语义。 */
public var CfirDeclaration.annotationInfo: CfirDeclarationAnnotationInfo?
    get() = when (val selectedSource = contentSource) {
        org.cangnova.cangjie.cfir.CfirContentSource.Owned -> ownedDerivedAnnotationInfo
        is org.cangnova.cangjie.cfir.CfirContentSource.Published -> when (val content = selectedSource.snapshot().annotations(this)) {
            org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected -> serializedAnnotationHeaderInfo
            is org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Complete -> content.annotationInfo
        }
    }
    set(value) {
        check(contentSource === org.cangnova.cangjie.cfir.CfirContentSource.Owned) { "Published annotation metadata requires its package completion product" }
        ownedDerivedAnnotationInfo = value
    }
