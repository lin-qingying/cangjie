/*
 * Copyright 2026 LinQingYing. and contributors.
 * Licensed under the Apache License, Version 2.0.
 */

package org.cangnova.cangjie.psi.stubs

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility

/** 二进制声明数据的独立所有者接口；语法 stub 属性渲染只遍历既有语法接口。 */
interface CangJieCompiledDeclarationMetadataOwner {
    /** CJO 的完整语义声明头；源码 modifier-list 没有该元数据。 */
    val compiledDeclarationMetadata: CangJieCompiledDeclarationMetadata?
}

/**
 * CJO 声明头的完整语义状态；modifier mask 只负责可重解析的语法投影。
 *
 * 接口隐式 ABSTRACT/OPEN、DEFAULT 和 CJMP 等事实无法由展示 token 无损恢复。
 * 此快照由已解码的 CJO 声明产生，不含符号或 session，随所属 stub 一起持久化。
 */
data class CangJieCompiledDeclarationStatus(
    val visibility: Visibility,
    val modality: Modality,
    val visibilityExplicitness: Explicitness,
    val modalityExplicitness: Explicitness,
    val bodyPresence: BodyPresence,
    val flags: Set<Flag>,
) {
    enum class Explicitness { UNKNOWN, IMPLICIT, EXPLICIT }
    enum class BodyPresence { UNKNOWN, ABSENT, PRESENT }

    /** 位序属于 stub 持久化协议；追加或调整字段必须同步递增 stub version。 */
    enum class Flag {
        DECLARATION_FILE,
        VISIBILITY_EXPLICIT,
        MODALITY_EXPLICIT,
        ABSTRACT_EXPLICIT,
        OVERRIDE,
        OPERATOR,
        STATIC,
        CONST,
        MUT,
        UNSAFE,
        FOREIGN,
        C,
        COMMON,
        FROM_COMMON_PART,
        SPECIFIC,
        COMMON_WITH_DEFAULT,
        REDEF,
        DEFAULT,
        ABSTRACT,
        OPEN,
        SEALED,
    }
}

/**
 * 属于一条实际 CJO 声明的状态快照。
 *
 * property 的访问器来自 PropInfo 的真实 getter/setter 声明引用；访问器没有 modifier-list
 * 语法节点，故由 property 的声明头保存各自状态，不能用 property 状态或默认状态代替。
 */
data class CangJieCompiledDeclarationMetadata(
    val status: CangJieCompiledDeclarationStatus,
    val getterStatus: CangJieCompiledDeclarationStatus? = null,
    val setterStatus: CangJieCompiledDeclarationStatus? = null,
    /** 当前 binary 内的精确 pool index；源码不携带这个引用。 */
    val declarationIndex: Int? = null,
    val setterParameter: CangJieCompiledSetterParameter? = null,
)

/** setter 参数的真实签名事实；类型由官方 property 签名确定，annotations 按同 binary index 读取。 */
data class CangJieCompiledSetterParameter(
    val name: String,
    val isNameSpecial: Boolean,
    val isNamed: Boolean,
    val hasDefaultValue: Boolean,
    val status: CangJieCompiledDeclarationStatus,
)
