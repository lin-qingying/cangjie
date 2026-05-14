package org.cangnova.cangjie.analysis.api.lightDeclarations

/**
 * Light declaration 的来源类别。
 *
 * 与 [CaLightDeclarationOrigin.kind] 配合,
 * 表明该声明是直接来自源 PSI、反编译产物,还是引擎合成。
 */
enum class CaLightDeclarationOriginKind {
    /**
     * 源码 PSI:对应工程中真实存在的仓颉源文件声明。
     */
    SOURCE_PSI,

    /**
     * 反编译 PSI:从二进制库 / cjo 反编译得到的声明视图。
     */
    DECOMPILED_PSI,

    /**
     * 合成声明:由 Analysis API 引擎本身生成,无对应真实声明 PSI。
     */
    SYNTHETIC,
}
