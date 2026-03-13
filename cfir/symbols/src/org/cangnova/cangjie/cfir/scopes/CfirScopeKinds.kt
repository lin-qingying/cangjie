package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/** 包级 scope，解析包内的顶级声明 */
interface CfirPackageScope : CfirScope

/** 类级 scope，解析类内部的成员声明 */
interface CfirClassScope : CfirScope

/** import scope，解析通过 import 引入的声明 */
interface CfirImportScope : CfirScope

/** 局部 scope，解析函数体/代码块内的局部声明 */
interface CfirLocalScope : CfirScope

/** extend scope，解析 extend 声明引入的成员 */
interface CfirExtendScope : CfirScope

/** 类型参数 scope，解析泛型类/函数中的类型参数名称 */
interface CfirTypeParameterScope : CfirScope

/**
 * 组合 scope，将多个 scope 合并为一个。
 */
class CfirCompositeScope(private val scopes: List<CfirScope>) : CfirScope {

    constructor(vararg scopes: CfirScope) : this(scopes.toList())

    override fun processClassifiersByName(name: Name, processor: (CfirClassSymbol) -> Unit) {
        for (scope in scopes) scope.processClassifiersByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol) -> Unit) {
        for (scope in scopes) scope.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        for (scope in scopes) scope.processPropertiesByName(name, processor)
    }
}
