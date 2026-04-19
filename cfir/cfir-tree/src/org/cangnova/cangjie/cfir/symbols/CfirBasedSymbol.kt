package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolvedStatus
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.cfir.withCfirSymbolIdEntry
import org.cangnova.cangjie.constant.EvaluatedConstTracker
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * 符号基类。每个声明对应一个唯一的符号实例。
 *
 * 符号是声明的稳定标识符，在解析过程中保持不变，
 * 即使声明节点本身因转换而被替换。
 */
sealed class CfirBasedSymbol<out D : CfirDeclaration> {
    private var _cfir: @UnsafeVariance D? = null

    /** 指向对应的 CFIR 声明，未绑定时访问会抛出异常。 */
    val cfir: D
        get() = _cfir
            ?: errorWithAttachment("Cfir is not initialized for ${this::class}") {
                withCfirSymbolIdEntry("symbol", this@CfirBasedSymbol)
            }
    /** 符号是否已绑定到声明。 */
    val isBound: Boolean
        get() = _cfir != null
    val origin: CfirDeclarationOrigin
        get() = cfir.origin

    @CfirImplementationDetail
    fun bind(e: @UnsafeVariance D) {
        _cfir = e
    }


    open val debugName: String
        get() = toString()
}

// ================================
// 分类器符号（Classifier Symbols）
// ================================

/**
 * 所有分类器声明（class、interface、struct、enum、typealias、类型参数）的符号基类。
 *
 * 分类器是"可以作为类型模板的具名声明"，
 * 每种分类器都能生成对应的 [ConeClassifierLookupTag] 供类型系统使用。
 */
sealed class CfirClassifierSymbol<D : CfirDeclaration> : CfirThisOwnerSymbol<D>() {
    /** 生成该分类器对应的 lookup tag，用于在类型系统中定位此声明。 */
    abstract fun toLookupTag(): ConeClassifierLookupTag

}
/**
 * 持有 [ClassId] 的分类器符号抽象基类。
 *
 * 仓颉只有顶层 class-like 声明拥有稳定的 [ClassId]。
 * 因此这里不再区分 Kotlin 式的嵌套或局部 class-like 变体，
 * lookup tag 统一直接由顶层 [ClassId] 派生。
 */
sealed class CfirClassLikeSymbol<D : CfirClassLikeDeclaration>(
    classId: ClassId,
) : CfirClassifierSymbol<D>() {

    open val classId: ClassId = classId
    val resolvedSuperTypeRefs: List<CfirResolvedTypeRef>
        get() {
            lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
            @Suppress("UNCHECKED_CAST")
            return cfir.superTypeRefs as List<CfirResolvedTypeRef>
        }

    open val name: Name
        get() = classId.shortClassName


    private val lookupTag: ConeClassLikeLookupTag =
         classId.toLookupTag()

    final override fun toLookupTag(): ConeClassLikeLookupTag = lookupTag

    override val debugName: String get() = classId.asString()

    /**
     * 对齐 Kotlin FIR：
     * class-like symbol 只读取自身声明上的弃用信息，不沿展开链向外追溯。
     */
    fun getOwnDeprecation(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        if (deprecationsAreDefinitelyEmpty()) {
            return null
        }

        lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        return cfir.deprecationsProvider.getDeprecationsInfo(languageVersionSettings)
    }

    private fun deprecationsAreDefinitelyEmpty(): Boolean {
        if (cfir.annotations.isEmpty() && cfir.deprecationsProvider == EmptyDeprecationsProvider) {
            return true
        }

        return false
    }
}

//sealed class CfirClassLikeSymbol<D : CfirClassLikeDeclaration>(
//    override val classId: ClassId ,
//) : CfirClassLikeSymbol<D>(classId)

/**
 * class 符号，对应仓颉中的顶层引用语义类型声明。
 *
 * class 成员只包含构造器、函数、属性和字段变量，
 * 不再承载任何嵌套 class-like 语义。
 */
class CfirClassSymbol(
    override val classId: ClassId ,
) : CfirClassLikeSymbol<CfirClass>(classId) {

    override val name: Name
        get() = if (isBound) cfir.name else super.name

    override fun toString(): String =
        if (isBound) "CfirClassSymbol(${cfir.name})" else "CfirClassSymbol(unbound)"
}

/**
 * interface 符号，对应仓颉中的接口声明。
 *
 * 接口只能包含属性（prop）和方法（function），
 * 不能有构造器和字段变量。
 * 这一约束在节点层（[CfirInterface]）通过独立的 properties/functions
 * 字段而非通用 declarations 列表来静态保证。
 */
