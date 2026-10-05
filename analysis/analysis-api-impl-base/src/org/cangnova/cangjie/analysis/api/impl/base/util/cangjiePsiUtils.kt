package org.cangnova.cangjie.analysis.api.impl.base.util

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement

/**
 * 非局部命名函数对应的 callableId。
 */
val CjNamedFunction.callableId: CallableId?
    get() = if (isLocal) null else callableIdForName(nameAsSafeName)

/**
 * 基于声明所在类型或包为指定 callable 名称构造 callableId。
 *
 * 对齐 Kotlin `KtDeclaration.callableIdForName`：容器有 `ClassId` 时用 `CallableId(ClassId, name)`，
 * 否则回退到包级 `CallableId(packageFqName, name)`。
 *
 * 仓颉偏差：`extend` 是 `CjTypeStatement` 但没有 `ClassId`，而 CFIR 侧 extend 成员的 callableId
 * 同样是包级（raw builder 的 `callableIdFor` 只对 class-like 容器写 `ClassId`）。
 * 因此这里对“无 ClassId 的容器”统一回退包级，而不是像 Kotlin 那样返回 `null`
 * （Kotlin 的 script 成员没有 `containingClassOrObject`，自然落到包级分支）。
 */
fun CjDeclaration.callableIdForName(callableName: Name): CallableId? {
    val containingClassId = containingTypeStatement?.getClassId()
    if (containingClassId != null) {
        return CallableId(classId = containingClassId, callableName = callableName)
    }

    return CallableId(packageName = containingCjFile.packageFqName, callableName = callableName)
}
