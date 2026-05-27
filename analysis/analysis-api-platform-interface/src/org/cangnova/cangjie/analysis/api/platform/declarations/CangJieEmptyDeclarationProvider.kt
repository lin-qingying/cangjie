package org.cangnova.cangjie.analysis.api.platform.declarations

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
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

@CaPlatformInterface
object CangJieEmptyDeclarationProvider : CangJieDeclarationProvider {
    override fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration? = null
    override fun getAllClassesByClassId(classId: ClassId): List<CjTypeStatement> = emptyList()
    override fun getAllTypeAliasesByClassId(classId: ClassId): List<CjTypeAlias> = emptyList()
    override fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
    override fun getTopLevelProperties(callableId: CallableId): List<CjProperty> = emptyList()
    override fun getTopLevelFunctions(callableId: CallableId): List<CjNamedFunction> = emptyList()
    override fun getTopLevelMacros(callableId: CallableId): List<CjMacroDeclaration> = emptyList()
    override fun getTopLevelCallableFiles(callableId: CallableId): List<CjFile> = emptyList()
    override fun getTopLevelExtends(): List<CjExtend> = emptyList()
    override fun getTopLevelExtendFiles(): List<CjFile> = emptyList()
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
    override fun findFilesForFacadeByPackage(packageFqName: FqName): List<CjFile> = emptyList()
    override fun findFilesForFacade(facadeFqName: FqName): List<CjFile> = emptyList()
    override fun findInternalFilesForFacade(facadeFqName: FqName): List<CjFile> = emptyList()

    override fun computePackageNames(): Set<String> = emptySet()
    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false
}
