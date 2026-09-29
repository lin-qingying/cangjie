@file:OptIn(CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieSourceSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLEmptyCangJieSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.CfirElementFinder
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLContainingClassCalculator
import org.cangnova.cangjie.cfir.ThreadSafeMutableState
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * 源码 low-level CFIR session 的主 [CfirProvider] 实现。
 *
 * 它通过模块专属 symbol provider 查询源码声明，并通过模块缓存恢复声明所在的 CFIR 文件。
 */
@ThreadSafeMutableState
@OptIn(CaPlatformInterface::class)
internal class LLCfirProvider(
    /**
     * 当前 provider 所属的低阶 CFIR session。
     */
    val session: LLCfirSession,

    /**
     * 当前模块解析组件集合，提供文件缓存和容器文件查询能力。
     */
    private val moduleComponents: LLCfirModuleResolveComponents,
    disregardSelfDeclarations: Boolean = false,
    declarationProviderFactory: (GlobalSearchScope) -> CangJieDeclarationProvider?,
) : CfirProvider() {
    /**
     * 当前模块的 CangJie symbol provider。
     *
     * 当 [disregardSelfDeclarations] 为 `true` 时使用空 provider，避免当前模块声明参与查询。
     */
    override val symbolProvider: LLCangJieSymbolProvider =
        if (!disregardSelfDeclarations) {
            LLCangJieSourceSymbolProvider(session, moduleComponents, declarationProviderFactory)
        } else {
            LLEmptyCangJieSymbolProvider(session)
        }

    /**
     * IDE low-level provider 允许返回分阶段 CFIR。
     */
    override val isPhasedCfirAllowed: Boolean get() = true

    /**
     * 按 [classId] 查询 class-like CFIR 声明。
     */
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? =
        symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir

    /**
     * @param classLikeDeclaration The [CjClassLikeDeclaration] must be contained in the module associated with this [LLCfirProvider]. See
     *  [LLModuleSpecificSymbolProviderAccess] for details.
     */
    fun getCfirClassifierByDeclaration(classLikeDeclaration: CjClassLikeDeclaration): CfirClassLikeDeclaration? {
        val classId = classLikeDeclaration.getClassId() ?: return null

        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return symbolProvider.getClassLikeSymbolByPsi(classId, classLikeDeclaration)?.cfir
    }

    /**
     * 查询 [fqName] 对应 classifier 的容器 CFIR 文件，找不到时抛出带 class id 附件的错误。
     */
    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile {
        return getCfirClassifierContainerFileIfAny(fqName)
            ?: errorWithAttachment("Couldn't find container") {
                withEntry("classId", fqName.asString())
            }
    }

    /**
     * 查询 [fqName] 对应 classifier 的容器 CFIR 文件；找不到时返回 `null`。
     */
    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? {
        return getCfirClassifierByFqName(fqName)?.let { moduleComponents.cache.getContainerCfirFile(it) }
    }

    /**
     * 查询 [symbol] 对应 classifier 的容器 CFIR 文件，找不到时抛出带 symbol 附件的错误。
     */
    override fun getCfirClassifierContainerFile(symbol: CfirClassLikeSymbol<*>): CfirFile {
        return getCfirClassifierContainerFileIfAny(symbol)
            ?: errorWithAttachment("Couldn't find container") {
                withCfirSymbolEntry("symbol", symbol)
            }
    }

    /**
     * 查询 [symbol] 对应 classifier 的容器 CFIR 文件；找不到时返回 `null`。
     */
    override fun getCfirClassifierContainerFileIfAny(symbol: CfirClassLikeSymbol<*>): CfirFile? {
        return moduleComponents.cache.getContainerCfirFile(symbol.cfir)
    }

    /**
     * 查询 [symbol] 对应 callable 的容器 CFIR 文件。
     */
    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? {
        return moduleComponents.cache.getContainerCfirFile(symbol.cfir)
    }

    /**
     * 通过绑定模式 PSI 与模块文件结构恢复所属 pattern variable。
     *
     * standalone/IDE 源码 provider 不预建主编译器的声明 owner 表，因此 LL 层从共享 CFIR 文件树中
     * 按 PSI 身份查找 owner。遍历完整树同时覆盖顶层与函数体内的局部 pattern 声明。
     */
    override fun getCfirPatternVariableForBinding(symbol: CfirPatternBindingSymbol): CfirPatternVariable? {
        val bindingPsi = symbol.cfir.psi as? CjBindingPattern ?: return null
        val file = getCfirCallableContainerFile(symbol) ?: return null
        val ownerPsi = PsiTreeUtil.getParentOfType(bindingPsi, CjPatternVariable::class.java, false) ?: return null
        return CfirElementFinder.findElementIn<CfirPatternVariable>(file) { patternVariable ->
            patternVariable.psi === ownerPsi
        }
    }

    /**
     * 枚举当前 source session 包内的 CFIR 文件。
     *
     * 从模块声明索引取文件并通过同一模块缓存构造 raw CFIR；不在 provider 查询时推进解析阶段，
     * 由需要完整签名的 checker 按自身阶段契约显式推进，避免 import/extend 查找递归进入 body resolve。
     */
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> =
        symbolProvider.declarationProvider.findFilesForFacadeByPackage(fqName)
            .distinct()
            .map(moduleComponents.cfirFileBuilder::buildRawCfirFileWithCaching)

    /**
     * 返回 [fqName] 包内顶层 classifier 名称集合。
     */
    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(fqName)
            ?: errorWithAttachment("Cannot compute the set of class names in the given package") {
                withEntry("packageFqName", fqName.asString())
            }

    /**
     * 查询 [symbol] 的包含类 symbol。
     *
     * 源码符号按 PSI 结构保留重声明身份；其余声明使用基类的 owner lookup tag 契约。
     */
    override fun getContainingClass(symbol: CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? {
        val psiResult = LLContainingClassCalculator.getContainingClassSymbol(symbol)
        if (psiResult != null) {
            return psiResult
        }

        return super.getContainingClass(symbol)
    }
}
