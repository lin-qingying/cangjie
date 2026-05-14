package org.cangnova.cangjie.analysis.api.lightDeclarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjFile

/**
 * Analysis API 的轻量声明视图提供器。
 *
 * 该服务把 source / decompiled PSI 与公开 symbol 统一投影为 [CaLightDeclaration],
 * 供 analysis-tools、外围一致性检查和 IDE 辅助视图共享。
 *
 * 默认实现注册为项目级 IntelliJ Service,可通过 [getInstance] 获取。
 */
interface CaLightDeclarationProvider {
    /**
     * 从公开 [symbol] 构造对应的轻量声明视图。
     *
     * 若无法为该 symbol 投影(例如纯合成、缺失声明位置)则返回 `null`。
     */
    fun getLightDeclaration(symbol: CaSymbol): CaLightDeclaration?

    /**
     * 构造指定 [file] 内的顶层轻量声明视图列表。
     *
     * @param useSiteModule 可选 use-site module,用于在多模块场景下消歧。
     */
    fun getLightDeclarations(file: CjFile, useSiteModule: CaModule? = null): List<CaLightDeclaration>

    companion object {
        /**
         * 获取项目级别的 [CaLightDeclarationProvider]。
         */
        fun getInstance(project: Project): CaLightDeclarationProvider =
            project.service()
    }
}
