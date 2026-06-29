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
 */
fun CjDeclaration.callableIdForName(callableName: Name): CallableId? {
    val containingTypeStatement = containingTypeStatement
    if (containingTypeStatement!= null) {
        return containingTypeStatement.getClassId()?.let { classId ->
            CallableId(classId = classId, callableName = callableName)
        }
    }

    return CallableId(packageName = containingCjFile.packageFqName, callableName = callableName)
}
