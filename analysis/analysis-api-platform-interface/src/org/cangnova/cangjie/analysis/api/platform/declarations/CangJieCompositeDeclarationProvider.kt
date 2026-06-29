package org.cangnova.cangjie.analysis.api.platform.declarations

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CangJieCompositeProvider
import org.cangnova.cangjie.analysis.api.platform.CaCompositeProviderFactory
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.utils.flatMapToNullableSet

/**
 * 多个声明 provider 的组合实现。
 */
@CaPlatformInterface
class CangJieCompositeDeclarationProvider private constructor(
    /**
     * 被组合的声明 provider 列表。
     */
    override val providers: List<CangJieDeclarationProvider>
) : CangJieDeclarationProvider, CangJieCompositeProvider<CangJieDeclarationProvider> {
    /**
     * 从组合 provider 中查找首个匹配 class id 的类状声明。
     */
    override fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration? {
        return providers.firstNotNullOfOrNull { it.getClassLikeDeclarationByClassId(classId) }
    }

    /**
     * 汇总所有 provider 中匹配 class id 的类声明。
     */
    override fun getAllClassesByClassId(classId: ClassId): Collection<CjTypeStatement> {
        return providers.flatMap { it.getAllClassesByClassId(classId) }
    }

    /**
     * 汇总所有 provider 中匹配 class id 的类型别名声明。
     */
    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<CjTypeAlias> {
        return providers.flatMap { it.getAllTypeAliasesByClassId(classId) }
    }

    /**
     * 汇总指定包中的顶层类状声明名称。
     */
    override fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName) }
    }

    /**
     * 汇总指定 callable id 的顶层属性。
     */
    override fun getTopLevelProperties(callableId: CallableId): Collection<CjProperty> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelProperties(callableId) }
    }

    /**
     * 汇总指定 callable id 的顶层函数。
     */
    override fun getTopLevelFunctions(callableId: CallableId): Collection<CjNamedFunction> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelFunctions(callableId) }
    }

    /**
     * 汇总指定 callable id 的顶层宏。
     */
    override fun getTopLevelMacros(callableId: CallableId): Collection<CjMacroDeclaration> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelMacros(callableId) }
    }

    /**
     * 汇总指定 callable id 的顶层 callable 文件。
     */
    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelCallableFiles(callableId) }
    }

    /**
     * 汇总所有顶层 extend 声明。
     */
    override fun getTopLevelExtends(): Collection<CjExtend> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelExtends() }
    }

    /**
     * 汇总包含顶层 extend 的文件。
     */
    override fun getTopLevelExtendFiles(): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelExtendFiles() }
    }

    /**
     * 汇总指定包中的顶层 callable 名称。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getTopLevelCallableNamesInPackage(packageFqName) }
    }

    /**
     * 按包名汇总 facade 文件。
     */
    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.findFilesForFacadeByPackage(packageFqName) }
    }

    /**
     * 按 facade 完整名汇总文件。
     */
    override fun findFilesForFacade(facadeFqName: FqName): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.findFilesForFacade(facadeFqName) }
    }

    /**
     * 按 facade 完整名汇总内部文件。
     */
    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.findInternalFilesForFacade(facadeFqName) }
    }


    /**
     * 汇总 provider 支持精确计算的包名集合。
     */
    override fun computePackageNames(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNames() }
    }

    /**
     * 任一 provider 支持 classifier 包名计算时返回 true。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = providers.any { it.hasSpecificClassifierPackageNamesComputation }

    /**
     * 汇总包含顶层 classifier 的包名集合。
     */
    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNamesWithTopLevelClassifiers() }
    }

    /**
     * 任一 provider 支持 callable 包名计算时返回 true。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = providers.any { it.hasSpecificCallablePackageNamesComputation }

    /**
     * 汇总包含顶层 callable 的包名集合。
     */
    override fun computePackageNamesWithTopLevelCallables(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNamesWithTopLevelCallables() }
    }

    @CaPlatformInterface
    companion object {
        /**
         * 声明 provider 的标准组合工厂。
         */
        val factory: CaCompositeProviderFactory<CangJieDeclarationProvider> = CaCompositeProviderFactory(
            CangJieEmptyDeclarationProvider,
            ::CangJieCompositeDeclarationProvider,
        )

        /**
         * 创建组合声明 provider。
         */
        fun create(providers: List<CangJieDeclarationProvider>): CangJieDeclarationProvider = factory.create(providers)
    }
}
