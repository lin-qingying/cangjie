package org.cangnova.cangjie.analysis.api.projectStructure

/**
 * 源码模块显式声明的 CJMP 编译角色，独立于目标架构和语言版本。
 * 平台/standalone 配置持有该事实，每次前端 session 重建都从模块输入读取。
 */
enum class CaCjmpCompilationMode {
    /** 显式关闭 CJMP，与模块未指定编译角色不同。 */
    NONE,
    /** 编译 common part。 */
    COMMON,
    /** 编译 common part 的 specific 实现。 */
    SPECIFIC,
}
