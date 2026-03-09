package org.cangnova.cangjie.icon

import com.intellij.openapi.application.ApplicationManager
import javax.swing.Icon

abstract class CangJieIconProviderService {
    abstract fun getFileIcon(): Icon?
    abstract fun getBuiltInFileIcon(): Icon?

    class CompilerCangJieFileIconProviderService : CangJieIconProviderService() {
        override fun getFileIcon(): Icon? = null
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
