/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.annotations

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 互操作库源码注解的独立身份域。
 *
 * v1.0.0 中这些值只作为平台注解类的派生身份；v1.1.x Parser/AST 又为其中
 * 一部分提供了正式 AnnotationKind（见 [PlatformAnnotationDescriptor.officialKind]）。
 * 无论版本如何，平台类身份仍必须先命中精确 FqName，不能由源码短名制造。
 */
public enum class CangjiePlatformAnnotationKind {
    JAVA_MIRROR,
    JAVA_IMPL,
    JAVA_HAS_DEFAULT,
    OBJ_C_MIRROR,
    OBJ_C_IMPL,
    OBJ_C_INIT,
    OBJ_C_OPTIONAL,
    FOREIGN_NAME,
    FOREIGN_GETTER_NAME,
    FOREIGN_SETTER_NAME,
}

/**
 * v1.1.x 中由 parser 直接发布的对应官方 kind；v1.0.x 只有平台派生身份。
 * 该映射是跨 checker/provider/serialization 的唯一身份桥，不允许各消费者
 * 按源码短名自行重建。
 */
public val CangjiePlatformAnnotationKind.officialKind: BuiltInAnnotationKind
    get() = when (this) {
        CangjiePlatformAnnotationKind.JAVA_MIRROR -> BuiltInAnnotationKind.JAVA_MIRROR
        CangjiePlatformAnnotationKind.JAVA_IMPL -> BuiltInAnnotationKind.JAVA_IMPL
        CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT -> BuiltInAnnotationKind.JAVA_HAS_DEFAULT
        CangjiePlatformAnnotationKind.OBJ_C_MIRROR -> BuiltInAnnotationKind.OBJ_C_MIRROR
        CangjiePlatformAnnotationKind.OBJ_C_IMPL -> BuiltInAnnotationKind.OBJ_C_IMPL
        CangjiePlatformAnnotationKind.OBJ_C_INIT -> BuiltInAnnotationKind.OBJ_C_INIT
        CangjiePlatformAnnotationKind.OBJ_C_OPTIONAL -> BuiltInAnnotationKind.OBJ_C_OPTIONAL
        CangjiePlatformAnnotationKind.FOREIGN_NAME -> BuiltInAnnotationKind.FOREIGN_NAME
        CangjiePlatformAnnotationKind.FOREIGN_GETTER_NAME -> BuiltInAnnotationKind.FOREIGN_GETTER_NAME
        CangjiePlatformAnnotationKind.FOREIGN_SETTER_NAME -> BuiltInAnnotationKind.FOREIGN_SETTER_NAME
    }

/**
 * 平台注解的 source/semantic 契约。
 *
 * [classFqName] 是由 package 与 source name 构成的真实类型身份；同名注解可在
 * Java/ObjC 互操作库中分别声明，因此不能以 source name 作为 map key。
 */
public data class PlatformAnnotationDescriptor(
    val sourceName: String,
    val platformKind: CangjiePlatformAnnotationKind,
    val packageFqName: FqName,
    val argumentSyntax: CangjieAnnotationArgumentSyntax,
    val argumentSchema: AnnotationArgumentSchema,
    val declarationTargets: Set<CangjieAnnotationTarget>,
    val semanticHandler: AnnotationSemanticHandler,
    /** Official parser/AST kind when this spelling is promoted in v1.1.x. */
    val officialKind: BuiltInAnnotationKind? = null,
    /** Platform annotation family version gate; source names are not gates. */
    val requiredLanguageFeature: LanguageFeature = requireNotNull(officialKind?.requiredLanguageFeature) {
        "Platform annotation $sourceName must declare an official kind with a version contract"
    },
) {
    public val classFqName: FqName
        get() = packageFqName.child(Name.identifier(sourceName))

    public val origin: CangjieAnnotationOrigin
        get() = CangjieAnnotationOrigin.PLATFORM_DERIVED
}
