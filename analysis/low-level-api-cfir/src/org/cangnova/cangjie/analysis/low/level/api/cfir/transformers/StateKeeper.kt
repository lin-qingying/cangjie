/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.cfir.CfirElementWithResolveState

/**
 * 标记 [StateKeeper] 构建 DSL 的作用域，避免嵌套 DSL 中误用接收者。
 */
@DslMarker
internal annotation class StateKeeperDsl

/**
 * 状态后处理函数。
 *
 * 后处理在 arranger 调整之后、真实 action 执行之前运行，用于处理无法由单个状态保存规则自行完成的额外修正。
 */
internal typealias PostProcessor = () -> Unit

/**
 * [StateKeeper] DSL 中用于登记待恢复状态的构建器。
 */
@StateKeeperDsl
internal interface StateKeeperBuilder {
    /**
     * 登记一份需要在失败时恢复或在 action 前后处理的 [state]。
     */
    fun register(state: PreservedState)
}

/**
 * 针对单个 [Owner] 构建状态保存规则的 DSL 作用域。
 *
 * @property owner 当前正在登记状态规则的对象。
 */
@JvmInline
@StateKeeperDsl
internal value class StateKeeperScope<Owner : Any, Context : Any>(private val owner: Owner) {
    /**
     * 定义一条带 arranger 的实体状态保存规则。
     *
     * @param provider 读取当前实体状态的函数。
     * @param mutator 写回实体状态的函数。
     * @param arranger 基于当前状态生成临时调整状态的函数。
     */
    inline fun <Value> StateKeeperBuilder.add(
        provider: (Owner) -> Value,
        crossinline mutator: (Owner, Value) -> Unit,
        arranger: (Value & Any) -> Value,
    ) {
        val owner = this@StateKeeperScope.owner

        val storedValue = provider(owner)
        if (storedValue != null) {
            val arrangedValue = arranger(storedValue)
            if (arrangedValue !== storedValue) {
                mutator(owner, arrangedValue)
            }
        }

        register(object : PreservedState {
            override fun restore() = mutator(owner, storedValue)
            override fun postProcess() {}
        })
    }

    /**
     * 定义一条简单的实体状态保存规则。
     *
     * @param provider 读取当前实体状态的函数。
     * @param mutator 写回实体状态的函数。
     */
    inline fun <Value> StateKeeperBuilder.add(provider: (Owner) -> Value, crossinline mutator: (Owner, Value) -> Unit) {
        val owner = this@StateKeeperScope.owner
        val storedValue = provider(owner)

        register(object : PreservedState {
            override fun restore() = mutator(owner, storedValue)
            override fun postProcess() {}
        })
    }

    /**
     * 通过委托给 [keeper] 为当前实体定义状态保存规则。
     */
    fun StateKeeperBuilder.add(keeper: StateKeeper<Owner, Context>, context: Context) {
        val owner = this@StateKeeperScope.owner
        register(keeper.prepare(owner, context))
    }

    /**
     * 定义一段 action 执行前运行的后处理逻辑。
     */
    fun StateKeeperBuilder.postProcess(block: PostProcessor) {
        register(object : PreservedState {
            override fun postProcess() = block()
            override fun restore() {}
        })
    }
}

/**
 * 使用委托 [keeper] 登记 [entity] 的状态保存规则。
 *
 * 当 [entity] 为 `null` 时不执行任何操作。
 */
internal fun <Entity : Any, Context : Any> StateKeeperBuilder.entity(
    entity: Entity?,
    keeper: StateKeeper<Entity, Context>,
    context: Context,
) {
    if (entity != null) {
        with(StateKeeperScope<Entity, Context>(entity)) {
            this@entity.add(keeper, context)
        }
    }
}

/**
 * 使用 [block] 登记 [entity] 的状态保存规则。
 *
 * 当 [entity] 为 `null` 时不执行任何操作。
 */
internal inline fun <Entity : Any, Context : Any> StateKeeperBuilder.entity(
    entity: Entity?,
    context: Context,
    block: StateKeeperScope<Entity, Context>.(Entity, Context) -> Unit,
) {
    if (entity != null) {
        StateKeeperScope<Entity, Context>(entity).block(entity, context)
    }
}

/**
 * 使用委托 [keeper] 顺序登记 [list] 中每个非空实体的状态保存规则。
 *
 * 当 [list] 为 `null` 时不执行任何操作。
 */
internal fun <Entity : Any, Context : Any> StateKeeperBuilder.entityList(
    list: List<Entity?>?,
    keeper: StateKeeper<Entity, Context>,
    context: Context,
) {
    if (list != null) {
        for (entity in list) {
            if (entity != null) {
                with(StateKeeperScope<Entity, Context>(entity)) { this@entityList.add(keeper, context) }
            }
        }
    }
}