class CfirInterfaceSymbol(
    override val classId: ClassId ,
) : CfirClassLikeSymbol<CfirInterface>(classId) {

    override val name: Name
        get() = if (isBound) cfir.name else super.name

    override fun toString(): String =
        if (isBound) "CfirInterfaceSymbol(${cfir.name})" else "CfirInterfaceSymbol(unbound)"
}

/**
 * struct（值类型结构体）符号，对应仓颉中值语义的具名类型声明。
 *
 * 赋值时复制整个结构体而非共享引用。
 * 与 [CfirClassSymbol] 结构对称，但在类型系统中区分值语义和引用语义。
 */
class CfirStructSymbol(
    override val classId: ClassId ,
) : CfirClassLikeSymbol<CfirStruct>(classId) {

    override val name: Name
        get() = if (isBound) cfir.name else super.name

    override fun toString(): String =
        if (isBound) "CfirStructSymbol(${cfir.name})" else "CfirStructSymbol(unbound)"
}

/**
 * enum（代数数据类型枚举）符号。
 *
 * 支持带参数的构造器（ADT 风格）。
 * [isRefEnum] 区分值枚举（EnumTy）和引用枚举（RefEnumTy），
 * 决定构造器实例的内存分配方式。
 */
class CfirEnumSymbol(
    override val classId: ClassId ,
    /** 是否为引用枚举（RefEnumTy）。 */
    val isRefEnum: Boolean = false,
) : CfirClassLikeSymbol<CfirEnum>(classId) {

    override val name: Name
        get() = if (isBound) cfir.name else super.name

    override fun toString(): String =
        if (isBound) "CfirEnumSymbol(${cfir.name})" else "CfirEnumSymbol(unbound)"
}

/**
 * 类型别名符号。
 *
 * 不引入新的类型身份，仅记录展开规则（别名名 → 目标类型）。
 * 持有 [ClassId] 是为了在符号表中按名字查找时与真实类统一处理，
 * 所有实质性类型运算均发生在展开后的目标类型上。
 */
class CfirTypeAliasSymbol(
    classId: ClassId ,
) : CfirClassLikeSymbol<CfirTypeAlias>(classId) {

    override val name: Name
        get() = if (isBound) cfir.name else super.name

    override fun toString(): String =
        if (isBound) "CfirTypeAliasSymbol(${cfir.name})" else "CfirTypeAliasSymbol(unbound)"
}

/**
 * 类型参数符号，对应声明中的泛型参数，如 `<T>`、`<E>`。
 *
 * 与持有 ClassId 的符号不同，类型参数没有全局唯一的 ClassId，
 * 其身份由声明位置决定，lookup tag 直接绑定到本符号实例（引用相等）。
 */
class CfirTypeParameterSymbol : CfirClassifierSymbol<CfirTypeParameter>() {

    val name: Name
        get() = if (isBound) cfir.name else SpecialNames.NO_NAME_PROVIDED

    /** lookup tag 直接持有本符号引用，类型参数身份即为引用相等。 */
    private val lookupTag = ConeTypeParameterLookupTag(this)

    override fun toLookupTag(): ConeTypeParameterLookupTag = lookupTag

    override val debugName: String get() = name.asString()

    val resolvedBounds: List<CfirResolvedTypeRef>
        get() {
            lazyResolveToPhase(CfirResolvePhase.TYPES)
            @Suppress("UNCHECKED_CAST")
            return cfir.bounds as List<CfirResolvedTypeRef>
        }
    val containingDeclarationSymbol: CfirBasedSymbol<*>
        get() = cfir.containingDeclarationSymbol

    override fun toString(): String =
        if (isBound) "CfirTypeParameterSymbol(${cfir.name})" else "CfirTypeParameterSymbol(unbound)"
}

// ================================
// 可调用符号（Callable Symbols）
// ================================

/**
 * 所有可调用声明（函数、构造器、属性、变量等）的符号基类。
 *
 * [CallableId] = 所在包/类 + 可调用名称，在全局范围内唯一标识一个可调用声明。
 */
sealed class CfirCallableSymbol<out D : CfirCallableDeclaration> : CfirBasedSymbol<D>() {
    abstract val callableId: CallableId
    abstract val name: Name
    private fun ensureType(typeRef: CfirTypeRef?) {
        when (typeRef) {
            null, is CfirResolvedTypeRef -> {}
            is CfirImplicitTypeRef -> lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
            else -> lazyResolveToPhase(CfirResolvePhase.TYPES)
        }
    }
    val resolvedStatus: CfirResolvedDeclarationStatus
        get() = cfir.resolvedStatus()
    val rawStatus: CfirDeclarationStatus
        get() = cfir.status

