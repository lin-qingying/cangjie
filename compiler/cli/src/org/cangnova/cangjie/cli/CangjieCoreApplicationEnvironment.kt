package org.cangnova.cangjie.cli

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.Disposable

/**
 * 仓颉核心应用环境（对齐 Kotlin 的 KotlinCoreApplicationEnvironment）。
 *
 * 继承 IntelliJ [CoreApplicationEnvironment]，注册仓颉语言的文件类型和解析器。
 */
class CangjieCoreApplicationEnvironment private constructor(
    parentDisposable: Disposable,
    unitTestMode: Boolean,
) : CoreApplicationEnvironment(parentDisposable, unitTestMode) {

    companion object {
        fun create(
            parentDisposable: Disposable,
            unitTestMode: Boolean = true,
        ): CangjieCoreApplicationEnvironment {
            return CangjieCoreApplicationEnvironment(parentDisposable, unitTestMode)
        }
    }
}
