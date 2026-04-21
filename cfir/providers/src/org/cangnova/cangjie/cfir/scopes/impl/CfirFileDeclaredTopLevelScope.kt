package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.scopes.CfirPackageScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 当前文件顶层声明 scope。
 *
 * 这个 scope 只暴露当前源码文件中已经存在的顶层声明，不依赖 symbol provider 的包级查询结果。
 * 这样可以保证同文件声明在名字解析时始终先于同包其他文件与默认导入生效，
 * 避免无包源码中的本地声明被 `std.core.*` 之类的默认导入同名符号抢占。
 */
class CfirFileDeclaredTopLevelScope(
    private val file: CfirFile,
) : CfirPackageScope() {

    private val classifiersByName: Map<Name, List<CfirClassLikeSymbol<*>>> = buildMap {
        file.declarations
            .asSequence()
            .filterIsInstance<CfirClassLikeDeclaration>()
            .forEach { declaration ->
                val symbol = declaration.symbol
                put(declaration.name, (get(declaration.name).orEmpty() + symbol))
            }
    }

    private val functionsByName: Map<Name, List<CfirNamedFunctionSymbol>> = buildMap {
        file.declarations
            .asSequence()
            .filterIsInstance<CfirNamedFunction>()
            .forEach { declaration ->
                val symbol = declaration.symbol
                put(declaration.name, (get(declaration.name).orEmpty() + symbol))
            }
    }

    private val propertiesByName: Map<Name, List<CfirPropertySymbol>> = buildMap {
        file.declarations
            .asSequence()
            .filterIsInstance<CfirProperty>()
            .forEach { declaration ->
                val symbol = declaration.symbol
                put(declaration.name, (get(declaration.name).orEmpty() + symbol))
            }
    }

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
    }

    override fun getCallableNames(): Set<Name> = callablesByName.keys

    override fun getClassifierNames(): Set<Name> = classifiersByName.keys

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        classifiersByName[name].orEmpty().forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        functionsByName[name].orEmpty().forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        propertiesByName[name].orEmpty().forEach(processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        callablesByName[name].orEmpty().forEach(processor)
    }

    override fun toString(): String = "Current file top-level scope of /${file.packageDirective.packageFqName}"
}
