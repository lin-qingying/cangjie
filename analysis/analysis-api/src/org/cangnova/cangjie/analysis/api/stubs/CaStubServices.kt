package org.cangnova.cangjie.analysis.api.stubs

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

interface CaStubIndexFacade {
    val fileProvider: CaStubFileProvider

    val packageIndex: CaStubPackageIndex

    fun getClassMemberNames(classId: ClassId): Set<Name>

    companion object {
        fun getInstance(project: Project): CaStubIndexFacade = project.service()
    }
}
