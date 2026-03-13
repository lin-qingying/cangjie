package org.cangnova.cangjie.analysis.api

import com.intellij.openapi.project.Project

/**
 * 分析模块抽象（对齐 Kotlin 的 KaModule）。
 *
 * 表示一个编译/分析单元，定义了符号的解析范围。
 */
interface CaModule {
    val name: String
    val project: Project
}