    val resolvedReturnType: ConeCangJieType
        get() = resolvedReturnTypeRef.coneType

    fun calculateReturnType() {
        ensureType(cfir.returnTypeRef)
        val returnTypeRef = cfir.returnTypeRef
        if (returnTypeRef !is CfirResolvedTypeRef) {
            errorInLazyResolve("returnTypeRef", returnTypeRef::class, CfirResolvedTypeRef::class)
        }
    }
    fun callableIdAsString(): String = callableId.toString()
    val resolvedReturnTypeRef: CfirResolvedTypeRef
        get() {
            calculateReturnType()
            return cfir.returnTypeRef as CfirResolvedTypeRef
        }

    override val debugName: String get() = name.asString()
}

/**
 * 对齐 Kotlin FIR：
 * callable symbol 通过 declaration 上的 `deprecationsProvider` 拉取按 use-site 组织的弃用信息。
 */
fun CfirCallableSymbol<*>.getDeprecation(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
    if (deprecationsAreDefinitelyEmpty()) {
        return null
    }

    lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
    return cfir.deprecationsProvider.getDeprecationsInfo(languageVersionSettings)
}

private fun CfirCallableSymbol<*>.deprecationsAreDefinitelyEmpty(): Boolean {
    if (cfir.annotations.isEmpty() && cfir.deprecationsProvider == EmptyDeprecationsProvider) {
        return true
    }

    return false
}

sealed class CfirNamedValueSymbol<out D : CfirCallableDeclaration> (override val callableId: CallableId): CfirCallableSymbol<D>()
{


}

/** 具名函数符号，对齐 K2 `FirNamedFunctionSymbol`。 */
class CfirNamedFunctionSymbol(
   callableId: CallableId,
) : CfirFunctionSymbol<CfirNamedFunction>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirNamedFunctionSymbol(${cfir.name})" else "CfirNamedFunctionSymbol(unbound)"
}

/**
 * 匿名函数符号，对应 lambda 表达式中的匿名函数。
 *
 * 名称始终为 `<anonymous>`，不持有全局唯一的 [CallableId]。
 */
class CfirAnonymousFunctionSymbol : CfirFunctionWithoutNameSymbol<CfirAnonymousFunction>(Name.identifier("anonymous")) {
    override val name: Name get() = SpecialNames.ANONYMOUS

    override fun toString(): String = "CfirAnonymousFunctionSymbol"
}

/** 顶层 main 函数符号，程序入口点。 */
class CfirMainFunctionSymbol(
   callableId: CallableId,
) : CfirFunctionSymbol<CfirMainFunction>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirMainFunctionSymbol" else "CfirMainFunctionSymbol(unbound)"
}

/** 宏声明符号。 */
class CfirMacroDeclarationSymbol(
      callableId: CallableId,
) : CfirFunctionSymbol<CfirMacroDeclaration>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirMacroDeclarationSymbol(${cfir.name})" else "CfirMacroDeclarationSymbol(unbound)"
}

/** finalizer（析构函数）符号，在对象生命周期结束时被调用。 */
class CfirFinalizerSymbol(
     callableId: CallableId,
) : CfirFunctionSymbol<CfirFinalizer>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirFinalizerSymbol" else "CfirFinalizerSymbol(unbound)"
}

/** 构造器符号，对应 class/struct 的 `init` 声明。 */
class CfirConstructorSymbol(
  callableId: CallableId,
) : CfirFunctionSymbol<CfirConstructor>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirConstructorSymbol" else "CfirConstructorSymbol(unbound)"
}

/** 属性符号，对应 `prop` 声明。 */
class CfirPropertySymbol(
    callableId: CallableId,
) : CfirNamedValueSymbol<CfirProperty>(callableId) {
    override val name: Name get() = callableId.callableName

    /**
     * 对齐 Kotlin FIR，property 自身负责暴露 accessor symbol。
     *
     * accessor 的声明真相仍然在 [CfirProperty] 上，symbol 层只做稳定投影。
     */
    open val getterSymbol: CfirPropertyAccessorSymbol?
        get() = cfir.getter?.symbol

    open val setterSymbol: CfirPropertyAccessorSymbol?
        get() = cfir.setter?.symbol

    override fun toString(): String =
        if (isBound) "CfirPropertySymbol(${cfir.name})" else "CfirPropertySymbol(unbound)"
}

/** 属性访问器符号，对齐 K2 `FirPropertyAccessorSymbol`。 */
open class CfirPropertyAccessorSymbol : CfirFunctionWithoutNameSymbol<CfirPropertyAccessor>(Name.identifier("accessor")) {
    val isGetter: Boolean
        get() = cfir.isGetter
    val isSetter: Boolean
        get() = !cfir.isGetter
    open val propertySymbol: CfirPropertySymbol
        get() = cfir.propertySymbol

