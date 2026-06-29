package org.cangnova.cangjie.icon

import com.intellij.openapi.application.ApplicationManager
import javax.swing.Icon

/**
 * 表示 `CangJieIconProviderService`，承载PSI 模块中的语法节点、索引桩或辅助模型。
 */
abstract class CangJieIconProviderService {
    /**
     * 提供 `getFileIcon` 操作，封装PSI 模块节点的访问、构造或判断逻辑。
     */
    abstract fun getFileIcon(): Icon?
    /**
     * 提供 `getBuiltInFileIcon` 操作，封装PSI 模块节点的访问、构造或判断逻辑。
     */
    abstract fun getBuiltInFileIcon(): Icon?

    /**
     * 表示 `CompilerCangJieFileIconProviderService`，承载PSI 模块中的语法节点、索引桩或辅助模型。
     */
    class CompilerCangJieFileIconProviderService : CangJieIconProviderService() {
        /**
         * 实现 `getFileIcon` 的PSI 模块协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun getFileIcon(): Icon? = null
        /**
         * 实现 `getBuiltInFileIcon` 的PSI 模块协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun getBuiltInFileIcon(): Icon? = null
    }

    companion object {
        @JvmStatic
        fun getInstance(): CangJieIconProviderService {
            val service = ApplicationManager.getApplication()?.getService(CangJieIconProviderService::class.java)
            return service ?: CompilerCangJieFileIconProviderService()
        }
    }
}
