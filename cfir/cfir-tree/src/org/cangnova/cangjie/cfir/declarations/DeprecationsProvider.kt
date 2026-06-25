/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCachesFactory
import org.cangnova.cangjie.cfir.caches.createCache
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
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
