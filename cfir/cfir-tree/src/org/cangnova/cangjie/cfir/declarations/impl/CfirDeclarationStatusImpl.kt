/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.source.CjSourceElement

/**
 * [CfirDeclarationStatus] 的标准可变实现。
 *
 * 该实现把大量布尔修饰符压缩到 [flags] bitset 中，只把可见性和 modality 作为独立字段保存。
 * 这样生成式声明树在复制和阶段推进时可以低成本携带状态，同时保持与接口上的逐项属性一致。
 *
 * @property visibility 声明可见性。
 * @property modality 声明 modality；为 `null` 时表示声明尚未或无需拥有 modality。
 */
open class CfirDeclarationStatusImpl(
    /**
     * 声明可见性。
     */
    override val visibility: Visibility = Visibilities.Public,
    /**
     * 声明 modality；为 `null` 时表示声明尚未或无需拥有 modality。
     */
    override val modality: Modality? = null,
) : CfirPureAbstractElement(), CfirDeclarationStatus {
    /**
     * 声明状态对象本身没有独立源码节点。
     */
    override val source: CjSourceElement? get() = null

    /**
     * 声明修饰符的压缩 bitset。
     *
     * 具体 bit 含义由 [Modifier.mask] 定义。
     */
    protected var flags: Int = 0

    /**
     * 暴露给框架内部复制逻辑的原始 bitset。
     */
    @CfirImplementationDetail
    internal val rawFlags: Int get() = flags

    /**
     * 读取指定修饰符 bit。
     */
    private operator fun get(modifier: Modifier): Boolean = (flags and modifier.mask) != 0

    /**
     * 设置或清除指定修饰符 bit。
     */
    private operator fun set(modifier: Modifier, value: Boolean) {
        flags = if (value) {
            flags or modifier.mask
        } else {
            flags and modifier.mask.inv()
        }
    }

    /**
     * 声明是否显式带有 override 语义。
     */
    override var isOverride: Boolean
        get() = this[Modifier.OVERRIDE]
        set(value) {
            this[Modifier.OVERRIDE] = value
        }

    /**
     * 声明可见性是否由源码显式写出。
     */
    override var isVisibilityExplicit: Boolean
        get() = this[Modifier.VISIBILITY_EXPLICIT]
        set(value) {
            this[Modifier.VISIBILITY_EXPLICIT] = value
        }

    /**
     * 声明 modality 是否由源码显式写出。
     */
    override var isModalityExplicit: Boolean
        get() = this[Modifier.MODALITY_EXPLICIT]
        set(value) {
            this[Modifier.MODALITY_EXPLICIT] = value
        }

    /**
     * abstract 是否由源码显式写出。
     *
     * 它记录 raw CFIR 的来源事实，不能用 [isAbstract] 推断：后者还会携带无 body 成员
     * 推导出的抽象语义。STATUS 阶段依赖该位区分非法的 `static abstract` 与合法的隐式 abstract。
     */
    override var isAbstractExplicit: Boolean
        get() = this[Modifier.ABSTRACT_EXPLICIT]
        set(value) {
            this[Modifier.ABSTRACT_EXPLICIT] = value
        }

    /**
     * 声明是否带有 operator 修饰。
     */
    override var isOperator: Boolean
        get() = this[Modifier.OPERATOR]
        set(value) {
            this[Modifier.OPERATOR] = value
        }

    /**
     * 声明是否为 static 成员。
     */
    override var isStatic: Boolean
        get() = this[Modifier.STATIC]
        set(value) {
            this[Modifier.STATIC] = value
        }

    /**
     * 声明是否带有 const 修饰。
     */
    override var isConst: Boolean
        get() = this[Modifier.CONST]
        set(value) {
            this[Modifier.CONST] = value
        }

    /**
     * 声明是否带有 mut 修饰。
     */
    override var isMut: Boolean
        get() = this[Modifier.MUT]
        set(value) {
            this[Modifier.MUT] = value
        }

    /**
     * 声明是否处于 unsafe 语义上下文。
     */
    override var isUnsafe: Boolean
        get() = this[Modifier.UNSAFE]
        set(value) {
            this[Modifier.UNSAFE] = value
        }

    /**
     * 声明是否带有 foreign 修饰。
     */
    override var isForeign: Boolean
        get() = this[Modifier.FOREIGN]
        set(value) {
            this[Modifier.FOREIGN] = value
        }

    /**
     * 声明是否带有 common 修饰。
     */
    override var isCommon: Boolean
        get() = this[Modifier.COMMON]
        set(value) {
            this[Modifier.COMMON] = value
        }

    /**
     * 声明是否带有 specific 修饰。
     */
    override var isSpecific: Boolean
        get() = this[Modifier.SPECIFIC]
        set(value) {
            this[Modifier.SPECIFIC] = value
        }

    /**
     * 声明是否带有 redef 修饰。
     */
    override var isRedef: Boolean
        get() = this[Modifier.REDEF]
        set(value) {
            this[Modifier.REDEF] = value
        }

    /**
     * 声明是否带有 default 修饰。
     */
    override var isDefault: Boolean
        get() = this[Modifier.DEFAULT]
        set(value) {
            this[Modifier.DEFAULT] = value
        }

    /**
     * 声明是否具有 abstract 语义。
     */
    override var isAbstract: Boolean
        get() = this[Modifier.ABSTRACT]
        set(value) {
            this[Modifier.ABSTRACT] = value
        }

    /**
     * 声明是否具有 open 语义。
     */
    override var isOpen: Boolean
        get() = this[Modifier.OPEN]
        set(value) {
            this[Modifier.OPEN] = value
        }

    /**
     * 声明是否具有 sealed 语义。
     */
    override var isSealed: Boolean
        get() = this[Modifier.SEALED]
        set(value) {
            this[Modifier.SEALED] = value
        }

    /**
     * [flags] 中各修饰符 bit 的定义。
     *
     * @property mask 当前修饰符在 bitset 中使用的掩码。
     */
    enum class Modifier(val mask: Int) {
        /**
         * override 修饰符 bit。
         */
        OVERRIDE(0x1),
        /**
         * operator 修饰符 bit。
         */
        OPERATOR(0x2),
        /**
         * static 修饰符 bit。
         */
        STATIC(0x4),
        /**
         * const 修饰符 bit。
         */
        CONST(0x8),
        /**
         * mut 修饰符 bit。
         */
        MUT(0x10),
        /**
         * unsafe 修饰符 bit。
         */
        UNSAFE(0x20),
        /**
         * foreign 修饰符 bit。
         */
        FOREIGN(0x40),
        /**
         * common 修饰符 bit。
         */
        COMMON(0x80),
        /**
         * specific 修饰符 bit。
         */
        SPECIFIC(0x100),
        /**
         * redef 修饰符 bit。
         */
        REDEF(0x200),
        /**
         * abstract 修饰符 bit。
         */
        ABSTRACT(0x400),
        /**
         * open 修饰符 bit。
         */
        OPEN(0x800),
        /**
         * sealed 修饰符 bit。
         */
        SEALED(0x1000),
        /**
         * 可见性显式写出 bit。
         */
        VISIBILITY_EXPLICIT(0x2000),
        /**
         * modality 显式写出 bit。
         */
        MODALITY_EXPLICIT(0x4000),
        /**
         * default 修饰符 bit。
         */
        DEFAULT(0x8000),
        /**
         * abstract 由源码显式写出的来源标记 bit。
         */
        ABSTRACT_EXPLICIT(0x10000),
    }

    /**
     * 状态节点没有子节点需要访问。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    /**
     * 状态节点没有子节点需要转换，直接返回自身。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }

    /**
     * 常用声明状态实例。
     */
    companion object {
        /**
         * public、无 modality、无任何修饰符 bit 的默认状态。
         */
        val DEFAULT = CfirDeclarationStatusImpl()
    }
}
