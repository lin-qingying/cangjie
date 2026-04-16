package org.cangnova.cangjie.analysis.api.decompiled

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

abstract class CaBuiltinsVirtualFileProvider {
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance(): CaBuiltinsVirtualFileProvider {
            return requireNotNull(
                ApplicationManager.getApplication().getService(CaBuiltinsVirtualFileProvider::class.java),
            ) {
                "CaBuiltinsVirtualFileProvider is not registered in the current application container"
            }
        }
    }
}
