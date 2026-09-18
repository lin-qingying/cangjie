package org.cangnova.cangjie.frontend.sources

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.compileCjd
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.macro.file.CangJieMacroCallFileType

/**
 * 两条前端源收集路径共用的文件分类。非仓颉文件返回 null，留给调用方的插件注册和诊断流程。
 *
 * FileType 优先于文件名；多段后缀只用完整文件名识别，不能把 `.cj.d` 的 `extension == "d"`
 * 当成文件种类。宏调用类型必须先于其 SOURCE 基类识别。
 */
internal fun VirtualFile.cangjieSourceKind(): CjSourceKind? = when (fileType) {
    is CangJieDeclarationFileType -> CjSourceKind.DECLARATION
    is CangJieMacroCallFileType -> CjSourceKind.MACRO_CALL
    is CangJieFileType -> CjSourceKind.SOURCE
    else -> when (val kind = CjSourceKind.fromFileName(name)) {
        CjSourceKind.DECLARATION, CjSourceKind.MACRO_CALL -> kind
        CjSourceKind.SOURCE -> kind.takeIf { extension == CangJieFileType.EXTENSION }
    }
}

/** 编译输入互斥策略；文件的可识别性不等于它可以参与当前编译。 */
internal fun CompilerConfiguration.acceptsCangjieSource(kind: CjSourceKind): Boolean = when (kind) {
    CjSourceKind.SOURCE -> !compileCjd
    CjSourceKind.DECLARATION -> compileCjd
    CjSourceKind.MACRO_CALL -> false
}
