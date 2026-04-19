/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.platform.resolution

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import org.cangnova.cangjie.analysis.api.CaIdeApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaEngineService

/**
 * 该服务用于在 Analysis API、仓颉 IntelliJ 插件与其他 IntelliJ 语言插件之间传递“当前线程是否正处于仓颉解析中”的状态。
 *
 * 当线程正执行仓颉解析时，其他语言侧可以据此施加更严格的约束，避免破坏编译器/IDE 的解析契约。
 */
@CaIdeApi
@CaPlatformInterface
interface CaResolutionActivityTracker : CaEngineService {
    /**
     * 当前线程是否正在执行仓颉解析逻辑。
     */
    val isCangJieResolutionActive: Boolean

    @CaIdeApi
    @CaPlatformInterface
    companion object {
        fun getInstance(): CaResolutionActivityTracker? {
            return ApplicationManager.getApplication().serviceOrNull<CaResolutionActivityTracker>()
        }
    }
}
