package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.constant.EvaluatedConstTracker

/**
 * 符号基类。每个声明对应一个唯一的符号实例。
 *
 * 符号是声明的稳定标识符，在解析过程中保持不变，
 * 即使声明节点本身因转换而被替换。
 *
 * 对应仓颉编译器中的 Symbol 系统。
 */
sealed class CfirSymbol<out D : CfirDeclaration> {
    private var _fir: @UnsafeVariance D? = null

    /** 指向对应的 CFIR 声明 */
    val cfir: D
        get() = _fir ?: error("Symbol is not bound to a declaration")

    /** 符号是否已绑定到声明 */
    val isBound: Boolean
        get() = _fir != null

    /** 绑定符号到声明（只能绑定一次） */
    fun bind(declaration: @UnsafeVariance D) {
        check(_fir == null) { "Symbol is already bound" }
        _fir = declaration
    }
}

// ---- 分类器符号 ----

sealed class CfirClassifierSymbol<D : CfirDeclaration> : CfirSymbol<D>()

class CfirClassSymbol : CfirClassifierSymbol<CfirClass>() {
    override fun toString(): String =
        if (isBound) "CfirClassSymbol(${cfir.name})" else "CfirClassSymbol(unbound)"
}

class CfirTypeAliasSymbol : CfirClassifierSymbol<CfirTypeAlias>() {
    override fun toString(): String =
        if (isBound) "CfirTypeAliasSymbol(${cfir.name})" else "CfirTypeAliasSymbol(unbound)"
}

class CfirTypeParameterSymbol : CfirClassifierSymbol<CfirTypeParameter>() {
    override fun toString(): String =
        if (isBound) "CfirTypeParameterSymbol(${cfir.name})" else "CfirTypeParameterSymbol(unbound)"
}

// ---- 可调用符号 ----

sealed class CfirCallableSymbol<D : CfirCallableDeclaration> : CfirSymbol<D>()

class CfirFunctionSymbol : CfirCallableSymbol<CfirFunction>() {
    override fun toString(): String =
        if (isBound) "CfirFunctionSymbol(${cfir.name})" else "CfirFunctionSymbol(unbound)"
}

class CfirMainFunctionSymbol : CfirCallableSymbol<CfirMainFunction>() {
    override fun toString(): String =
        if (isBound) "CfirMainFunctionSymbol" else "CfirMainFunctionSymbol(unbound)"
}

class CfirMacroDeclarationSymbol : CfirCallableSymbol<CfirMacroDeclaration>() {
    override fun toString(): String =
        if (isBound) "CfirMacroDeclarationSymbol(${cfir.name})" else "CfirMacroDeclarationSymbol(unbound)"
}

class CfirFinalizerSymbol : CfirCallableSymbol<CfirFinalizer>() {
    override fun toString(): String =
        if (isBound) "CfirFinalizerSymbol" else "CfirFinalizerSymbol(unbound)"
}

class CfirConstructorSymbol : CfirCallableSymbol<CfirConstructor>() {
    override fun toString(): String =
        if (isBound) "CfirConstructorSymbol" else "CfirConstructorSymbol(unbound)"
}

class CfirPropertySymbol : CfirCallableSymbol<CfirProperty>() {
    override fun toString(): String =
        if (isBound) "CfirPropertySymbol(${cfir.name})" else "CfirPropertySymbol(unbound)"
}

class CfirVariableSymbol : CfirCallableSymbol<CfirVariable>() {
    override fun toString(): String =
        if (isBound) "CfirVariableSymbol(${cfir.name})" else "CfirVariableSymbol(unbound)"
}

class CfirPatternVariableSymbol : CfirCallableSymbol<CfirPatternVariable>() {
    override fun toString(): String =
        if (isBound) "CfirPatternVariableSymbol(${cfir.pattern::class.simpleName})"
        else "CfirPatternVariableSymbol(unbound)"
}

class CfirValueParameterSymbol : CfirCallableSymbol<CfirValueParameter>() {
    override fun toString(): String =
        if (isBound) "CfirValueParameterSymbol(${cfir.name})" else "CfirValueParameterSymbol(unbound)"
}

// ---- 其他符号 ----

class CfirFileSymbol : CfirSymbol<CfirFile>() , EvaluatedConstTracker.Key{

    val sourceFile: CjSourceFile? get() = cfir.sourceFile

    override fun asStringBasedKey(): EvaluatedConstTracker.Key.StringBased? {
        if (!isBound) return null
        return sourceFile?.path?.let { EvaluatedConstTracker.Key.StringBased(it) }
    }

    override fun toString(): String =
        if (isBound) "CfirFileSymbol(${cfir.name})" else "CfirFileSymbol(unbound)"
}

class CfirExtendSymbol : CfirSymbol<CfirExtend>() {
    override fun toString(): String = "CfirExtendSymbol"
}

class CfirEnumConstructorSymbol : CfirSymbol<CfirEnumConstructor>() {
    override fun toString(): String =
        if (isBound) "CfirEnumConstructorSymbol(${cfir.name})" else "CfirEnumConstructorSymbol(unbound)"
}

class CfirInvalidDeclarationSymbol : CfirSymbol<CfirInvalidDeclaration>() {
    override fun toString(): String =
        if (isBound) "CfirInvalidDeclarationSymbol(${cfir.reason})" else "CfirInvalidDeclarationSymbol(unbound)"
}
