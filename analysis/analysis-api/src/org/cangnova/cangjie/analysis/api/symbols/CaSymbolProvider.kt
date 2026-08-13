package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
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
 * 公开 symbol 查询协议。
 *
 * 该协议只承载两类稳定入口：
 * 1. `PSI -> public symbol` 的恢复；
 * 2. 基于稳定语义标识的直接查询。
 *
 * 它不负责任何兼容桥接、文本兜底或宽松回退。
 * 查询失败就返回 `null` / 空列表，由上层明确处理。
 */
interface CaSymbolProvider : CaLifetimeOwner {
    /**
     * 从任意声明 PSI 恢复其公开声明符号。
     */
    val CjDeclaration.symbol: CaDeclarationSymbol

    /**
     * 从源码文件恢复文件级公开符号。
     */
    val CjFile.symbol: CaFileSymbol


    /**
     * 从类型声明 PSI 恢复 class 符号。
     */
    val CjTypeStatement.classSymbol: CaClassSymbol

    /**
     * 从 `extend` 声明 PSI 恢复 extend 符号。
     */
    val CjExtend.symbol: CaExtendSymbol

    /**
     * 从 typealias 声明 PSI 恢复 typealias 符号。
     */
    val CjTypeAlias.symbol: CaTypeAliasSymbol

    /**
     * 从命名函数 PSI 恢复命名函数符号。
     */
    val CjNamedFunction.symbol: CaNamedFunctionSymbol

    /**
     * 从函数字面量 PSI 恢复匿名函数符号。
     */
    val CjFunctionLiteral.symbol: CaAnonymousFunctionSymbol

    /**
     * 从构造器 PSI 恢复构造器符号（同时覆盖主、次构造器）。
     */
    val CjConstructor<*>.symbol: CaConstructorSymbol

    /**
     * 从宏声明 PSI 恢复宏符号。
     */
    val CjMacroDeclaration.symbol: CaMacroSymbol

    /**
     * 从 finalizer PSI 恢复 finalizer 符号。
     */
    val CjFinalizer.symbol: CaFinalizerSymbol

    /**
     * 从属性声明 PSI 恢复属性符号。
     */
    val CjProperty.symbol: CaPropertySymbol

    /**
     * 从属性访问器 PSI 恢复访问器符号（getter / setter 公共父类型）。
     */
    val CjPropertyAccessor.symbol: CaPropertyAccessorSymbol

    /**
     * 从字段变量 PSI 恢复字段符号。
     */
    val CjFieldVariable.symbol: CaFieldSymbol

    /**
     * 从枚举构造器 PSI 恢复枚举构造器符号。
     */
    val CjEnumConstructor.symbol: CaEnumConstructorSymbol

    /**
     * 从模式变量 PSI 恢复模式变量符号。
     */
    val CjPatternVariable.symbol: CaPatternVariableSymbol

    /**
     * 从绑定模式 PSI 恢复模式绑定符号。
     */
    val CjBindingPattern.symbol: CaPatternBindingSymbol

    /**
     * 从形参 PSI 恢复对应的变量符号。
     *
     * 形参在符号体系中以 [CaVariableSymbol] 的子族（[CaValueParameterSymbol]）表达。
     */
    val CjParameter.symbol: CaVariableSymbol

    /**
     * 从类型参数 PSI 恢复类型参数符号。
     */
    val CjTypeParameter.symbol: CaTypeParameterSymbol

    /**
     * 按包名查询包符号。
     */
    fun getPackageSymbol(fqName: FqName): CaPackageSymbol?

    /**
     * 按稳定 `ClassId` 查询任意 class-like 符号。
     */
    fun getClassLikeSymbol(classId: ClassId): CaClassLikeSymbol?

    /**
     * 按稳定 `ClassId` 查询 class 符号。
     */
    fun getClassSymbol(classId: ClassId): CaClassSymbol?

    /**
     * 按稳定 `ClassId` 查询 typealias 符号。
     */
    fun getTypeAliasSymbol(classId: ClassId): CaTypeAliasSymbol?

    /**
     * 查询某个包内、指定短名下的全部顶层 class-like 声明。
     */
    fun getTopLevelClassLikeSymbols(packageFqName: FqName, name: Name): List<CaClassLikeSymbol>

    /**
     * 查询某个包内、指定短名下的全部顶层 callable 声明。
     */
    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CaCallableSymbol>

    /**
     * 查询某个包内声明的全部 `extend`。
     */
    fun getTopLevelExtendSymbols(packageFqName: FqName): List<CaExtendSymbol>

    /**
     * 查询针对某个目标 `ClassId` 的全部 `extend`。
     */
    fun getExtendSymbols(targetClassId: ClassId): List<CaExtendSymbol>
}

/**
 * 在 [CaSession] 上下文中从任意声明 PSI 恢复其公开声明符号。
 *
 * 这是 [CaSymbolProvider.symbol] 的 context-receiver 形式入口，便于不显式构造
 * provider 的调用方使用：`analyze { someDeclaration.symbol }`。
 */
context(session: CaSession)
internal val CjDeclaration.symbol: CaDeclarationSymbol
    get() = with(session) { symbol }

/**
 * 在 [CaSession] 上下文中从任意声明 PSI 恢复其公开声明符号。
 *
 * 这是 [CaSymbolProvider.symbol] 的 context-receiver 形式入口，便于不显式构造
 * provider 的调用方使用：`analyze { someDeclaration.symbol }`。
 */
context(session: CaSession)
internal val CjBindingPattern.symbol: CaDeclarationSymbol
    get() = with(session) { symbol }

/**
 * 在 [CaSession] 上下文中从任意声明 PSI 恢复其公开声明符号。
 *
 * 这是 [CaSymbolProvider.symbol] 的 context-receiver 形式入口，便于不显式构造
 * provider 的调用方使用：`analyze { someDeclaration.symbol }`。
 */
context(session: CaSession)
val CjElement.symbol: CaDeclarationSymbol
    get() {
        return when (this) {
            is CjDeclaration -> symbol
            is CjBindingPattern -> symbol
            else -> error("Unsupported PSI element for symbol resolution: ${this::class.simpleName}")
        }
    }


