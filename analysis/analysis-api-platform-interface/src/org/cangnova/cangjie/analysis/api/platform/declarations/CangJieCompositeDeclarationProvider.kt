package org.cangnova.cangjie.analysis.api.platform.declarations

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CangJieCompositeProvider
import org.cangnova.cangjie.analysis.api.platform.CaCompositeProviderFactory
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.utils.flatMapToNullableSet

@CaPlatformInterface
class CangJieCompositeDeclarationProvider private constructor(
    override val providers: List<CangJieDeclarationProvider>
) : CangJieDeclarationProvider, CangJieCompositeProvider<CangJieDeclarationProvider> {
    override fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration? {
        return providers.firstNotNullOfOrNull { it.getClassLikeDeclarationByClassId(classId) }
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<CjTypeStatement> {
        return providers.flatMap { it.getAllClassesByClassId(classId) }
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<CjTypeAlias> {
        return providers.flatMap { it.getAllTypeAliasesByClassId(classId) }
    }

    override fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName) }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<CjProperty> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelProperties(callableId) }
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<CjNamedFunction> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelFunctions(callableId) }
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelCallableFiles(callableId) }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getTopLevelCallableNamesInPackage(packageFqName) }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.findFilesForFacadeByPackage(packageFqName) }
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.findFilesForFacade(facadeFqName) }
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<CjFile> {
        return providers.flatMapTo(mutableListOf()) { it.findInternalFilesForFacade(facadeFqName) }
    }


    override fun computePackageNames(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNames() }
    }

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = providers.any { it.hasSpecificClassifierPackageNamesComputation }

    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNamesWithTopLevelClassifiers() }
    }

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = providers.any { it.hasSpecificCallablePackageNamesComputation }

    override fun computePackageNamesWithTopLevelCallables(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNamesWithTopLevelCallables() }
    }

    @CaPlatformInterface
    companion object {
        val factory: CaCompositeProviderFactory<CangJieDeclarationProvider> = CaCompositeProviderFactory(
            CangJieEmptyDeclarationProvider,
            ::CangJieCompositeDeclarationProvider,
        )

        fun create(providers: List<CangJieDeclarationProvider>): CangJieDeclarationProvider = factory.create(providers)
    }
}
