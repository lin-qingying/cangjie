@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.modification.publishCodeFragmentContextModificationEvent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.psi.CjElement

/**
 * 将低阶 CFIR 的块内修改事件桥接为代码片段上下文修改事件的监听器。
 *
 * 代码片段依赖外部上下文进行解析；当其内部声明被重新分析后，需要通知 analysis API
 * 代码片段上下文已经变化，使相关缓存按代码片段维度失效。
 *
 * @param project 当前监听器注册到的工程。
 */
internal class LLCfirInBlockModificationListenerForCodeFragments(val project: Project) : LLCfirInBlockModificationListener {
    /**
     * 在任意块内修改发生后发布代码片段上下文修改事件。
     *
     * 当前实现不区分 [element] 的具体种类，依赖 [module] 的项目结构事件机制完成实际缓存失效。
     */
    override fun afterModification(element: CjElement, module: CaModule) {
        module.publishCodeFragmentContextModificationEvent()
    }
}
