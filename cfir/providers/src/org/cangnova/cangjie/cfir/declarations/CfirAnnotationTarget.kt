package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.annotations.CangjieAnnotationTarget
import org.cangnova.cangjie.cfir.containingExtend

/** 官方 AnnotationChecker::GetTarget；成员归属来自声明符号而非 isLocal 的反值。 */
public fun CfirDeclaration.annotationTargetFor(): CangjieAnnotationTarget? = when (this) {
    is CfirValueParameter -> CangjieAnnotationTarget.PARAMETER
    is CfirProperty -> CangjieAnnotationTarget.MEMBER_PROPERTY
    is CfirEnumConstructor -> CangjieAnnotationTarget.ENUM_CONSTRUCTOR
    is CfirConstructor -> CangjieAnnotationTarget.INIT
    is CfirFunction -> if (isTopLevelCallable()) {
        CangjieAnnotationTarget.GLOBAL_FUNCTION
    } else CangjieAnnotationTarget.MEMBER_FUNCTION
    is CfirVariable -> if (!isLocal && symbol.callableId.classId == null) {
        CangjieAnnotationTarget.GLOBAL_VARIABLE
    } else CangjieAnnotationTarget.MEMBER_VARIABLE
    is CfirExtend -> CangjieAnnotationTarget.EXTEND
    is CfirClassLikeDeclaration -> CangjieAnnotationTarget.TYPE
    else -> null
}

/**
 * `isLocal == false` 不能区分顶层函数和 class/extend 成员。
 *
 * enum 的 dispatch receiver 是合法的成员归属，但不一定实现为
 * `ConeClassLikeType`，所以这里必须检查原始 dispatch receiver 是否存在，不能只依赖
 * lookup tag 的派生结果。
 */
private fun CfirFunction.isTopLevelCallable(): Boolean =
    !isLocal &&
        dispatchReceiverType == null &&
        containingExtend == null
