package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.scopes.CfirClassScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * class-like symbol 的声明成员 scope，对齐 Kotlin FIR declared-member scope 模型。
 */
class CfirClassDeclaredMemberScope(
    /**
     * 当前 scope 所属 class-like symbol。
     */
    private val classSymbol: CfirClassLikeSymbol<*>,
) : CfirClassScope() {

    /**
     * 声明成员索引。
     */
    private val memberIndex: MemberIndex by lazy { buildIndex(classSymbol.cfir.declarations) }

    /**
     * 返回声明成员 callable 名称集合。
     */
    override fun getCallableNames(): Set<Name> = memberIndex.callableNames

    /**
     * 返回声明成员 classifier 名称集合。
     */
    override fun getClassifierNames(): Set<Name> = memberIndex.classifierNames

    /**
     * 按名称处理声明 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        memberIndex.classifiers[name]?.forEach(processor)
    }

    /**
     * 按名称处理声明函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
    }

    /**
     * 按名称处理声明属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        memberIndex.properties[name]?.forEach(processor)
    }

    /**
     * 按名称处理声明 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        memberIndex.enumConstructors[name]?.forEach(processor)
        memberIndex.functions[name]?.forEach(processor)
        memberIndex.properties[name]?.forEach(processor)
        memberIndex.variables[name]?.forEach(processor)
    }

    /**
     * 处理声明构造器。
     */
    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
        memberIndex.constructors.forEach(processor)
    }

    /**
     * class-like 声明成员索引。
     */
    private class MemberIndex(
        /**
         * classifier 短名到 class-like symbol 列表的索引。
         */
        val classifiers: Map<Name, List<CfirClassLikeSymbol<*>>>,
        /**
         * 当前 class-like 直接声明的构造器列表。
         */
        val constructors: List<CfirConstructorSymbol>,
        /**
         * enum constructor 短名到 symbol 列表的索引。
         */
        val enumConstructors: Map<Name, List<CfirEnumConstructorSymbol>>,
        /**
         * 函数短名到函数 symbol 列表的索引。
         */
        val functions: Map<Name, List<CfirNamedFunctionSymbol>>,
        /**
         * 属性短名到属性 symbol 列表的索引。
         */
        val properties: Map<Name, List<CfirPropertySymbol>>,
        /**
         * 字段变量短名到变量 symbol 列表的索引。
         */
        val variables: Map<Name, List<CfirVariableSymbol<*>>>,
    ) {
        /**
         * callable 名称集合。
         */
        val callableNames: Set<Name> = buildSet {
            addAll(enumConstructors.keys)
            addAll(functions.keys)
            addAll(properties.keys)
            addAll(variables.keys)
        }

        /**
         * classifier 名称集合。
         */
        val classifierNames: Set<Name> = classifiers.keys
    }

    /**
     * 从声明列表构建成员索引。
     */
    private fun buildIndex(declarations: List<CfirDeclaration>): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>()
        val constructors = mutableListOf<CfirConstructorSymbol>()
        val enumConstructors = HashMap<Name, MutableList<CfirEnumConstructorSymbol>>()
        val functions = HashMap<Name, MutableList<CfirNamedFunctionSymbol>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()
        val variables = HashMap<Name, MutableList<CfirVariableSymbol<*>>>()

        for (declaration in declarations) {
            when (declaration) {
                is CfirClassLikeDeclaration -> {
                    val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: continue
                    classifiers.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
                }

                is CfirConstructor -> {
                    // 静态初始化器（static init，官方 static.init）不是实例构造器，不能作为
                    // A<T>() 等构造调用的候选，否则会干扰隐式默认构造器的解析。
                    if (!declaration.status.isStatic) {
                        constructors += declaration.symbol
                    }
                }

                is CfirNamedFunction -> {
                    val symbol = declaration.symbol ?: continue
                    val callableName = declaration.callableNameOrNull() ?: continue
                    functions.getOrPut(callableName) { mutableListOf() }.add(symbol)
                }

                is CfirEnumConstructor -> {
                    val symbol = declaration.symbol as? CfirEnumConstructorSymbol ?: continue
                    enumConstructors.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                is CfirProperty -> {
                    val symbol = declaration.symbol as? CfirPropertySymbol ?: continue
                    properties.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                is CfirFieldVariable -> {
                    val symbol = declaration.symbol as? CfirVariableSymbol<*> ?: continue
                    variables.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                else -> Unit
            }
        }

        val (visibleProperties, visibleVariables) = filterShadowedValueDeclarations(properties, variables)

        return MemberIndex(
            classifiers = classifiers,
            constructors = constructors,
            enumConstructors = enumConstructors,
            functions = functions,
            properties = visibleProperties,
            variables = visibleVariables,
        )
    }

    /**
     * 仓颉官方 PreCheck 对同名值成员先报告重定义，然后普通引用解析仍以先声明成员为目标。
     *
     * CFIR 仍保留完整 declarations 供冲突检查遍历；这里仅收窄 declared member scope
     * 暴露给普通 lookup 的值成员集合，避免后续 overload resolver 把已重定义成员建模为歧义使用。
     */
    private fun filterShadowedValueDeclarations(
        properties: Map<Name, List<CfirPropertySymbol>>,
        variables: Map<Name, List<CfirVariableSymbol<*>>>,
    ): Pair<Map<Name, List<CfirPropertySymbol>>, Map<Name, List<CfirVariableSymbol<*>>>> {
        val visibleProperties = linkedMapOf<Name, List<CfirPropertySymbol>>()
        val visibleVariables = linkedMapOf<Name, List<CfirVariableSymbol<*>>>()

        for (name in properties.keys + variables.keys) {
            val valueMembers = buildList {
                properties[name].orEmpty().forEachIndexed { index, symbol ->
                    add(IndexedValueMember(symbol, ValueMemberKind.PROPERTY, index))
                }
                variables[name].orEmpty().forEachIndexed { index, symbol ->
                    add(IndexedValueMember(symbol, ValueMemberKind.VARIABLE, index))
                }
            }
            if (valueMembers.isEmpty()) continue

            val selected = valueMembers.minWith(
                compareBy<IndexedValueMember> { it.symbol.declarationStartOffset() }
                    .thenBy { it.kind.ordinal }
                    .thenBy { it.index },
            )
            when (selected.kind) {
                ValueMemberKind.PROPERTY -> visibleProperties[name] = listOf(selected.symbol as CfirPropertySymbol)
                ValueMemberKind.VARIABLE -> visibleVariables[name] = listOf(selected.symbol as CfirVariableSymbol<*>)
            }
        }

        return visibleProperties to visibleVariables
    }

    /**
     * 带声明顺序信息的值成员。
     */
    private data class IndexedValueMember(
        /**
         * 参与同名值成员选择的 callable symbol。
         */
        val symbol: CfirCallableSymbol<*>,
        /**
         * 值成员分类，用于在同偏移时稳定选择属性或变量。
         */
        val kind: ValueMemberKind,
        /**
         * 同一分类内的声明顺序下标。
         */
        val index: Int,
    )

    /**
     * 值成员种类。
     */
    private enum class ValueMemberKind {
        PROPERTY,
        VARIABLE,
    }

    /**
     * 返回 callable 声明起始偏移。
     */
    private fun CfirCallableSymbol<*>.declarationStartOffset(): Int =
        if (isBound) cfir.source?.startOffset ?: Int.MAX_VALUE else Int.MAX_VALUE

    /**
     * 返回调试文本。
     */
    override fun toString(): String {
        return "Declared member scope of ${classSymbol.classId}"
    }
}
