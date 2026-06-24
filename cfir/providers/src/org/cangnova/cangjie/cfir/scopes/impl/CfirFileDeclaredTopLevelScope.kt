package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.scopes.CfirPackageScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 当前文件顶层声明 scope。
 *
 * 这个 scope 只暴露当前源码文件中已经存在的顶层声明，不依赖 symbol provider 的包级查询结果。
 * 这样可以保证同文件声明在名字解析时始终先于同包其他文件与默认导入生效，
 * 避免无包源码中的本地声明被 `std.core.*` 之类的默认导入同名符号抢占。
 *
 * @property file 当前文件。
 */
class CfirFileDeclaredTopLevelScope(
    private val file: CfirFile,
) : CfirPackageScope() {
    /**
     * 顶层 pattern variable 容器不直接参与名字解析；
     * 只有其内部 binding variable 才是当前文件真正可见的顶层 callable。
     */
    private val topLevelPatternBindingsByName: Map<Name, List<CfirCallableSymbol<*>>> = buildMap {
        file.declarations
            .asSequence()
            .filterIsInstance<CfirPatternVariable>()
            .flatMap { declaration -> declaration.pattern.bindingVariables().asSequence() }
            .forEach { bindingVariable ->
                put(bindingVariable.name, get(bindingVariable.name).orEmpty() + bindingVariable.symbol)
            }
    }

    /**
     * 当前文件顶层 class-like 声明索引。
     */
    private val classifiersByName: Map<Name, List<CfirClassLikeSymbol<*>>> = buildMap {
        file.declarations
            .asSequence()
            .filterIsInstance<CfirClassLikeDeclaration>()
            .forEach { declaration ->
                val symbol = declaration.symbol
                put(declaration.name, (get(declaration.name).orEmpty() + symbol))
            }
    }

    /**
     * 当前文件顶层函数索引。
     */
    private val functionsByName: Map<Name, List<CfirNamedFunctionSymbol>> = buildMap {
        file.declarations
            .asSequence()
            .filterIsInstance<CfirNamedFunction>()
            .forEach { declaration ->
                val symbol = declaration.symbol
                put(declaration.name, (get(declaration.name).orEmpty() + symbol))
            }
    }

    /**
     * 当前文件顶层属性索引。
     */
    private val propertiesByName: Map<Name, List<CfirPropertySymbol>> = buildMap {
        file.declarations
            .asSequence()
            .filterIsInstance<CfirProperty>()
            .forEach { declaration ->
                val symbol = declaration.symbol
                put(declaration.name, (get(declaration.name).orEmpty() + symbol))
            }
    }

    /**
     * 当前文件顶层 callable 索引。
     */
    private val callablesByName: Map<Name, List<CfirCallableSymbol<*>>> = buildMap {
        fun append(name: Name, symbol: CfirCallableSymbol<*>) {
            put(name, (get(name).orEmpty() + symbol))
        }

        file.declarations
            .asSequence()
            .filterIsInstance<CfirNamedFunction>()
            .forEach { declaration -> append(declaration.name, declaration.symbol) }

        file.declarations
            .asSequence()
            .filterIsInstance<CfirProperty>()
            .forEach { declaration -> append(declaration.name, declaration.symbol) }

        topLevelPatternBindingsByName.forEach { (name, symbols) ->
            symbols.forEach { symbol -> append(name, symbol) }
        }
    }

    /**
     * 返回当前文件可见的顶层 callable 名称。
     */
    override fun getCallableNames(): Set<Name> = callablesByName.keys

    /**
     * 返回当前文件可见的顶层 classifier 名称。
     */
    override fun getClassifierNames(): Set<Name> = classifiersByName.keys

    /**
     * 按名称处理当前文件顶层 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        classifiersByName[name].orEmpty().forEach(processor)
    }

    /**
     * 按名称处理当前文件顶层函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        functionsByName[name].orEmpty().forEach(processor)
    }

    /**
     * 按名称处理当前文件顶层属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        propertiesByName[name].orEmpty().forEach(processor)
    }

    /**
     * 按名称处理当前文件顶层 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        callablesByName[name].orEmpty().forEach(processor)
    }

    /**
     * 返回调试文本。
     */
    override fun toString(): String = "Current file top-level scope of /${file.packageDirective.packageFqName}"
}
