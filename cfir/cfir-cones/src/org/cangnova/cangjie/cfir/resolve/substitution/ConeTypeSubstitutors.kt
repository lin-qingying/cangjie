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

/**
 * Cone 类型替换器抽象。
 *
 * 替换器负责把类型变量、类型参数或临时构造器替换成解析后的 Cone 类型，
 * 同时允许调用方按类型实参位置替换投影。
 */
abstract class ConeSubstitutor : TypeSubstitutorMarker {
    /**
     * 替换 [type]；没有替换结果时返回原类型。
     */
    open fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = substituteOrNull(type) ?: type

    /**
     * 替换 [type]；没有替换结果时返回 `null`。
     */
    abstract fun substituteOrNull(type: ConeCangJieType): ConeCangJieType?

    /**
     * 替换类型实参 [projection]。
     *
     * [index] 是该投影在外层实参列表中的位置。
     */
    abstract fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection?

    /**
     * 不执行任何替换的空替换器。
     */
    object Empty : ConeSubstitutor() {
        /**
         * 空替换器直接返回原类型。
         */
        override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = type

        /**
         * 空替换器没有可空替换结果。
         */
        override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? = null

        /**
         * 空替换器不替换类型实参。
         */
        override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? = null

        /**
         * 空替换器的调试名称。
         */
        override fun toString(): String = "Empty"
    }
}
