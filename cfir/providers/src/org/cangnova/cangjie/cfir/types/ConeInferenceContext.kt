package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.type.model.AnnotationMarker
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.CaptureStatus
import org.cangnova.cangjie.type.model.CapturedTypeMarker
import org.cangnova.cangjie.type.model.RigidTypeMarker
import org.cangnova.cangjie.type.model.SimpleTypeMarker
import org.cangnova.cangjie.type.model.StubTypeMarker
import org.cangnova.cangjie.type.model.TypeArgumentMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeParameterMarker
import org.cangnova.cangjie.type.model.TypeSubstitutorMarker
import org.cangnova.cangjie.type.model.TypeSystemInferenceExtensionContext
import org.cangnova.cangjie.type.model.TypeVariableMarker
import org.cangnova.cangjie.type.model.TypeVariableTypeConstructorMarker

interface ConeInferenceContext : TypeSystemInferenceExtensionContext, ConeTypeContext{
    val symbolProvider: CfirSymbolProvider get() = session.symbolProvider
    override fun CangJieTypeMarker.contains(predicate: (CangJieTypeMarker) -> Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(expectedType: CangJieTypeMarker?): CangJieTypeMarker {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun CangJieTypeMarker.eraseContainingTypeParameters(): CangJieTypeMarker {
        TODO("Not yet implemented")
    }

    override fun Collection<CangJieTypeMarker>.singleBestRepresentative(): CangJieTypeMarker? {
        TODO("Not yet implemented")
    }

    override fun CangJieTypeMarker.isUnit(): Boolean {
        TODO("Not yet implemented")
    }

    override fun CangJieTypeMarker.isFunctionType(): Boolean {
        TODO("Not yet implemented")
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<CangJieTypeMarker>,
        lowerType: CangJieTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        TODO("Not yet implemented")
    }

    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        TODO("Not yet implemented")
    }

    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): RigidTypeMarker {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isFinalClassConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun TypeVariableMarker.freshTypeConstructor(): TypeVariableTypeConstructorMarker {
        TODO("Not yet implemented")
    }

    override fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker {
        TODO("Not yet implemented")
    }

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        TODO("Not yet implemented")
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        TODO("Not yet implemented")
    }

    override fun createTypeWithUpperBoundForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker
    ): CangJieTypeMarker {
        TODO("Not yet implemented")
    }

    override fun CangJieTypeMarker.isSpecial(): Boolean {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun CangJieTypeMarker.isSignedOrUnsignedNumberType(): Boolean {
        TODO("Not yet implemented")
    }

    override fun CangJieTypeMarker.contextParameterCount(): Int {
        TODO("Not yet implemented")
    }

    override fun CangJieTypeMarker.extractArgumentsForFunctionType(): List<CangJieTypeMarker> {
        TODO("Not yet implemented")
    }

    override fun getFunctionTypeConstructor(parametersNumber: Int): TypeConstructorMarker {
        TODO("Not yet implemented")
    }

    override fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker {
        TODO("Not yet implemented")
    }

    override fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker {
        TODO("Not yet implemented")
    }

    override fun createSubstitutorForSuperTypes(baseType: CangJieTypeMarker): TypeSubstitutorMarker? {
        TODO("Not yet implemented")
    }

    override val session: CfirSession
        get() = TODO("Not yet implemented")

    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> {
        TODO("Not yet implemented")
    }

    override fun isSameTypeConstructor(
        a: ConeCangJieType,
        b: ConeCangJieType
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun nothingType(): SimpleTypeMarker {
        TODO("Not yet implemented")
    }

    override fun anyType(): SimpleTypeMarker {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.isExtensionFunction(): Boolean {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.typeDepth(): Int {
        TODO("Not yet implemented")
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<RigidTypeMarker>): RigidTypeMarker? {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        TODO("Not yet implemented")
    }

    override fun unionTypeAttributes(types: List<CangJieTypeMarker>): List<AnnotationMarker> {
        TODO("Not yet implemented")
    }

    override fun CangJieTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): CangJieTypeMarker {
        TODO("Not yet implemented")
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        attributes: List<AnnotationMarker>?
    ): SimpleTypeMarker {
        TODO("Not yet implemented")
    }

    override fun createTypeArgument(type: CangJieTypeMarker): TypeArgumentMarker {
        TODO("Not yet implemented")
    }

    override fun createErrorType(
        debugName: String,
        delegatedType: RigidTypeMarker?
    ): SimpleTypeMarker {
        TODO("Not yet implemented")
    }

    override fun createUninferredType(constructor: TypeConstructorMarker): CangJieTypeMarker {
        TODO("Not yet implemented")
    }

}