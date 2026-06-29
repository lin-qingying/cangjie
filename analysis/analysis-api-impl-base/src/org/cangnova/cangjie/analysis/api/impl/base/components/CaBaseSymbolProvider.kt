package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolProvider
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 对声明 PSI 的统一分派层。
 *
 * 这层只负责把宽泛的 `CjDeclaration.symbol` 请求路由到更具体的 PSI 入口，
 * 不承担任何后端恢复或缓存逻辑。
 */
abstract class CaBaseSymbolProvider<T : CaSession> : CaBaseSessionComponent<T>(), CaSymbolProvider {
    /**
     * 将任意仓颉声明 PSI 分派为对应的 Analysis API 声明符号。
     */
    override val CjDeclaration.symbol: CaDeclarationSymbol
        get() = withValidityAssertion {
            when (this) {
                is CjBindingPattern -> resolvePatternBindingSymbol(this)
                is CjEnumConstructor -> resolveEnumEntrySymbol(this)

                is CjParameter -> resolveParameterSymbol(this)
                is CjNamedFunction -> resolveNamedFunctionSymbol(this)
                is CjFunctionLiteral -> resolveAnonymousFunctionSymbol(this)
                is CjConstructor<*> -> resolveConstructorSymbol(this)
                is CjMacroDeclaration -> resolveMacroSymbol(this)
                is CjFinalizer -> resolveFinalizerSymbol(this)
                is CjTypeParameter -> resolveTypeParameterSymbol(this)
                is CjTypeAlias -> resolveTypeAliasSymbol(this)
                is CjProperty -> resolvePropertySymbol(this)
                is CjPropertyAccessor -> resolvePropertyAccessorSymbol(this)
                is CjFieldVariable -> resolveFieldSymbol(this)
                is CjPatternVariable -> resolvePatternVariableSymbol(this)
                is CjExtend -> resolveExtendSymbol(this)
                is CjTypeStatement -> resolveClassSymbol(this)
                else -> error("Cannot build symbol for ${this::class}")
            }
        }

    /**
     * 解析参数声明符号。
     */
    private fun resolveParameterSymbol(parameter: CjParameter): CaDeclarationSymbol = with(this) { parameter.symbol }

    /**
     * 解析命名函数声明符号。
     */
    private fun resolveNamedFunctionSymbol(function: CjNamedFunction): CaDeclarationSymbol = with(this) { function.symbol }

    /**
     * 解析匿名函数声明符号。
     */
    private fun resolveAnonymousFunctionSymbol(functionLiteral: CjFunctionLiteral): CaDeclarationSymbol = with(this) { functionLiteral.symbol }

    /**
     * 解析构造器声明符号。
     */
    private fun resolveConstructorSymbol(constructor: CjConstructor<*>): CaDeclarationSymbol = with(this) { constructor.symbol }

    /**
     * 解析宏声明符号。
     */
    private fun resolveMacroSymbol(macroDeclaration: CjMacroDeclaration): CaDeclarationSymbol = with(this) { macroDeclaration.symbol }

    /**
     * 解析终结器声明符号。
     */
    private fun resolveFinalizerSymbol(finalizer: CjFinalizer): CaDeclarationSymbol = with(this) { finalizer.symbol }

    /**
     * 解析类型参数声明符号。
     */
    private fun resolveTypeParameterSymbol(typeParameter: CjTypeParameter): CaDeclarationSymbol = with(this) { typeParameter.symbol }

    /**
     * 解析类型别名声明符号。
     */
    private fun resolveTypeAliasSymbol(typeAlias: CjTypeAlias): CaDeclarationSymbol = with(this) { typeAlias.symbol }

    /**
     * 解析属性声明符号。
     */
    private fun resolvePropertySymbol(property: CjProperty): CaDeclarationSymbol = with(this) { property.symbol }

    /**
     * 解析属性访问器声明符号。
     */
    private fun resolvePropertyAccessorSymbol(accessor: CjPropertyAccessor): CaDeclarationSymbol = with(this) { accessor.symbol }

    /**
     * 解析字段变量声明符号。
     */
    private fun resolveFieldSymbol(field: CjFieldVariable): CaDeclarationSymbol = with(this) { field.symbol }

    /**
     * 解析枚举项声明符号。
     */
    private fun resolveEnumEntrySymbol(enumConstructor: CjEnumConstructor): CaDeclarationSymbol = with(this) { enumConstructor.symbol }

    /**
     * 解析模式变量声明符号。
     */
    private fun resolvePatternVariableSymbol(patternVariable: CjPatternVariable): CaDeclarationSymbol = with(this) { patternVariable.symbol }

    /**
     * 解析绑定模式声明符号。
     */
    private fun resolvePatternBindingSymbol(bindingPattern: CjBindingPattern): CaDeclarationSymbol = with(this) { bindingPattern.symbol }

    /**
     * 解析扩展声明符号。
     */
    private fun resolveExtendSymbol(extend: CjExtend): CaDeclarationSymbol = with(this) { extend.symbol }

    /**
     * 解析类型声明符号。
     */
    private fun resolveClassSymbol(typeStatement: CjTypeStatement): CaDeclarationSymbol = with(this) { typeStatement.classSymbol }

}
