package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.name.ClassId

interface CaTypeCreator : CaLifetimeOwner {
    /**
     * Builds a class type with the given class ID.
     *
     * A generic class type can be built by providing type arguments using the [init] block.
     * The caller is supposed to provide the correct number of type arguments for the class.
     *
     * For Kotlin built-in types, consider using the overload that accepts a [CaClassLikeSymbol] instead:
     * `buildClassType(builtinTypes.string)`.
     *
     *  #### Example
     *
     * ```kotlin
     * buildClassType(ClassId.fromString("kotlin/collections/List")) {
     *     argument(buildClassType(ClassId.fromString("kotlin/String")))
     * }
     * ```
     */
    public fun buildClassType(classId: ClassId, init: CaClassTypeBuilder.() -> Unit = {}): CaType

    /**
     * Builds a class type with the given class symbol.
     *
     * A generic class type can be built by providing type arguments using the [init] block.
     * The caller is supposed to provide the correct number of type arguments for the class.
     *
     * #### Example
     *
     * ```kotlin
     * buildClassType(builtinTypes.string)
     * ```
     */
    public fun buildClassType(symbol: CaClassLikeSymbol, init: CaClassTypeBuilder.() -> Unit = {}): CaType

    /**
     * Builds a [CaTypeParameterType] with the given type parameter symbol.
     */
    public fun buildTypeParameterType(symbol: CaTypeParameterSymbol, init: CaTypeParameterTypeBuilder.() -> Unit = {}): CaTypeParameterType

    fun buildFunctionType(
        parameterTypes: List<CaType>,
        returnType: CaType,
        isCFunction: Boolean = false,
        isClosureType: Boolean = false,
        hasVariableLengthArgument: Boolean = false,
    ): CaFunctionType

    fun buildTupleType(
        elementTypes: List<CaType>,
    ): CaTupleType

    fun buildIntersectionType(
        conjuncts: List<CaType>,
    ): CaIntersectionType

    fun buildUnionType(
        alternatives: Collection<CaType>,
    ): CaUnionType
}

@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaTypeBuilder : CaLifetimeOwner


/**
 * A builder for class types.
 *
 * @see CaTypeCreator.buildClassType
 */
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaClassTypeBuilder : CaTypeBuilder {



    public val arguments: List<CaTypeProjection>

    /**
     * Adds a type projection as an [argument] to the class type.
     */
    public fun argument(argument: CaTypeProjection)

    /**
     * Adds a [type] argument to the class type, with the given [variance].
     */
    public fun argument(type: CaType )
}

/**
 * A builder for type parameter types.
 *
 * @see CaTypeCreator.buildTypeParameterType
 */
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaTypeParameterTypeBuilder : CaTypeBuilder {

}