    override fun toString(): String =
        if (isBound) "CfirPropertyAccessorSymbol(${if (cfir.isGetter) "getter" else "setter"})"
        else "CfirPropertyAccessorSymbol(unbound)"
}

/** 成员字段变量符号，对应 class/struct 内的 `var`/`let` 字段声明。 */
sealed class CfirVariableSymbol<out D : CfirVariable> (callableId: CallableId): CfirNamedValueSymbol<D>(callableId)

class CfirFieldVariableSymbol(
    override val callableId: CallableId,
) : CfirVariableSymbol<CfirFieldVariable>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirFieldVariableSymbol(${cfir.name})" else "CfirFieldVariableSymbol(unbound)"
}

/** 模式变量符号，对应模式匹配中绑定的变量，如 `case Foo(x) =>` 中的 `x`。 */
class CfirPatternVariableSymbol(
  callableId: CallableId,
) : CfirVariableSymbol<CfirPatternVariable>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirPatternVariableSymbol(${cfir.pattern::class.simpleName})"
        else "CfirPatternVariableSymbol(unbound)"
}

/**
 * 模式内部绑定变量符号。
 *
 * 该符号对应模式树中真正进入作用域的绑定名，例如：
 * - `let (a, b) = pair` 中的 `a` / `b`
 * - `case Year(y)` 中的 `y`
 * - `case value: Int` 中的 `value`
 *
 * 它与外层 `CfirPatternVariableSymbol` 明确分层：
 * 外层 symbol 只描述模式声明容器，内部 binding symbol 才承担名称解析、导航与诊断职责。
 */
class CfirPatternBindingSymbol(
  callableId: CallableId,
) : CfirVariableSymbol<CfirPatternBindingVariable>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirPatternBindingSymbol(${cfir.name})"
        else "CfirPatternBindingSymbol(unbound)"
}

/** 值参数符号，对应函数声明中的形参。 */
class CfirValueParameterSymbol(
  callableId: CallableId,
) : CfirVariableSymbol<CfirValueParameter>(callableId) {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirValueParameterSymbol(${cfir.name})" else "CfirValueParameterSymbol(unbound)"
}

// ================================
// 其他符号
// ================================

/**
 * 源文件符号，对应一个 `.cj` 源文件。
 *
 * 同时实现 [EvaluatedConstTracker.Key]，以源文件路径作为常量求值缓存的 key。
 */
class CfirFileSymbol : CfirBasedSymbol<CfirFile>(), EvaluatedConstTracker.Key {

    val sourceFile: CjSourceFile? get() = cfir.sourceFile

    override fun asStringBasedKey(): EvaluatedConstTracker.Key.StringBased? {
        if (!isBound) return null
        return sourceFile?.path?.let { EvaluatedConstTracker.Key.StringBased(it) }
    }

    override fun toString(): String =
        if (isBound) "CfirFileSymbol(${cfir.name})" else "CfirFileSymbol(unbound)"
}

/** code fragment 符号，对齐 Kotlin FIR 的 `FirCodeFragmentSymbol`。 */
class CfirCodeFragmentSymbol : CfirBasedSymbol<CfirCodeFragment>() {
    override fun toString(): String = "CfirCodeFragmentSymbol"
}

/**
 * extend 声明符号，对应仓颉中的 `extend` 块。
 *
 * extend 为已有类型附加新的方法或接口实现，不引入新的类型声明。
 */
class CfirExtendSymbol : CfirThisOwnerSymbol<CfirExtend>() {
    override fun toString(): String = "CfirExtendSymbol"
}

/** enum 构造器符号，对应枚举中带参数的构造器，如 `case Foo(Int)`。 */
class CfirEnumConstructorSymbol(
    override val callableId: CallableId,
) : CfirCallableSymbol<CfirEnumConstructor>() {
    override val name: Name get() = callableId.callableName

    override fun toString(): String =
        if (isBound) "CfirEnumConstructorSymbol(${cfir.name})" else "CfirEnumConstructorSymbol(unbound)"
}

/** 无效声明符号，用于在解析出错时占位，携带错误原因以便诊断。 */
class CfirInvalidDeclarationSymbol : CfirBasedSymbol<CfirInvalidDeclaration>() {
    override fun toString(): String =
        if (isBound) "CfirInvalidDeclarationSymbol(${cfir.reason})" else "CfirInvalidDeclarationSymbol(unbound)"
}
