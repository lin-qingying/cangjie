package org.cangnova.cangjie.cfir.resolve.substitution


import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeRigidType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeConstructorMarker
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeSubstitutorMarker

abstract class ConeSubstitutor : TypeSubstitutorMarker {
    open fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = substituteOrNull(type) ?: type

    abstract fun substituteOrNull(type: ConeCangJieType): ConeCangJieType?

    abstract fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection?

    object Empty : ConeSubstitutor() {
        override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = type

        override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? = null

        override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? = null

        override fun toString(): String = "Empty"
    }
}
