package org.cangnova.cangjie.cli

import com.intellij.core.CoreProjectEnvironment
import com.intellij.openapi.Disposable

/**
 * 仓颉核心项目环境（对齐 Kotlin 的 KotlinCoreProjectEnvironment）。
 *
 * 继承 IntelliJ [com.intellij.core.CoreProjectEnvironment]，提供仓颉项目的 PSI 和服务支持。
 */
open class CangjieCoreProjectEnvironment(
    disposable: Disposable,
    applicationEnvironment: CangjieCoreApplicationEnvironment,
) : CoreProjectEnvironment(disposable, applicationEnvironment)
