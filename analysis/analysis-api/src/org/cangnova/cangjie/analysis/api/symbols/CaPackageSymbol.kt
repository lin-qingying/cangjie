package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 包的公开语义视图。
 *
 * 包是仓颉中跨文件的命名空间聚合体。它不是 [CaDeclarationSymbol]：
 * 包没有源码中"单个声明节点"承载，也没有可见性/模态等声明级属性；
 * 但具备稳定的全限定名身份，作为符号查找的根入口。
 */
interface CaPackageSymbol : CaSymbol, CaNamedSymbol {
    /**
     * 包的全限定名。
     */
    val fqName: FqName

    /**
     * 包名的短名，默认从 [fqName] 推导。
     *
     * 对根包（无父包）返回特殊名称。
     */
    override val name: Name
        get() = fqName.shortNameOrSpecial()
}
