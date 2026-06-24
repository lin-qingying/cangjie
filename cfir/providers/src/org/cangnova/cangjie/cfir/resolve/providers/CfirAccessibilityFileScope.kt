package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.name.FqName

/**
 * 当前文件可访问性上下文。
 *
 * Body resolve 入口设置当前文件，CST 计算和 extend 成员 scope 读取。
 * 文件 resolve 是单线程顺序处理，ThreadLocal 安全。
 */
object CfirAccessibilityFileScope {
    /**
     * 当前正在解析的文件。
     */
    val currentFile = ThreadLocal<CfirFile?>()

    /**
     * 返回当前文件；没有文件上下文时返回 `null`。
     */
    fun get(): CfirFile? = currentFile.get()

    /**
     * 当前 use-site 文件包名。
     *
     * extend 成员 scope 会把包名固化到缓存 key 和 scope 实例中，避免跨文件复用时
     * 重新读取 ThreadLocal 导致 private extend 成员导出面漂移。
     */
    fun currentPackageFqName(): FqName? = get()?.packageDirective?.packageFqName

    /**
     * 在 [file] 作为当前可访问性上下文的情况下执行 [block]，并在结束后恢复旧上下文。
     */
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
