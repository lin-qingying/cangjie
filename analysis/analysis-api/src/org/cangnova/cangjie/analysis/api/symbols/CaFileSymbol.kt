package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 源文件的公开语义视图。
 *
 * 文件是仓颉中顶层声明的物理承载单元，也是包成员实际定义所在。
 * 它本身不是 [CaDeclarationSymbol]：文件没有可见性、模态等声明级属性，
 * 但它扮演"声明容器"和"具名实体"的角色。
 */
interface CaFileSymbol : CaSymbol, CaDeclarationContainerSymbol, CaNamedSymbol {
    /**
     * 当前文件符号对应的 PSI 文件节点。
     */
    val file: CjFile

    /**
     * 文件所属的包全限定名。
     */
    val packageFqName: FqName

    /**
     * 文件名作为符号名。
     *
     * 默认实现取 PSI 文件名（含扩展名），供渲染层、错误信息等使用。
     */
    override val name: Name
        get() = Name.identifier(file.name)
}
