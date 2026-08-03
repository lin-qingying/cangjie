/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElementWithResolveState

/**
 * 按 designation 路径逐步进入嵌套声明并收集目标位置上下文的抽象 collector。
 */
abstract class ContextByDesignationCollector<C : Any>(
    /**
     * 指示需要沿哪条 CFIR 路径进入目标声明。
     */
    private val designation: CfirDesignation,
) {
    /**
     * 已在目标位置收集到的上下文。
     */
    private var context: C? = null

    /**
     * 当前遍历 designation 路径时的游标状态。
     */
    private val designationState = CfirDesignationState(designation)

    /**
     * 返回当前嵌套声明位置的上下文。
     */
    protected abstract fun getCurrentContext(): C


    /**
     * 进入下一个嵌套声明，使子类同步推进自身持有的解析上下文。
     */
    protected abstract fun goToNestedDeclaration(target: CfirElementWithResolveState)

    /**
     * 返回已经收集到的目标上下文；调用前必须已推进到目标位置。
     */
    fun getCollectedContext(): C {
        return context
            ?: error("Context is not collected yet")
    }

    /**
     * 沿 designation 路径前进一步，或在到达目标时保存目标上下文。
     */

    fun nextStep() {
        if (designationState.canGoNext()) {
            designationState.goNext()
            if (designationState.currentDeclarationIfPresent == designation.target) {
                check(context == null)
                context = getCurrentContext()
            }
            goToNestedDeclaration(designationState.currentDeclaration)
        } else {
            if (designationState.currentDeclarationIfPresent == designation.target) {
                designationState.goToInnerDeclaration()
            }
        }
    }
}

/**
 * designation 路径遍历过程中的索引状态。
 */
private class CfirDesignationState(
    /**
     * 当前正在遍历的 designation。
     */
    val designation: CfirDesignation,
) {
    /**
     * Holds current declaration index
     * if `currentIndex in [0, designation.path.lastIndex]` then current declaration is in path
     * if `currentIndex == `designation.path.lastIndex + 1` then current declaration is our target declaration
     * if `currentIndex > designation.path.lastIndex + 1` then we are inside target declaration
     */
    private var currentIndex = -1

    /**
     * 是否还可以继续进入 designation.path 中的下一个声明。
     */
    fun canGoNext(): Boolean = currentIndex < designation.path.size

    /**
     * 当前索引对应的声明；尚未开始或已经深入目标内部时返回 null。
     */
    val currentDeclarationIfPresent: CfirElementWithResolveState?
        get() = designation.path.getOrNull(currentIndex) ?: when (currentIndex) {
            designation.path.size -> designation.target
            else -> null
        }

    /**
     * 当前索引对应的声明；若已经深入目标内部则抛出带 CFIR 附件的错误。
     */
    val currentDeclaration: CfirElementWithResolveState
        get() = currentDeclarationIfPresent
            ?: errorWithCfirSpecificEntries("Went inside target declaration")

    /**
     * 将游标推进到下一个 path 声明。
     */
    fun goNext() {
        if (canGoNext()) {
            currentIndex++
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    /**
     * 将游标从目标声明推进到目标内部状态。
     */
    fun goToInnerDeclaration() {
        if (currentIndex == designation.path.size) {
            currentIndex++
        } else {
            throw IndexOutOfBoundsException()
        }
    }
}
