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
 * 这些值不是官方 Parser.h::AnnotationKind，不能写入 CfirAnnotationCall.annotationKind。
 * 它们只有在 annotation class 的解析结果命中精确 FqName 后才能发布。
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
    /** Platform annotation family version gate; source names are not gates. */
    val requiredLanguageFeature: LanguageFeature = when (platformKind) {
        CangjiePlatformAnnotationKind.JAVA_MIRROR,
        CangjiePlatformAnnotationKind.JAVA_IMPL,
        CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT,
        -> LanguageFeature.JavaInteropAnnotations

        CangjiePlatformAnnotationKind.OBJ_C_MIRROR,
        CangjiePlatformAnnotationKind.OBJ_C_IMPL,
        CangjiePlatformAnnotationKind.OBJ_C_INIT,
        CangjiePlatformAnnotationKind.OBJ_C_OPTIONAL,
        -> LanguageFeature.ObjCInteropAnnotations

        CangjiePlatformAnnotationKind.FOREIGN_NAME,
        CangjiePlatformAnnotationKind.FOREIGN_GETTER_NAME,
        CangjiePlatformAnnotationKind.FOREIGN_SETTER_NAME,
        -> LanguageFeature.InteropForeignNameAnnotations
    },
) {
    public val classFqName: FqName
        get() = packageFqName.child(Name.identifier(sourceName))

    public val origin: CangjieAnnotationOrigin
        get() = CangjieAnnotationOrigin.PLATFORM_DERIVED
}
