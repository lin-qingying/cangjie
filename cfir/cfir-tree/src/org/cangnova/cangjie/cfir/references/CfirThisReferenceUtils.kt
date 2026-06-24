package org.cangnova.cangjie.cfir.references

import org.cangnova.cangjie.cfir.references.builder.CfirThisReferenceBuilder

/**
 * 构建隐式 `this` 引用。
 *
 * 该 helper 在调用 [init] 前固定 [CfirThisReferenceBuilder.isImplicit] 为 `true`，
 * 用于 receiver 补全、隐式 this 注入和成员访问解析。
 */
inline fun buildImplicitThisReference(init: CfirThisReferenceBuilder.() -> Unit): CfirThisReference =
    CfirThisReferenceBuilder().apply {
        isImplicit = true
        init()
    }.build()
