/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators

import java.util.Collections

/**
 * 记录测试生成器发现的源码不一致文件。
 *
 * dry-run 模式下生成器不会写入磁盘，而是把需要更新的文件路径收集到该接口中；
 * 正常生成模式下可使用空实现避免额外状态。
 */
interface InconsistencyChecker {
    /**
     * 记录一个生成内容与磁盘内容不一致的文件。
     *
     * @param affectedFile 需要重新生成或提交的测试源码路径。
     */
    fun add(affectedFile: String)

    /**
     * 已记录的不一致文件路径列表。
     *
     * 实现可以选择线程安全集合，因为测试类生成会按测试类并行执行。
     */
    val affectedFiles: List<String>

    /**
     * 不一致检查器的命令行解析与实现选择入口。
     */
    companion object {
        /**
         * 判断生成器启动参数是否包含 dry-run 开关。
         */
        fun hasDryRunArg(args: Array<String>) = args.any { it == "dryRun" }

        /**
         * 根据 dry-run 状态选择真实收集器或空收集器。
         */
        fun inconsistencyChecker(dryRun: Boolean) = if (dryRun) DefaultInconsistencyChecker else EmptyInconsistencyChecker
    }
}

/**
 * dry-run 模式使用的线程安全不一致文件收集器。
 */
object DefaultInconsistencyChecker : InconsistencyChecker {
    /**
     * 并行生成测试类时共享的受影响文件列表。
     */
    private val files = Collections.synchronizedList(mutableListOf<String>())

    /**
     * 将一个需要重新生成的文件加入全局列表。
     */
    override fun add(affectedFile: String) {
        files.add(affectedFile)
    }

    /**
     * 返回当前 dry-run 过程中收集到的全部不一致文件。
     */
    override val affectedFiles: List<String>
        get() = files
}

/**
 * 非 dry-run 模式使用的空不一致检查器。
 *
 * 实际写入文件时不需要额外记录，因为生成器已经直接更新了目标源码文件。
 */
object EmptyInconsistencyChecker : InconsistencyChecker {
    /**
     * 忽略不一致文件记录请求。
     */
    override fun add(affectedFile: String) {
    }

    /**
     * 空实现始终返回空列表。
     */
    override val affectedFiles: List<String>
        get() = emptyList()
}
