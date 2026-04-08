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
import org.cangnova.cangjie.psi.CjScript
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
                is CjScript -> resolveScriptSymbol(this)
                else -> error("Cannot build symbol for ${this::class}")
            }
        }

    private fun resolveParameterSymbol(parameter: CjParameter): CaDeclarationSymbol = with(this) { parameter.symbol }

    private fun resolveNamedFunctionSymbol(function: CjNamedFunction): CaDeclarationSymbol = with(this) { function.symbol }

    private fun resolveAnonymousFunctionSymbol(functionLiteral: CjFunctionLiteral): CaDeclarationSymbol = with(this) { functionLiteral.symbol }

    private fun resolveConstructorSymbol(constructor: CjConstructor<*>): CaDeclarationSymbol = with(this) { constructor.symbol }

    private fun resolveMacroSymbol(macroDeclaration: CjMacroDeclaration): CaDeclarationSymbol = with(this) { macroDeclaration.symbol }

    private fun resolveFinalizerSymbol(finalizer: CjFinalizer): CaDeclarationSymbol = with(this) { finalizer.symbol }

    private fun resolveTypeParameterSymbol(typeParameter: CjTypeParameter): CaDeclarationSymbol = with(this) { typeParameter.symbol }

    private fun resolveTypeAliasSymbol(typeAlias: CjTypeAlias): CaDeclarationSymbol = with(this) { typeAlias.symbol }

    private fun resolvePropertySymbol(property: CjProperty): CaDeclarationSymbol = with(this) { property.symbol }

    private fun resolvePropertyAccessorSymbol(accessor: CjPropertyAccessor): CaDeclarationSymbol = with(this) { accessor.symbol }

    private fun resolveFieldSymbol(field: CjFieldVariable): CaDeclarationSymbol = with(this) { field.symbol }

    private fun resolveEnumEntrySymbol(enumConstructor: CjEnumConstructor): CaDeclarationSymbol = with(this) { enumConstructor.symbol }

    private fun resolvePatternVariableSymbol(patternVariable: CjPatternVariable): CaDeclarationSymbol = with(this) { patternVariable.symbol }

    private fun resolvePatternBindingSymbol(bindingPattern: CjBindingPattern): CaDeclarationSymbol = with(this) { bindingPattern.symbol }

    private fun resolveExtendSymbol(extend: CjExtend): CaDeclarationSymbol = with(this) { extend.symbol }

    private fun resolveClassSymbol(typeStatement: CjTypeStatement): CaDeclarationSymbol = with(this) { typeStatement.classSymbol }

    private fun resolveScriptSymbol(script: CjScript): CaDeclarationSymbol = with(this) { script.symbol }
}
