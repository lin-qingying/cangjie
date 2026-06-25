/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.checkers.generator

import org.cangnova.cangjie.cfir.CfirElement
import java.io.File
import kotlin.reflect.KClass

/**
 * checker 组件生成 DSL。
 */
class CheckersConfigurator {
    /**
     * 已注册的 CFIR 元素类型到 checker 别名映射。
     */
    private val registeredAliases: MutableMap<KClass<*>, Pair<String, Boolean>> = LinkedHashMap()
    /**
     * 额外 checker 集合字段。
     */
    private val additionalCheckers: MutableMap<String, String> = LinkedHashMap()
    /**
     * 需要额外生成 visit 分派的元素类型。
     */
    private val visitAlso: MutableMap<KClass<*>, Alias> = LinkedHashMap()

    /**
     * 为类型 [T] 注册 checker 别名。
     */
    inline fun <reified T : CfirElement> alias(name: String, withVisit: Boolean = true): Alias {
        return alias(T::class, name, withVisit)
    }

    /**
     * 为指定 CFIR 元素类型注册 checker 别名。
     */
    fun alias(kClass: KClass<out CfirElement>, name: String, withVisit: Boolean): Alias {
        val realName = name.takeIf { it.startsWith("Cfir") } ?: "Cfir$name"
        registeredAliases[kClass] = Pair(realName, withVisit)
        return realName
    }

    /**
     * 注册额外 checker 集合字段。
     */
    fun additional(fieldName: String, classFqn: String) {
        additionalCheckers[fieldName] = classFqn
    }

    /**
     * 为类型 [T] 注册额外 visit 分派。
     */
    inline fun <reified T : CfirElement> visitAlso(name: String) {
        visitAlso(T::class, name)
    }

    /**
     * 为指定 CFIR 元素类型注册额外 visit 分派。
     */
    fun visitAlso(kClass: KClass<out CfirElement>, by: Alias) {
        visitAlso[kClass] = by
    }

    /**
     * 构建不可变生成器配置。
     */
    fun build(): CheckersConfiguration {
        return CheckersConfiguration(registeredAliases, additionalCheckers, visitAlso)
    }
}

/**
 * 根据 DSL 配置生成 checker 组件、组合组件和诊断分派组件。
 */
fun generateCheckersComponents(
    generationPath: File,
    packageName: String,
    abstractCheckerName: String,
    checkMethodTypeParameterConstraint: KClass<out CfirElement>,
    checkType: KClass<out CfirElement>,
    init: CheckersConfigurator.() -> Unit,
) {
    val configuration = CheckersConfigurator().apply(init).build()
    Generator(configuration, generationPath, packageName, abstractCheckerName, checkMethodTypeParameterConstraint, checkType).generate()
}
