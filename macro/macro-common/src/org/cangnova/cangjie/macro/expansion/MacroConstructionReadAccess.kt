package org.cangnova.cangjie.macro.expansion

import org.cangnova.cangjie.config.CompilerConfigurationKey

/**
 * 为宏构造中必须读取 PSI 的短阶段提供调度边界。
 *
 * IDE 应把 PSI fragment reparse、stable splice 和读取 PSI source 的快照哈希分别放入读访问；
 * 宏执行器与宏包编译不经过此接口，避免外部进程等待期间长期持有读锁。
 *
 * 该类型放在 `macro:macro-common`，使 `analysis:macro-expansion` 能声明读访问边界
 * 而不依赖 `compiler:frontend`；前端通过类型别名复用同一类型。
 */
interface MacroConstructionReadAccess {
    companion object {
        /** 宿主与 frontend 共享同一个配置键，保证实际 reparse/splice 读取受同一 generation 屏障保护。 */
        val CONFIGURATION_KEY: CompilerConfigurationKey<MacroConstructionReadAccess> =
            CompilerConfigurationKey.create("MACRO_CONSTRUCTION_READ_ACCESS")
    }

    /** 在宿主定义的读访问中计算 [action]。 */
    fun <T> compute(action: () -> T): T

    /** 不要求 PSI 访问的编译器调用默认直接运行。 */
    object Direct : MacroConstructionReadAccess {
        override fun <T> compute(action: () -> T): T = action()
    }
}
