/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCachesFactory
import org.cangnova.cangjie.cfir.caches.createCache
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.deprecation.DeprecationLevelValue

/**
 * 对齐 Kotlin `DeprecationsProvider` 的主抽象。
 *
 * declaration tree 只持有 provider，不直接缓存解析后的弃用信息；
 * 具体缓存策略由 provider 内部通过 [CfirCachesFactory] 承担。
 */
abstract class DeprecationsProvider {
    /**
     * 根据当前语言版本设置计算声明的弃用信息。
     *
     * 返回 `null` 表示该声明没有可用弃用信息。
     */
    abstract fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite?
}

/**
 * 基于注解解析结果的弃用信息提供者实现。
 *
 * @property all 作用于整个声明的弃用信息计算器列表。
 * @property bySpecificSite 按 use-site target 拆分的弃用信息计算器列表。
 */
class DeprecationsProviderImpl(
    cfirCachesFactory: CfirCachesFactory,
    /**
     * 作用于整个声明的弃用信息计算器列表。
     */
    private val all: List<DeprecationInfoProvider>?,
    /**
     * 按 use-site target 拆分的弃用信息计算器列表。
     */
    private val bySpecificSite: Map<AnnotationUseSiteTarget, List<DeprecationInfoProvider>>?,
) : DeprecationsProvider() {
    /**
     * 以 [LanguageVersionSettings] 为键缓存弃用信息。
     *
     * 弃用等级可能受语言版本影响，因此缓存不能只绑定到声明本身。
     */
    private val cache: CfirCache<LanguageVersionSettings, DeprecationsPerUseSite, Nothing?> =
        cfirCachesFactory.createCache { languageVersionSettings ->
            @Suppress("UNCHECKED_CAST")
            DeprecationsPerUseSite(
                all?.computeDeprecationInfoOrNull(languageVersionSettings),
                bySpecificSite
                    ?.mapValues { (_, info) -> info.computeDeprecationInfoOrNull(languageVersionSettings) }
                    ?.filterValues { it != null } as Map<AnnotationUseSiteTarget, CfirDeprecationInfo>?,
            )
        }

    /**
     * 返回当前语言版本下合并后的弃用信息。
     */
    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite {
        return cache.getValue(languageVersionSettings, null)
    }

    /**
     * 计算一组注解提供者中最高优先级的弃用信息。
     */
    private fun List<DeprecationInfoProvider>.computeDeprecationInfoOrNull(
        languageVersionSettings: LanguageVersionSettings,
    ): CfirDeprecationInfo? {
        return mapNotNull { it.computeDeprecationInfo(languageVersionSettings) }.maxByOrNull { it.deprecationLevel }
    }
}

/**
 * 明确表示声明没有弃用信息的 provider。
 */
object EmptyDeprecationsProvider : DeprecationsProvider() {
    /**
     * 空 provider 永远返回 `null`。
     */
    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        return null
    }
}

/**
 * 声明弃用注解尚未解析完成时使用的占位 provider。
 */
object UnresolvedDeprecationProvider : DeprecationsProvider() {
    /**
     * 未解析 provider 在真正解析完成前不暴露弃用信息。
     */
    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        return null
    }
}

/**
 * 单个弃用注解或来源的弃用信息计算器。
 */
abstract class DeprecationInfoProvider {
    /**
     * 在指定语言版本设置下计算弃用信息。
     */
    abstract fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): CfirDeprecationInfo?
}

/**
 * CFIR 中可比较的弃用语义信息。
 */
abstract class CfirDeprecationInfo : Comparable<CfirDeprecationInfo> {
    /**
     * 弃用等级，决定诊断严重程度与多个信息合并时的优先级。
     */
    abstract val deprecationLevel: DeprecationLevelValue

    /**
     * 当前弃用信息是否传播到 override 成员。
     */
    abstract val propagatesToOverrides: Boolean

    /**
     * 该消息访问时机与 Kotlin FIR 保持一致：
     * 不应在注解参数尚未完成解析前读取。
     */
    abstract fun getMessage(session: CfirSession): String?

    /**
     * 按弃用等级和 override 传播策略比较两个弃用信息的优先级。
     */
    override fun compareTo(other: CfirDeprecationInfo): Int {
        val levelResult = deprecationLevel.compareTo(other.deprecationLevel)
        return if (levelResult == 0 && !propagatesToOverrides && other.propagatesToOverrides) {
            1
        } else {
            levelResult
        }
    }
}

// ==================================== 注解驱动的弃用信息 ====================================

/**
 * 从单个 `@Deprecated` 注解调用解析出的弃用信息。
 *
 * @property deprecationLevel 弃用严格级别（strict=true 时为 ERROR，否则 WARNING）。
 * @property message 弃用提示信息（`message` 参数或首个位置参数）。
 * @property since 弃用起始版本（`since` 参数）。
 */
class CfirAnnotationDeprecationInfo(
    override val deprecationLevel: DeprecationLevelValue,
    private val message: String?,
    private val since: String?,
    override val propagatesToOverrides: Boolean,
) : CfirDeprecationInfo() {
    override fun getMessage(session: CfirSession): String? = message

    override fun toString(): String =
        "CfirAnnotationDeprecationInfo(level=$deprecationLevel, message=$message, since=$since)"
}

