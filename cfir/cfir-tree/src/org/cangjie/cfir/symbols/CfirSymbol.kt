package org.cangjie.cfir.symbols

import org.cangjie.cfir.declarations.*

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
    val fir: D
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
        if (isBound) "CfirClassSymbol(${fir.name})" else "CfirClassSymbol(unbound)"
}

class CfirTypeAliasSymbol : CfirClassifierSymbol<CfirTypeAlias>() {
    override fun toString(): String =
        if (isBound) "CfirTypeAliasSymbol(${fir.name})" else "CfirTypeAliasSymbol(unbound)"
}

class CfirTypeParameterSymbol : CfirClassifierSymbol<CfirTypeParameter>() {
    override fun toString(): String =
        if (isBound) "CfirTypeParameterSymbol(${fir.name})" else "CfirTypeParameterSymbol(unbound)"
}

// ---- 可调用符号 ----

sealed class CfirCallableSymbol<D : CfirCallableDeclaration> : CfirSymbol<D>()

class CfirFunctionSymbol : CfirCallableSymbol<CfirFunction>() {
    override fun toString(): String =
        if (isBound) "CfirFunctionSymbol(${fir.name})" else "CfirFunctionSymbol(unbound)"
}

class CfirConstructorSymbol : CfirCallableSymbol<CfirConstructor>() {
    override fun toString(): String =
        if (isBound) "CfirConstructorSymbol" else "CfirConstructorSymbol(unbound)"
}

class CfirPropertySymbol : CfirCallableSymbol<CfirProperty>() {
    override fun toString(): String =
        if (isBound) "CfirPropertySymbol(${fir.name})" else "CfirPropertySymbol(unbound)"
}

class CfirVariableSymbol : CfirCallableSymbol<CfirVariable>() {
    override fun toString(): String =
        if (isBound) "CfirVariableSymbol(${fir.name})" else "CfirVariableSymbol(unbound)"
}

class CfirPatternVariableSymbol : CfirCallableSymbol<CfirPatternVariable>() {
    override fun toString(): String =
        if (isBound) "CfirPatternVariableSymbol(${fir.bindings.joinToString { it.name.asString() }})"
        else "CfirPatternVariableSymbol(unbound)"
}

class CfirValueParameterSymbol : CfirCallableSymbol<CfirValueParameter>() {
    override fun toString(): String =
        if (isBound) "CfirValueParameterSymbol(${fir.name})" else "CfirValueParameterSymbol(unbound)"
}

// ---- 其他符号 ----

class CfirFileSymbol : CfirSymbol<CfirFile>() {
    override fun toString(): String =
        if (isBound) "CfirFileSymbol(${fir.name})" else "CfirFileSymbol(unbound)"
}

class CfirExtendSymbol : CfirSymbol<CfirExtend>() {
    override fun toString(): String = "CfirExtendSymbol"
}

class CfirEnumEntrySymbol : CfirSymbol<CfirEnumEntry>() {
    override fun toString(): String =
        if (isBound) "CfirEnumEntrySymbol(${fir.name})" else "CfirEnumEntrySymbol(unbound)"
}
