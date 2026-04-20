package org.cangnova.cangjie.analysis.api.impl.base.util

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement

val CjNamedFunction.callableId: CallableId?
    get() = if (isLocal) null else callableIdForName(nameAsSafeName)

fun CjDeclaration.callableIdForName(callableName: Name): CallableId? {
    val containingTypeStatement = containingTypeStatement
    if (containingTypeStatement!= null) {
        return containingTypeStatement.getClassId()?.let { classId ->
            CallableId(classId = classId, callableName = callableName)
        }
    }

    return CallableId(packageName = containingCjFile.packageFqName, callableName = callableName)
}