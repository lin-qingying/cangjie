/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget

/**
 * 对齐 Kotlin `DeprecationsPerUseSite`。
 *
 * `all` 表示声明整体上的弃用信息；
 * `bySpecificSite` 表示 getter/setter 等 use-site 的更细粒度弃用信息。
 */
class DeprecationsPerUseSite(
    val all: CfirDeprecationInfo?,
    val bySpecificSite: Map<AnnotationUseSiteTarget, CfirDeprecationInfo>?,
) {
    fun forUseSite(vararg sites: AnnotationUseSiteTarget): CfirDeprecationInfo? {
        if (bySpecificSite != null) {
            for (site in sites) {
                bySpecificSite[site]?.let { return it }
            }
        }
        return all
    }

    fun isEmpty(): Boolean = all == null && bySpecificSite == null

    fun isNotEmpty(): Boolean = !isEmpty()

    override fun toString(): String =
        if (isEmpty()) {
            "NoDeprecation"
        } else {
            "org.cangnova.cangjie.cfir.declarations.DeprecationInfoForUseSites(all=$all, bySpecificSite=$bySpecificSite)"
        }
}
