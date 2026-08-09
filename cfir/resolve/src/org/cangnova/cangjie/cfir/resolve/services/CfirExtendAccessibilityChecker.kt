package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageInternalDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageProtectedDeclaration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Extend 可访问性检查器。对齐 C++ ImportManager::IsExtendAccessible。
 *
 * 这里同时复现 ExtendDecl::IsExportedDecl 与 Modules::IsVisible：
 * 同包直接放行；跨包时先要求 extend 可导出，再按“同包 direct extend 看目标可见性，
 * 异包 interface extend 看接口/目标类型可访问性”的官方路径判断。
 */
class CfirExtendAccessibilityChecker(
    /**
     * 当前解析会话。
     */
    private val session: CfirSession,
) {

    /**
     * 判断 extend 声明在指定文件使用点是否可访问。
     */
    fun isAccessible(file: CfirFile, extend: CfirExtend): Boolean {
        val filePackage = file.packageDirective.packageFqName
        val model = session.extendIndexStoreOrNull?.modelForDeclaration(extend) ?: return true

        if (filePackage == model.packageFqName) return true

        val imports = ImportAccessibility(file)
        if (!extend.isExported(model)) return false
        if (!extend.allUpperBoundsAccessible(imports)) return false

        val targetClassId = model.targetClassId
        val isTargetInSamePackageAsExtend = model.isTargetInSamePackageAsExtend()

        return if (isTargetInSamePackageAsExtend) {
            targetClassId == null || isClassIdVisibleFromPackage(targetClassId, filePackage)
        } else {
            model.inheritedInterfaceClassIds.any(imports::isTypeAccessible) &&
                    (targetClassId == null || imports.isTypeAccessible(targetClassId))
        }
    }

    /**
     * 判断 extend 声明是否满足跨包导出的最低条件。
     */
    private fun CfirExtend.isExported(model: CfirExtendSemanticModel): Boolean {
        val targetClassId = model.targetClassId
        val isTargetInSamePackageAsExtend = model.isTargetInSamePackageAsExtend()

        if (model.inheritedInterfaceClassIds.isEmpty()) {
            if (model.packageFqName.asString() == STDLIB_CORE_PACKAGE) return true
            if (!isTargetInSamePackageAsExtend) return false
            return targetClassId == null || isClassIdExported(targetClassId) && allUpperBoundsExported()
        }

        if (isTargetInSamePackageAsExtend) {
            return targetClassId == null || isClassIdExported(targetClassId)
        }

        return model.inheritedInterfaceClassIds.any(::isClassIdExported) && allUpperBoundsExported()
    }

    /**
     * 判断 extend 目标是否与 extend 声明处于同一包。
     */
    private fun CfirExtendSemanticModel.isTargetInSamePackageAsExtend(): Boolean =
        targetClassId?.packageFqName == packageFqName ||
            targetClassId == null && targetKey != null && packageFqName.asString() == STDLIB_CORE_PACKAGE

    /**
     * 判断 extend 类型参数上界是否全部可导出。
     */
    private fun CfirExtend.allUpperBoundsExported(): Boolean =
        typeParameters.all { typeParameter -> typeParameter.bounds.all(::isTypeRefExported) }

    /**
     * 判断 extend 类型参数上界在当前导入上下文中是否全部可访问。
     */
    private fun CfirExtend.allUpperBoundsAccessible(imports: ImportAccessibility): Boolean =
        typeParameters.all { typeParameter -> typeParameter.bounds.all(imports::isTypeAccessible) }

    /**
     * 判断类型引用指向的 classId 是否可导出。
     */
    private fun isTypeRefExported(typeRef: CfirTypeRef): Boolean {
        val classId = typeRef.classIdOrNull() ?: return true
        return isClassIdExported(classId)
    }

    /**
     * 判断 classId 对应声明是否具备导出可见性。
     */
    private fun isClassIdExported(classId: ClassId): Boolean {
        val declaration = classId.classLikeDeclarationOrNull() ?: return true
        return declaration.status.visibility.isExportedVisibility()
    }

    /**
     * 判断 classId 对应声明是否可从指定包访问。
     */
    private fun isClassIdVisibleFromPackage(classId: ClassId, useSitePackage: FqName): Boolean {
        val declaration = classId.classLikeDeclarationOrNull() ?: return true
        return isVisibilityVisibleFromPackage(
            visibility = declaration.status.visibility,
            useSitePackage = useSitePackage,
            declarationPackage = classId.packageFqName,
        )
    }

    /**
     * 按仓颉包级 public/protected/internal 规则判断可见性。
     */
    private fun isVisibilityVisibleFromPackage(
        visibility: Visibility,
        useSitePackage: FqName,
        declarationPackage: FqName,
    ): Boolean {
        if (useSitePackage == declarationPackage) return true
        return when (visibility) {
            Visibilities.Public -> true
            Visibilities.Protected -> canAccessPackageProtectedDeclaration(useSitePackage, declarationPackage)
            Visibilities.Internal -> canAccessPackageInternalDeclaration(useSitePackage, declarationPackage)
            else -> false
        }
    }

    /**
     * 判断声明可见性是否允许跨包导出。
     */
    private fun Visibility.isExportedVisibility(): Boolean = when (this) {
        Visibilities.Public,
        Visibilities.Protected -> true
        Visibilities.Internal -> true
        else -> false
    }

    /**
     * 解析 classId 对应的 class-like 声明。
     */
    private fun ClassId.classLikeDeclarationOrNull(): CfirClassLikeDeclaration? {
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(this) ?: return null
        return symbol.cfir as? CfirClassLikeDeclaration
    }

    /**
     * 从已解析类型引用中提取 classId。
     */
    private fun CfirTypeRef.classIdOrNull(): ClassId? =
        (this as? CfirResolvedTypeRef)?.coneType?.classIdOrPrimitiveClassId

    /**
     * 当前文件导入语境下的类型可访问性查询器。
     */
    private inner class ImportAccessibility(file: CfirFile) {
        /**
         * 当前文件包名。
         */
        private val filePackage = file.packageDirective.packageFqName
        /** 当前检查绑定的 use-site 文件。 */
        private val useSiteFile: CfirFile = file

        /**
         * 判断类型引用在当前文件中是否可访问。
         */
        fun isTypeAccessible(typeRef: CfirTypeRef): Boolean {
            val classId = typeRef.classIdOrNull() ?: return true
            return isTypeAccessible(classId)
        }

        /**
         * 判断 classId 在当前文件中是否可访问。
         */
        fun isTypeAccessible(classId: ClassId): Boolean {
            if (!isClassIdVisibleFromPackage(classId, filePackage)) return false
            return useSiteFile.isClassIdReachableByImports(session, classId) == true
        }
    }

    /**
     * 本检查中需要特殊视作核心内建包的包名。
     */
    private companion object {
        /**
         * 标准库核心包名。
         */
        const val STDLIB_CORE_PACKAGE: String = "std.core"
    }
}
