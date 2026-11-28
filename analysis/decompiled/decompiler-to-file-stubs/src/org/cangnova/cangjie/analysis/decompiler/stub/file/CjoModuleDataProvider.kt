package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cfir.common.CfirModuleData

/**
 * `.cjo` binary 所属 CFIR module data 的解析入口。
 *
 * 该入口放在 file-stub 层，是因为 `.cjo` stub 构建需要真实 module owner；
 * 具体 owner 仍由上层 Analysis/low-level 装配提供，decompiler-to-psi 不直接依赖 low-level。
 *
 * 实现必须区分“结构内”与“结构外”：
 * - 结构内返回对应 library / builtins session 的 module data；
 * - 结构外（不属于项目结构中任何 library/builtins 模块的 `.cjo`，例如项目刚打开、SDK 尚未注册时
 *   索引里还没有该文件）返回 detached module data，让声明仍能物化。
 *
 * 因此实现不再以 `null` 表示“找不到 owner”：`null` 只会让 stub 构建静默失败并退回占位文本。
 */
interface CjoModuleDataProvider {
    /**
     * 返回指定 `.cjo` 二进制文件所属的 CFIR module data。
     * 同文件、同 metadata generation 必须与 getPackageContext 的 owner/binding 一致；
     * detached owner 同样由原 producer 缓存，不能为两个读取接口分别构造。
     */
    fun getModuleData(binaryFile: VirtualFile): CfirModuleData

    /** actual-file 内容、来源、owner、原 metadata binding 在同一次读取中确定。 */
    fun getPackageContext(
        binaryFile: VirtualFile,
        content: ByteArray,
        mode: org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode,
    ): org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext

    /** 调用方已确定实际 binary module/group 时保留该身份；来源仍必须属于该模块。 */
    fun getPackageContext(
        binaryFile: VirtualFile,
        content: ByteArray,
        mode: org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode,
        ownerModule: org.cangnova.cangjie.analysis.api.projectStructure.CaModule,
    ): org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext

    companion object {
        /**
         * 从 IntelliJ project service 容器中取得当前项目的 `.cjo` module data provider。
         */
        fun getInstance(project: Project): CjoModuleDataProvider = project.service()
    }
}
