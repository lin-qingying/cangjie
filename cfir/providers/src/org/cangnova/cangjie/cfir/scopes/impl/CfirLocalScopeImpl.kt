package org.cangnova.cangjie.cfir.scopes.impl

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.PersistentMultimap

/**
 * 局部作用域：解析函数体/代码块内逐步引入的声明。
 *
 * 对齐 Kotlin K2 `FirLocalScope`，persistent/immutable 实现，每次 `storeXxx` 返回新实例。
 * 字段沿用仓颉语义命名：
 *  - `variables` 保存函数/构造器参数、字段变量、模式绑定变量等局部绑定；
 *  - `properties` 保存需要按属性语义进入 tower 的局部声明，例如 catch 参数；
 *  - `functions` 支持局部函数重载（仓颉允许同作用域内同名不同参的函数重载，见
 *    `manual/function_overloading.md` Scenario 4）；
 *  - `classLikeSymbols` 保存局部 class-like 声明（class/interface/struct/enum/type alias）。
 *
 * 变量与函数在仓颉里不允许同名（checker 层报错），但 scope 层分栏保存，不由 scope 做冲突检查。
 *
 * @property variables 局部变量符号索引。
 * @property properties 局部属性符号索引。
 * @property functions 局部函数符号索引。
 * @property classLikeSymbols 局部 class-like 符号索引。
 * @property useSiteSession 当前 scope 所属 use-site session。
 */
class CfirLocalScope private constructor(
    /**
     * 局部变量符号索引。
     */
    val variables: PersistentMap<Name, CfirVariableSymbol<*>>,
    /**
     * 局部属性符号索引。
     */
    val properties: PersistentMap<Name, CfirPropertySymbol>,
    /**
     * 局部函数符号索引，允许同名函数重载。
     */
    val functions: PersistentMultimap<Name, CfirNamedFunctionSymbol>,
    /**
     * 局部 class-like 符号索引。
     */
    val classLikeSymbols: PersistentMap<Name, CfirClassLikeSymbol<*>>,
    /**
     * 当前局部 scope 所属的 use-site session。
     */
    val useSiteSession: CfirSession,
) : CfirContainingNamesAwareScope() {

    /**
     * 创建空局部 scope。
     */
    constructor(session: CfirSession) : this(
        persistentMapOf(),
        persistentMapOf(),
        PersistentMultimap(),
        persistentMapOf(),
        session,
    )

    /**
     * 存入局部 class-like 或 typealias 声明。
     */
    fun storeClassOrTypeAlias(classLikeDeclaration: CfirClassLikeDeclaration, session: CfirSession): CfirLocalScope {
        val name = classLikeDeclaration.name
        return CfirLocalScope(
            variables, properties, functions, classLikeSymbols.put(name, classLikeDeclaration.symbol), session
        )
    }

    /**
     * 存入局部函数声明。
     */
    fun storeFunction(function: CfirNamedFunction, session: CfirSession): CfirLocalScope {
        return CfirLocalScope(
            variables, properties, functions.put(function.name, function.symbol), classLikeSymbols, session
        )
    }

    /**
     * 存入局部变量声明。
     */
    fun storeVariable(variable: CfirVariable, session: CfirSession): CfirLocalScope {
        val name = when (variable) {
            is CfirFieldVariable -> variable.name
            is CfirPatternBindingVariable -> variable.name
            // PatternVariable 本身不直接入作用域，
            // 它内部的 PatternBindingVariable 才是真正的绑定名
            is CfirPatternVariable -> error(
                "CfirPatternVariable should not be stored directly; " +
                        "store its PatternBindingVariable children instead"
            )
            is CfirValueParameter -> variable.name
        }
        return CfirLocalScope(
            variables.put(name, variable.symbol), properties, functions, classLikeSymbols, session
        )
    }

    /**
     * 存入局部属性声明。
     */
    fun storeProperty(property: CfirProperty, session: CfirSession): CfirLocalScope {
        return CfirLocalScope(
            variables, properties.put(property.name, property.symbol), functions, classLikeSymbols, session
        )
    }

    /**
     * 按名称处理局部函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        for (function in functions[name]) {
            processor(function)
        }
    }

    /**
     * 按名称处理局部变量。
     */
    override fun processVariablesByName(name: Name, processor: (CfirVariableSymbol<*>) -> Unit) {
        val variable = variables[name]
        if (variable != null) {
            processor(variable)
        }
    }

    /**
     * 按名称处理局部属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        val property = properties[name]
        if (property != null) {
            processor(property)
        }
    }

    /**
     * 按名称处理局部 class-like 声明。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        val classLikeSymbol = classLikeSymbols[name]
        if (classLikeSymbol != null) {
            processor(classLikeSymbol)
        }
    }

    /**
     * 按名称处理所有局部 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        variables[name]?.let(processor)
        properties[name]?.let(processor)
        for (function in functions[name]) {
            processor(function)
        }
    }

    /**
     * 对齐 K2：快速判定当前 scope 是否**可能**包含指定名字。
     * 局部作用域三栏命中任一即返回 true，供 tower resolve 的热路径提前跳过空 scope。
     */
    override fun mayContainName(name: Name): Boolean {
        return variables.containsKey(name) ||
                properties.containsKey(name) ||
                functions[name].isNotEmpty() ||
                classLikeSymbols.containsKey(name)
    }

    /**
     * 返回局部 callable 名称集合。
     */
    override fun getCallableNames(): Set<Name> = variables.keys + properties.keys + functions.keys

    /**
     * 返回局部 classifier 名称集合。
     */
    override fun getClassifierNames(): Set<Name> = classLikeSymbols.keys

    /**
     * 局部 scope 不支持跨 session 复用。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirContainingNamesAwareScope? {
        return null
    }
}
