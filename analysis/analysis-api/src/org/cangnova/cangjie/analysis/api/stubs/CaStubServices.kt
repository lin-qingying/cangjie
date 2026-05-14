package org.cangnova.cangjie.analysis.api.stubs

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * Stub 索引的统一门面。
 *
 * 把"按文件"、"按包"、"按类"三种 stub 查询入口聚合在一处,
 * 由 [getInstance] 在 [Project] 范围提供单例,
 * 供 Analysis API 的其他组件直接消费。
 */
interface CaStubIndexFacade {
    /** 按文件维度的 stub 查询入口。 */
    val fileProvider: CaStubFileProvider

    /** 按包维度的 stub 查询入口。 */
    val packageIndex: CaStubPackageIndex

    /** 指定类的成员名集合(stub 维度)。 */
    fun getClassMemberNames(classId: ClassId): Set<Name>

    companion object {
        /** 获取当前 [Project] 上的 stub 门面实例。 */
        fun getInstance(project: Project): CaStubIndexFacade = project.service()
    }
}
