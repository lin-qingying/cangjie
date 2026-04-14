package org.cangnova.cangjie.idea.references

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project

interface CangJieCDocResolutionStrategyProviderService : Disposable {
    fun shouldUseExperimentalStrategy(): Boolean

    companion object {
        fun getService(project: Project): CangJieCDocResolutionStrategyProviderService? =
            project.serviceOrNull()
    }
}