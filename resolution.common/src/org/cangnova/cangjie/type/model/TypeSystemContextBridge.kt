package org.cangnova.cangjie.type.model

fun TypeVariableMarker.freshTypeConstructor(c: TypeSystemInferenceExtensionContext): TypeVariableTypeConstructorMarker =
    with(c) { freshTypeConstructor() }

fun TypeVariableMarker.defaultType(c: TypeSystemInferenceExtensionContext): SimpleTypeMarker =
    with(c) { defaultType() }

fun TypeSubstitutorMarker.safeSubstitute(
    c: TypeSystemInferenceExtensionContext,
    type: CangJieTypeMarker,
): CangJieTypeMarker = with(c) { safeSubstitute(type) }

fun CangJieTypeMarker.dependsOnTypeConstructor(
    c: TypeSystemInferenceExtensionContext,
    typeConstructors: Set<TypeConstructorMarker>,
): Boolean = with(c) {
    contains { it.typeConstructor() in typeConstructors }
}

fun CangJieTypeMarker.dependsOnTypeParameters(
    c: TypeSystemInferenceExtensionContext,
    typeParameters: Collection<TypeParameterMarker>,
): Boolean {
    val typeConstructors = with(c) { typeParameters.mapTo(mutableSetOf()) { it.getTypeConstructor() } }
    return dependsOnTypeConstructor(c, typeConstructors)
}
