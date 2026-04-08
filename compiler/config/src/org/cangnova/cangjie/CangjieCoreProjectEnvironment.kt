package org.cangnova.cangjie

import com.intellij.core.CoreProjectEnvironment
import com.intellij.openapi.Disposable

/**
 * 仓颉核心项目环境。
 *
 * 对齐 Kotlin `KotlinCoreProjectEnvironment` 的职责：
 * - 在项目容器创建期间尽早预注册项目级扩展点
 * - 在基础 PSI 服务装配完成后补齐仓颉需要的项目级平台服务
 */
open class CangjieCoreProjectEnvironment(
    disposable: Disposable,
    applicationEnvironment: CangjieCoreApplicationEnvironment,
) : CoreProjectEnvironment(disposable, applicationEnvironment) {
    override fun preregisterServices() {
        CangJieHeadlessPlatformBootstrap.preregisterProjectEnvironment(this)
    }

    init {
        CangJieHeadlessPlatformBootstrap.initializeProjectEnvironment(this)
    }
}
