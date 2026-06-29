package org.cangnova.cangjie.analysis.api.platform.declarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CangJieComposableProvider
import org.cangnova.cangjie.analysis.api.platform.CaComposableProviderMerger
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
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

/**
 * 面向 Analysis API 的仓颉声明索引抽象。
 */
@CaPlatformInterface
interface CangJieDeclarationProvider : CangJieComposableProvider {
    /**
     * 按 class id 查找首个类状声明。
     */
    fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration?

    /**
     * 按 class id 查找所有类声明。
     */
    fun getAllClassesByClassId(classId: ClassId): Collection<CjTypeStatement>
    /**
     * 按 class id 查找所有类型别名声明。
     */
    fun getAllTypeAliasesByClassId(classId: ClassId): Collection<CjTypeAlias>

    /**
     * 返回指定包中的顶层类状声明名称集合。
     */
    fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name>

    /**
     * 返回指定 callable id 对应的顶层属性声明。
     */
    fun getTopLevelProperties(callableId: CallableId): Collection<CjProperty>
    /**
     * 返回指定 callable id 对应的顶层函数声明。
     */
    fun getTopLevelFunctions(callableId: CallableId): Collection<CjNamedFunction>

    /**
     * 返回指定 [CallableId] 对应的顶层宏声明。
     *
     * 宏在仓颉中参与顶层 callable 导入解析，不能只通过函数/属性集合间接建模。
     */
    fun getTopLevelMacros(callableId: CallableId): Collection<CjMacroDeclaration>

    /**
     * 返回指定 callable id 关联的顶层 callable 所在文件。
     */
    fun getTopLevelCallableFiles(callableId: CallableId): Collection<CjFile>

    /**
     * 返回 provider 范围内所有顶层 extend 声明。
     */
    fun getTopLevelExtends(): Collection<CjExtend>

    /**
     * 返回包含顶层 extend 声明的文件。
     */
    fun getTopLevelExtendFiles(): Collection<CjFile>

    /**
     * 返回指定包中的顶层 callable 名称集合。
     */
    fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>

    /**
     * 按包名查找参与 facade 的文件。
     */
    fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<CjFile>

    /**
     * 按 facade 完整名查找文件。
     */
    fun findFilesForFacade(facadeFqName: FqName): Collection<CjFile>

    /**
     * 按 facade 完整名查找内部文件。
     */
    fun findInternalFilesForFacade(facadeFqName: FqName): Collection<CjFile>


    /**
     * 计算当前 provider 覆盖的包名集合；返回 null 表示不支持精确计算。
     */
    fun computePackageNames(): Set<String>? = null

    /**
     * 是否支持单独计算包含顶层 classifier 的包名。
     */
    val hasSpecificClassifierPackageNamesComputation: Boolean

    /**
     * 计算包含顶层 classifier 的包名集合。
     */
    fun computePackageNamesWithTopLevelClassifiers(): Set<String>? = computePackageNames()

    /**
     * 是否支持单独计算包含顶层 callable 的包名。
     */
    val hasSpecificCallablePackageNamesComputation: Boolean

    /**
     * 计算包含顶层 callable 的包名集合。
     */
    fun computePackageNamesWithTopLevelCallables(): Set<String>? = computePackageNames()
}

/**
 * 声明 provider 平台工厂。
 */
@CaPlatformInterface
interface CangJieDeclarationProviderFactory : CaPlatformComponent {
    /**
     * 为指定搜索范围和上下文模块创建声明 provider。
     */
    fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: CaModule?): CangJieDeclarationProvider

    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级声明 provider 工厂服务。
         */
        fun getInstance(project: Project): CangJieDeclarationProviderFactory = project.service()
    }
}

/**
 * 声明 provider 合并器。
 */
@CaPlatformInterface
interface CangJieDeclarationProviderMerger : CaComposableProviderMerger<CangJieDeclarationProvider>, CaPlatformComponent {
    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级声明 provider 合并器服务。
         */
        fun getInstance(project: Project): CangJieDeclarationProviderMerger = project.service()
    }
}

/**
 * 使用项目平台工厂创建声明 provider。
 */
@CaPlatformInterface
fun Project.createDeclarationProvider(scope: GlobalSearchScope, contextualModule: CaModule?): CangJieDeclarationProvider =
    CangJieDeclarationProviderFactory.getInstance(this).createDeclarationProvider(scope, contextualModule)

/**
 * 使用项目平台合并器合并多个声明 provider。
 */
@CaPlatformInterface
fun Project.mergeDeclarationProviders(declarationProviders: List<CangJieDeclarationProvider>): CangJieDeclarationProvider =
    CangJieDeclarationProviderMerger.getInstance(this).merge(declarationProviders)