/**
 * 使用 [block] 顺序登记 [list] 中每个非空实体的状态保存规则。
 *
 * 当 [list] 为 `null` 时不执行任何操作。
 */
internal inline fun <Entity : Any, Context : Any> StateKeeperBuilder.entityList(
    list: List<Entity?>?,
    context: Context,
    block: StateKeeperScope<Entity, Context>.(Entity, Context) -> Unit,
) {
    if (list != null) {
        for (entity in list) {
            if (entity != null) {
                StateKeeperScope<Entity, Context>(entity).block(entity, context)
            }
        }
    }
}

/**
 * 使用构建器 DSL 定义 [StateKeeper]。
 *
 * 这是创建状态保持器的主入口。[block] 为每个 owner 分别收集状态保存规则，嵌套 owner 可通过 [entity] 或 [entityList] 处理。
 */
internal fun <Owner : Any, Context : Any> stateKeeper(
    block: StateKeeperScope<Owner, Context>.(StateKeeperBuilder, Owner, Context) -> Unit,
): StateKeeper<Owner, Context> = StateKeeper { owner, context ->
    val states = mutableListOf<PreservedState>()

    val builder = object : StateKeeperBuilder {
        override fun register(state: PreservedState) {
            states += state
        }
    }

    val scope = StateKeeperScope<Owner, Context>(owner)
    block(scope, builder, owner, context)

    object : PreservedState {
        /**
         * 标记后处理是否已经执行，避免重复运行后处理规则。
         */
        private var isPostProcessed = false
        /**
         * 标记恢复是否已经执行，避免异常路径重复恢复状态。
         */
        private var isRestored = false

        /**
         * 执行所有登记的后处理规则。
         */
        override fun postProcess() {
            if (isPostProcessed) {
                return
            }

            isPostProcessed = true
            states.forEach { it.postProcess() }
        }

        /**
         * 执行所有登记的状态恢复规则。
         */
        override fun restore() {
            if (isRestored) {
                return
            }

            isRestored = true
            states.forEach { it.restore() }
        }
    }
}

/**
 * 显式保存对象局部状态并在失败时恢复的工具。
 *
 * 它不是完整对象快照机制，所有保存规则都必须通过 [stateKeeper] DSL 明确声明。生命周期为：准备并保存状态；对带 arranger 的规则
 * 应用临时调整；执行后处理；运行可能失败的 action；如果 action 失败，则恢复保存状态。
 *
 * @property provider 根据 owner 和上下文生成一份 [PreservedState] 的函数。
 *
 * @sample
 * ```
 * var state: PreservedState? = null
 *
 * try {
 *     state = keeper.prepare(owner)
 *     state.postProcess()
 *     action(owner)
 * } catch (e: Throwable) {
 *     state?.restore()
 * }
 * ```
 */
internal class StateKeeper<in Owner : Any, Context : Any>(val provider: (Owner, Context) -> PreservedState) {
    /**
     * 保存 [owner] 当前状态，并按规则执行必要的 arranger 调整。
     *
     * 后处理不会在准备阶段运行，调用方需要在 action 前显式调用 [PreservedState.postProcess]。
     */
    fun prepare(owner: Owner, context: Context): PreservedState {
        return provider(owner, context)
    }
}

/**
 * [StateKeeper] 保存出来的一组可后处理、可恢复状态。
 */
internal interface PreservedState {
    /**
     * 根据登记的 [PostProcessor] 执行状态后处理。
     *
     * 该函数必须在可能失败的 action 前调用。
     */
    fun postProcess()

    /**
     * 根据 [StateKeeper] 中定义的规则恢复状态。
     *
     * 该函数应在 action 失败时调用。
     */
    fun restore()
}

/**
 * 在 [keeper] 保护下解析 [target]。
 *
 * 该函数先准备保存状态，再调用 [prepareTarget] 做 action 前准备，随后运行后处理并执行 [action]。如果 action 抛出普通异常，
 * 会恢复保存状态后重新抛出；如果抛出 [PartialBodyAnalysisSuspendedException]，说明局部 body 分析正常挂起，不需要恢复。
 */
internal inline fun <Target : CfirElementWithResolveState, Context : Any, Result> resolveWithKeeper(
    target: Target,
    context: Context,
    keeper: StateKeeper<Target, Context>,
    prepareTarget: (Target) -> Unit = {},
    action: () -> Result,
): Result {
    var preservedState: PreservedState? = null

    try {
        preservedState = keeper.prepare(target, context)
        prepareTarget(target)
        preservedState.postProcess()
        return action()
    } catch (e: PartialBodyAnalysisSuspendedException) {
        // Partial body analysis is complete (and successful), no need for restoration
        throw e
    } catch (e: Throwable) {
        preservedState?.restore()
        throw e
    }
}
