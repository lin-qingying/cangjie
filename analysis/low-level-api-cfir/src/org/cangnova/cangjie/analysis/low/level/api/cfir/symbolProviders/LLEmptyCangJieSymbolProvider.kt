

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieEmptyDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieEmptyPackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirEmptySymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty

/**
 * 不暴露任何仓颉源码符号的空实现。
 *
 * 该提供器用于需要满足 [LLCangJieSymbolProvider] 契约、但当前会话没有源码声明可供解析的场景。所有查询都会返回空结果，
 * 同时仍提供空声明索引和空包索引，保证组合符号提供器可以统一处理源码符号提供器列表。
 */
@OptIn(CaPlatformInterface::class)
internal class LLEmptyCangJieSymbolProvider(session: CfirSession) : LLCangJieSymbolProvider(session) {
    /**
     * 空名称索引，表示没有任何可枚举的包名、class-like 名称或 callable 名称。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider
        get() = CfirEmptySymbolNamesProvider

    /**
     * 空声明索引，所有按 PSI 或名字的源码声明查询都会得到空集合。
     */
    override val declarationProvider: CangJieDeclarationProvider
        get() = CangJieEmptyDeclarationProvider

    /**
     * 空包索引，表示当前提供器不声明任何包存在。
     */
    override val packageProvider: CangJiePackageProvider
        get() = CangJieEmptyPackageProvider

    /**
     * 空实现不会为 [classId] 产生 class-like 符号。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? = null

    /**
     * 空实现不会把已知 [classLikeDeclaration] 物化为 class-like 符号。
     */
    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? = null

    /**
     * 空实现不会按 [declaration] PSI 命中 class-like 符号。
     */
    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): CfirClassLikeSymbol<*>? = null

    /**
     * 空实现不会向 [destination] 追加任何顶层 callable 符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    /**
     * 空实现不会把已知 [callables] 物化为顶层 callable 符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<CjCallableDeclaration>,
    ) {
    }

    /**
     * 空实现不会向 [destination] 追加任何顶层函数符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    /**
     * 空实现不会把已知 [functions] 物化为顶层函数符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<CjNamedFunction>,
    ) {
    }

    /**
     * 空实现不会向 [destination] 追加任何顶层属性符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    /**
     * 空实现不会把已知 [properties] 物化为顶层属性符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<CjProperty>,
    ) {
    }

    /**
     * 空实现始终报告 [fqName] 包不存在。
     */
    override fun hasPackage(fqName: FqName): Boolean = false
}
