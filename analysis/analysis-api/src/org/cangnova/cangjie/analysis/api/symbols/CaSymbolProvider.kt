package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
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

    val CjTypeAlias.symbol: CaTypeAliasSymbol

    val CjNamedFunction.symbol: CaNamedFunctionSymbol

    val CjFunctionLiteral.symbol: CaAnonymousFunctionSymbol

    val CjConstructor<*>.symbol: CaConstructorSymbol

    val CjMacroDeclaration.symbol: CaMacroSymbol

    val CjFinalizer.symbol: CaFinalizerSymbol

    val CjProperty.symbol: CaPropertySymbol

    val CjPropertyAccessor.symbol: CaPropertyAccessorSymbol

    val CjFieldVariable.symbol: CaFieldSymbol

    val CjEnumConstructor.symbol: CaEnumConstructorSymbol

    val CjPatternVariable.symbol: CaPatternVariableSymbol

    val CjBindingPattern.symbol: CaPatternBindingSymbol

    val CjParameter.symbol: CaVariableSymbol

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
context(session: CaSession)
public val CjDeclaration.symbol: CaDeclarationSymbol
    get() = with(session) { symbol }
