package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageInternalDeclaration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
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
class CfirExtendAccessibilityChecker(private val session: CfirSession) {

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

    private fun CfirExtendSemanticModel.isTargetInSamePackageAsExtend(): Boolean =
        targetClassId?.packageFqName == packageFqName ||
            targetClassId == null && targetKey != null && packageFqName.asString() == STDLIB_CORE_PACKAGE

    private fun CfirExtend.allUpperBoundsExported(): Boolean =
        typeParameters.all { typeParameter -> typeParameter.bounds.all(::isTypeRefExported) }

    private fun CfirExtend.allUpperBoundsAccessible(imports: ImportAccessibility): Boolean =
        typeParameters.all { typeParameter -> typeParameter.bounds.all(imports::isTypeAccessible) }

    private fun isTypeRefExported(typeRef: CfirTypeRef): Boolean {
        val classId = typeRef.classIdOrNull() ?: return true
        return isClassIdExported(classId)
    }

    private fun isClassIdExported(classId: ClassId): Boolean {
        val declaration = classId.classLikeDeclarationOrNull() ?: return true
        return declaration.status.visibility.isExportedVisibility()
    }

    private fun isClassIdVisibleFromPackage(classId: ClassId, useSitePackage: FqName): Boolean {
        val declaration = classId.classLikeDeclarationOrNull() ?: return true
        return isVisibilityVisibleFromPackage(
            visibility = declaration.status.visibility,
            useSitePackage = useSitePackage,
            declarationPackage = classId.packageFqName,
        )
    }

    private fun isVisibilityVisibleFromPackage(
        visibility: Visibility,
        useSitePackage: FqName,
        declarationPackage: FqName,
    ): Boolean {
        if (useSitePackage == declarationPackage) return true
        return when (visibility) {
            Visibilities.Public -> true
            Visibilities.Protected -> useSitePackage.hasSameRootModuleAs(declarationPackage)
            Visibilities.Internal -> canAccessPackageInternalDeclaration(useSitePackage, declarationPackage)
            else -> false
        }
    }

    private fun Visibility.isExportedVisibility(): Boolean = when (this) {
        Visibilities.Public,
        Visibilities.Protected -> true
        Visibilities.Internal -> true
        else -> false
    }

    private fun ClassId.classLikeDeclarationOrNull(): CfirClassLikeDeclaration? {
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(this) ?: return null
        return symbol.cfir as? CfirClassLikeDeclaration
    }

    private fun CfirTypeRef.classIdOrNull(): ClassId? =
        (this as? CfirResolvedTypeRef)?.coneType?.classIdOrPrimitiveClassId

    private inner class ImportAccessibility(file: CfirFile) {
        private val filePackage = file.packageDirective.packageFqName
        private val importedClassIds: Set<ClassId>
        private val importedPackages: Set<FqName>

        init {
            val bindings = session.importBindingStoreOrNull?.getBindings(file)
            importedClassIds = bindings?.imports.orEmpty()
                .flatMapTo(mutableSetOf()) { binding ->
                    binding.targets.filterIsInstance<CfirResolvedImportTarget.ClassLike>().map { it.classId }
                }
            importedPackages = bindings?.imports.orEmpty()
                .flatMapTo(mutableSetOf()) { binding ->
                    binding.targets.filterIsInstance<CfirResolvedImportTarget.Package>().map { it.fqName }
                }
        }

        fun isTypeAccessible(typeRef: CfirTypeRef): Boolean {
            val classId = typeRef.classIdOrNull() ?: return true
            return isTypeAccessible(classId)
        }

        fun isTypeAccessible(classId: ClassId): Boolean {
            if (!isClassIdVisibleFromPackage(classId, filePackage)) return false
            if (classId.packageFqName == filePackage) return true
            if (classId in importedClassIds) return true
            if (classId.packageFqName in importedPackages) return true
            return false
        }
    }

    private fun FqName.hasSameRootModuleAs(other: FqName): Boolean {
        if (isRoot || other.isRoot) return false
        return pathSegments().firstOrNull() == other.pathSegments().firstOrNull()
    }

    private companion object {
        const val STDLIB_CORE_PACKAGE: String = "std.core"
    }
}
