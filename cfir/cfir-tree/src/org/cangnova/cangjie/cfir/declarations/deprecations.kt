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
    /**
     * 作用于声明整体的弃用信息。
     */
    val all: CfirDeprecationInfo?,
    /**
     * 按注解 use-site target 细分的弃用信息。
     */
    val bySpecificSite: Map<AnnotationUseSiteTarget, CfirDeprecationInfo>?,
) {
    /**
     * 按优先顺序查询指定 use-site 的弃用信息。
     *
     * 若所有指定 use-site 都没有专属信息，则回退到声明整体的 [all]。
     */
    fun forUseSite(vararg sites: AnnotationUseSiteTarget): CfirDeprecationInfo? {
        if (bySpecificSite != null) {
            for (site in sites) {
                bySpecificSite[site]?.let { return it }
            }
        }
        return all
    }

    /**
     * 当前对象是否不包含任何弃用信息。
     */
    fun isEmpty(): Boolean = all == null && bySpecificSite == null

    /**
     * 当前对象是否至少包含一条弃用信息。
     */
    fun isNotEmpty(): Boolean = !isEmpty()

    /**
     * 返回调试输出使用的弃用信息摘要。
     */
    override fun toString(): String =
        if (isEmpty()) {
            "NoDeprecation"
        } else {
            "org.cangnova.cangjie.cfir.declarations.DeprecationInfoForUseSites(all=$all, bySpecificSite=$bySpecificSite)"
        }
}
