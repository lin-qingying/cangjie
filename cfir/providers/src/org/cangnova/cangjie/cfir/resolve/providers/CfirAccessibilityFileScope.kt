package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * 当前文件可访问性上下文。
 *
 * Body resolve 入口设置当前文件，CST 计算和 extend 成员 scope 读取。
 * 文件 resolve 是单线程顺序处理，ThreadLocal 安全。
 */
object CfirAccessibilityFileScope {
     val currentFile = ThreadLocal<CfirFile?>()

    fun get(): CfirFile? = currentFile.get()

    inline fun <T> with(file: CfirFile?, block: () -> T): T {
        val prev = currentFile.get()
        currentFile.set(file)
        return try {
            block()
        } finally {
            currentFile.set(prev)
        }
    }
}