/**
 * 基于 `@Deprecated` 注解调用惰性计算弃用信息。
 *
 * 对齐 C++ `ExtractArgumentsOfDeprecatedAnno`：
 * - 无命名或名为 `message` 的参数 → 弃用提示信息；
 * - 名为 `since` 的参数 → 弃用起始版本；
 * - 名为 `strict` 的参数为 `true` → ERROR 级，否则 WARNING 级。
 */
class DeprecatedAnnotationDeprecationInfoProvider(
    private val annotation: CfirAnnotationCall,
) : DeprecationInfoProvider() {
    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): CfirDeprecationInfo? {
        val strict = annotation.booleanArgument("strict") == true
        return CfirAnnotationDeprecationInfo(
            deprecationLevel = if (strict) DeprecationLevelValue.ERROR else DeprecationLevelValue.WARNING,
            message = annotation.deprecatedMessageOrNull(),
            since = annotation.stringArgument("since"),
            propagatesToOverrides = true,
        )
    }
}

/**
 * 不依赖 [CfirCachesFactory] 的轻量弃用信息提供者。
 *
 * 弃用注解参数均为已解析字面量，每次读取时直接重算，开销可忽略；
 * 供 CJO 反序列化等不保证注册缓存工厂的路径使用。
 */
class AnnotationDeprecationsProvider(
    private val all: List<DeprecationInfoProvider>,
) : DeprecationsProvider() {
    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite {
        val merged = all
            .mapNotNull { it.computeDeprecationInfo(languageVersionSettings) }
            .maxByOrNull { it.deprecationLevel }
        return DeprecationsPerUseSite(merged, null)
    }
}

/**
 * 根据声明注解列表构建弃用信息提供者。
 *
 * 只识别短名为 `Deprecated` 的内置弃用注解；无匹配注解时返回 [EmptyDeprecationsProvider]。
 * 返回的 provider 不依赖 session 级缓存工厂，可在反序列化/构建路径直接使用。
 */
fun buildDeprecationsProvider(annotations: List<CfirAnnotation>): DeprecationsProvider {
    val all = annotations.mapNotNullTo(mutableListOf()) { annotation ->
        if (!annotation.isDeprecatedAnnotation()) return@mapNotNullTo null
        (annotation as? CfirAnnotationCall)?.let(::DeprecatedAnnotationDeprecationInfoProvider)
    }
    if (all.isEmpty()) return EmptyDeprecationsProvider
    return AnnotationDeprecationsProvider(all)
}

/** 内置 `@Deprecated` 注解的短名。 */
private val DEPRECATED_ANNOTATION_NAME: Name = Name.identifier("Deprecated")

/** 判断注解是否表示内置 `@Deprecated`。 */
private fun CfirAnnotation.isDeprecatedAnnotation(): Boolean =
    deprecatedAnnotationShortNameOrNull() == DEPRECATED_ANNOTATION_NAME

/** 从注解 typeRef（已解析）或 calleeReference 名称提取注解短名。 */
private fun CfirAnnotation.deprecatedAnnotationShortNameOrNull(): Name? {
    val typeRef = typeRef
    if (typeRef is CfirResolvedTypeRef) {
        val classId = (typeRef.coneType as? ConeClassLikeType)?.classId
        if (classId != null) return classId.shortClassName
    }
    return (this as? CfirAnnotationCall)
        ?.calleeReference
        ?.let { it as? CfirNamedReference }
        ?.name
}

/** 读取 `message` 命名参数或首个位置参数作为弃用提示信息。 */
private fun CfirAnnotationCall.deprecatedMessageOrNull(): String? =
    (argumentByName("message") ?: explicitArguments().firstOrNull())
        ?.unwrapNamedArgument()
        ?.literalStringOrNull()

/** 读取指定命名字符串参数。 */
private fun CfirAnnotationCall.stringArgument(name: String): String? =
    argumentByName(name)?.unwrapNamedArgument()?.literalStringOrNull()

/** 读取指定命名布尔参数。 */
private fun CfirAnnotationCall.booleanArgument(name: String): Boolean? =
    argumentByName(name)
        ?.unwrapNamedArgument()
        ?.let { (it as? CfirLiteralExpression)?.value as? Boolean }

/** 去掉命名实参包装，取得注解实参的真实表达式。 */
private tailrec fun CfirExpression.unwrapNamedArgument(): CfirExpression = when (this) {
    is CfirNamedArgumentExpression -> expression.unwrapNamedArgument()
    else -> this
}

/** 字面量表达式 → 字符串值；非字面量返回 null。 */
private fun CfirExpression.literalStringOrNull(): String? =
    (this as? CfirLiteralExpression)?.value?.toString()

/** 返回注解调用在源码中显式写出的实参列表。 */
private fun CfirAnnotationCall.explicitArguments(): List<CfirExpression> =
    (argumentList as? CfirResolvedArgumentList)
        ?.originalArgumentList
        ?.arguments
        ?: argumentList.arguments

/** 根据命名实参或已解析实参映射查找指定名称的实参表达式。 */
private fun CfirAnnotationCall.argumentByName(name: String): CfirExpression? {
    explicitArguments()
        .filterIsInstance<CfirNamedArgumentExpression>()
        .firstOrNull { it.argumentName.asString() == name }
        ?.let { return it.expression }
    val resolved = argumentList as? CfirResolvedArgumentList ?: return null
    return resolved.mapping.entries
        .firstOrNull { (_, parameter) -> parameter.name.asString() == name }
        ?.key
        ?.unwrapNamedArgument()
}
