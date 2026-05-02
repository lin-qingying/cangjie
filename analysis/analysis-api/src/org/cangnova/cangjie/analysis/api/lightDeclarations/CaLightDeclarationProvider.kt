package org.cangnova.cangjie.analysis.api.lightDeclarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * Analysis API 的轻量声明视图提供器。
 *
 * 该服务把 source/decompiled PSI 与公开 symbol 统一投影为 [CaLightDeclaration]，
 * 供 analysis-tools、外围一致性检查和 IDE 辅助视图共享。
 */
interface CaLightDeclarationProvider {
    /**
     * 从公开 symbol 构造对应的轻量声明视图。
     */
    fun getLightDeclaration(symbol: CaSymbol): CaLightDeclaration?

    /**
     * 构造指定文件内的顶层轻量声明视图。
     */
    fun getLightDeclarations(file: CjFile, useSiteModule: CaModule? = null): List<CaLightDeclaration>

    /**
     * 构造指定模块内的全部顶层轻量声明视图。
     */
    fun getLightDeclarations(module: CaModule): List<CaLightDeclaration>

    /**
     * 构造指定包的轻量声明视图。
     */
    fun getPackageLightDeclaration(packageFqName: FqName, useSiteModule: CaModule): CaLightDeclaration?

    /**
     * 在指定包内按短名查询顶层轻量声明视图。
     */
    fun findLightDeclarations(packageFqName: FqName, name: Name, useSiteModule: CaModule): List<CaLightDeclaration>

    companion object {
        fun getInstance(project: Project): CaLightDeclarationProvider =
            project.service()
    }
}
