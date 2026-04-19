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
    abstract fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite?
}

class DeprecationsProviderImpl(
    cfirCachesFactory: CfirCachesFactory,
    private val all: List<DeprecationInfoProvider>?,
    private val bySpecificSite: Map<AnnotationUseSiteTarget, List<DeprecationInfoProvider>>?,
) : DeprecationsProvider() {
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

    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite {
        return cache.getValue(languageVersionSettings, null)
    }

    private fun List<DeprecationInfoProvider>.computeDeprecationInfoOrNull(
        languageVersionSettings: LanguageVersionSettings,
    ): CfirDeprecationInfo? {
        return mapNotNull { it.computeDeprecationInfo(languageVersionSettings) }.maxByOrNull { it.deprecationLevel }
    }
}

object EmptyDeprecationsProvider : DeprecationsProvider() {
    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        return null
    }
}

object UnresolvedDeprecationProvider : DeprecationsProvider() {
    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        return null
    }
}

abstract class DeprecationInfoProvider {
    abstract fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): CfirDeprecationInfo?
}

abstract class CfirDeprecationInfo : Comparable<CfirDeprecationInfo> {
    abstract val deprecationLevel: DeprecationLevelValue
    abstract val propagatesToOverrides: Boolean

    /**
     * 该消息访问时机与 Kotlin FIR 保持一致：
     * 不应在注解参数尚未完成解析前读取。
     */
    abstract fun getMessage(session: CfirSession): String?

    override fun compareTo(other: CfirDeprecationInfo): Int {
        val levelResult = deprecationLevel.compareTo(other.deprecationLevel)
        return if (levelResult == 0 && !propagatesToOverrides && other.propagatesToOverrides) {
            1
        } else {
            levelResult
        }
    }
}
