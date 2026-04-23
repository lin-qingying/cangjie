package org.cangnova.cangjie.analysis.api.components

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

interface CaDefaultImportsProvider  {
    val defaultImports: CaDefaultImports

    public companion object {
        public fun getService(project: Project): CaDefaultImportsProvider =
            project.service()
    }
}
