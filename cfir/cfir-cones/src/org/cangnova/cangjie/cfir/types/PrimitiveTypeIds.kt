package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

val PrimitiveTypeKind.classId: ClassId
    get() = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier(typeName))

fun ClassId.toPrimitiveTypeKindOrNull(): PrimitiveTypeKind? {
    if (packageFqName != StandardNames.BASIC_PACKAGE_FQ_NAME) return null
    return PrimitiveTypeKind.entries.firstOrNull { it.typeName == shortClassName.asString() }
}

val PrimitiveTypeKind.isExposedBuiltinClassifier: Boolean
    get() = this != PrimitiveTypeKind.IDEAL_INT && this != PrimitiveTypeKind.IDEAL_FLOAT

val ConeCangJieType.classIdOrPrimitiveClassId: ClassId?
    get() = when (this) {
        is ConePrimitiveType -> kind.classId
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    }
