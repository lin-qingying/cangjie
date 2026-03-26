package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

internal fun classIdForClassNesting(
    packageFqName: FqName,
    classNesting: List<Name>,
): ClassId? {
    if (classNesting.isEmpty()) return null
    val relativeClassName = classNesting
        .drop(1)
        .fold(FqName.topLevel(classNesting.first())) { current, nestedName ->
            current.child(nestedName)
        }
    return ClassId(packageFqName, relativeClassName)
}

