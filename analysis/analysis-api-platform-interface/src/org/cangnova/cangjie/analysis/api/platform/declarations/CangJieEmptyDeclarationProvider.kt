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

/**
 * 不提供任何声明的空声明 provider。
 */
@CaPlatformInterface
object CangJieEmptyDeclarationProvider : CangJieDeclarationProvider {
    /**
     * 空 provider 不返回类状声明。
     */
    override fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration? = null
    /**
     * 空 provider 不返回类声明。
     */
    override fun getAllClassesByClassId(classId: ClassId): List<CjTypeStatement> = emptyList()
    /**
     * 空 provider 不返回类型别名声明。
     */
    override fun getAllTypeAliasesByClassId(classId: ClassId): List<CjTypeAlias> = emptyList()
    /**
     * 空 provider 不返回顶层类状声明名称。
     */
    override fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
    /**
     * 空 provider 不返回顶层属性。
     */
    override fun getTopLevelProperties(callableId: CallableId): List<CjProperty> = emptyList()
    /**
     * 空 provider 不返回顶层函数。
     */
    override fun getTopLevelFunctions(callableId: CallableId): List<CjNamedFunction> = emptyList()
    /**
     * 空 provider 不返回顶层宏。
     */
    override fun getTopLevelMacros(callableId: CallableId): List<CjMacroDeclaration> = emptyList()
    /**
     * 空 provider 不返回顶层 callable 文件。
     */
    override fun getTopLevelCallableFiles(callableId: CallableId): List<CjFile> = emptyList()
    /**
     * 空 provider 不返回顶层 extend。
     */
    override fun getTopLevelExtends(): List<CjExtend> = emptyList()
    /**
     * 空 provider 不返回顶层 extend 文件。
     */
    override fun getTopLevelExtendFiles(): List<CjFile> = emptyList()
    /**
     * 空 provider 不返回顶层 callable 名称。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
    /**
     * 空 provider 不返回 facade 包文件。
     */
    override fun findFilesForFacadeByPackage(packageFqName: FqName): List<CjFile> = emptyList()
    /**
     * 空 provider 不返回 facade 文件。
     */
    override fun findFilesForFacade(facadeFqName: FqName): List<CjFile> = emptyList()
    /**
     * 空 provider 不返回内部 facade 文件。
     */
    override fun findInternalFilesForFacade(facadeFqName: FqName): List<CjFile> = emptyList()

    /**
     * 空 provider 的包名集合为空。
     */
    override fun computePackageNames(): Set<String> = emptySet()
    /**
     * 空 provider 不支持单独 classifier 包名计算。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    /**
     * 空 provider 不支持单独 callable 包名计算。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false
}
