/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators

import org.jetbrains.kotlin.generators.dsl.TestGroup

/**
 * 测试源码生成器的公共抽象入口。
 *
 * 该抽象屏蔽 JUnit4、JUnit5 等具体输出格式差异，调用方只需要传入 DSL 中描述的测试类模型，
 * 由具体实现负责生成 Java 测试源码并按 dry-run 或实际写入模式返回变更结果。
 */
abstract class AbstractTestGenerator {
    /**
     * 根据一个测试类 DSL 模型生成并保存对应的测试源码。
     *
     * @param testClass DSL 中收集到的测试类定义，包含基类、目标生成类、测试数据模型和附加方法。
     * @param dryRun 为 true 时只比较目标文件内容是否需要更新，不实际写入磁盘。
     * @param allowGenerationOnTeamCity 为 true 时允许在 TeamCity 环境中直接生成文件。
     * @param mainClassName 触发生成的主类名，用于写入生成文件头部的来源说明。
     * @return 生成结果，记录目标文件路径以及内容是否发生变化。
     */
    abstract fun generateAndSave(
        testClass: TestGroup.TestClass,
        dryRun: Boolean,
        allowGenerationOnTeamCity: Boolean,
        mainClassName: String?,
    ): GenerationResult

    /**
     * 单个测试源码生成动作的结果。
     *
     * @property newFileGenerated 表示目标源码文件内容是否与生成内容不同；dry-run 模式下表示是否需要重新生成。
     * @property testSourceFilePath 生成器计算出的测试源码文件路径，用于报告需要提交或检查的文件。
     */
    data class GenerationResult(
        /**
         * 目标源码文件内容是否与生成内容不同。
         */
        val newFileGenerated: Boolean,
        /**
         * 生成器计算出的测试源码文件路径。
         */
        val testSourceFilePath: String,
    )
}
