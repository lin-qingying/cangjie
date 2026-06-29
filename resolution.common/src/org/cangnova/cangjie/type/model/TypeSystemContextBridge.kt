package org.cangnova.cangjie.type.model

/**
 * 在指定推断类型系统上下文中取得类型变量的新鲜类型构造器。
 */
fun TypeVariableMarker.freshTypeConstructor(c: TypeSystemInferenceExtensionContext): TypeVariableTypeConstructorMarker =
    with(c) { freshTypeConstructor() }

/**
 * 在指定推断类型系统上下文中取得类型变量的默认类型。
 */
fun TypeVariableMarker.defaultType(c: TypeSystemInferenceExtensionContext): SimpleTypeMarker =
    with(c) { defaultType() }

/**
 * 在指定推断类型系统上下文中安全替换类型。
 */
fun TypeSubstitutorMarker.safeSubstitute(
    c: TypeSystemInferenceExtensionContext,
    type: CangJieTypeMarker,
): CangJieTypeMarker = with(c) { safeSubstitute(type) }

/**
 * 判断类型是否依赖给定类型构造器集合。
 */
fun CangJieTypeMarker.dependsOnTypeConstructor(
    c: TypeSystemInferenceExtensionContext,
    typeConstructors: Set<TypeConstructorMarker>,
): Boolean = with(c) {
    contains { it.typeConstructor() in typeConstructors }
}

/**
 * 判断类型是否依赖给定类型参数集合。
 */
fun CangJieTypeMarker.dependsOnTypeParameters(
    c: TypeSystemInferenceExtensionContext,
    typeParameters: Collection<TypeParameterMarker>,
): Boolean {
    val typeConstructors = with(c) { typeParameters.mapTo(mutableSetOf()) { it.getTypeConstructor() } }
    return dependsOnTypeConstructor(c, typeConstructors)
}
