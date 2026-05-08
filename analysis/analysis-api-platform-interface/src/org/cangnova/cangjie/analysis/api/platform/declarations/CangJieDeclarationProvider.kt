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
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement

@CaPlatformInterface
interface CangJieDeclarationProvider : CangJieComposableProvider {
    fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration?

    fun getAllClassesByClassId(classId: ClassId): Collection<CjTypeStatement>
    fun getAllTypeAliasesByClassId(classId: ClassId): Collection<CjTypeAlias>

    fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name>

    fun getTopLevelProperties(callableId: CallableId): Collection<CjProperty>
    fun getTopLevelFunctions(callableId: CallableId): Collection<CjNamedFunction>

    fun getTopLevelCallableFiles(callableId: CallableId): Collection<CjFile>

    fun getTopLevelExtends(): Collection<CjExtend>

    fun getTopLevelExtendFiles(): Collection<CjFile>

    fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>

    fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<CjFile>

    fun findFilesForFacade(facadeFqName: FqName): Collection<CjFile>

    fun findInternalFilesForFacade(facadeFqName: FqName): Collection<CjFile>


    fun computePackageNames(): Set<String>? = null

    val hasSpecificClassifierPackageNamesComputation: Boolean

    fun computePackageNamesWithTopLevelClassifiers(): Set<String>? = computePackageNames()

    val hasSpecificCallablePackageNamesComputation: Boolean

    fun computePackageNamesWithTopLevelCallables(): Set<String>? = computePackageNames()
}

@CaPlatformInterface
interface CangJieDeclarationProviderFactory : CaPlatformComponent {
    fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: CaModule?): CangJieDeclarationProvider

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJieDeclarationProviderFactory = project.service()
    }
}

@CaPlatformInterface
interface CangJieDeclarationProviderMerger : CaComposableProviderMerger<CangJieDeclarationProvider>, CaPlatformComponent {
    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJieDeclarationProviderMerger = project.service()
    }
}

@CaPlatformInterface
fun Project.createDeclarationProvider(scope: GlobalSearchScope, contextualModule: CaModule?): CangJieDeclarationProvider =
    CangJieDeclarationProviderFactory.getInstance(this).createDeclarationProvider(scope, contextualModule)

@CaPlatformInterface
fun Project.mergeDeclarationProviders(declarationProviders: List<CangJieDeclarationProvider>): CangJieDeclarationProvider =
    CangJieDeclarationProviderMerger.getInstance(this).merge(declarationProviders)
