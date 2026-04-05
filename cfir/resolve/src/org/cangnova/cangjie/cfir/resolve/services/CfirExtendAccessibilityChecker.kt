package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Extend 可访问性检查器。对齐 C++ ImportManager::IsExtendAccessible。
 *
 * 仓颉 extend 不支持访问修饰符（始终默认 public），
 * 因此跨包可访问性仅取决于导入关系：
 * 1. 同包 → 可访问
 * 2. 至少一个继承接口在当前文件已导入
 * 3. 扩展目标类型在当前文件可访问
 */
class CfirExtendAccessibilityChecker(private val session: CfirSession) {

    fun isAccessible(file: CfirFile, extend: CfirExtend): Boolean {
        val filePackage = file.packageDirective.packageFqName
        val model = session.extendIndexStoreOrNull?.modelForDeclaration(extend) ?: return true

        // 1. 同包 → 可访问
        if (filePackage == model.packageFqName) return true

        // 2. 获取文件 import 信息
        val importedClassIds = resolveImportedClassIds(file)
        val importedPackages = resolveImportedPackages(file)

        // 3. 至少一个继承接口已导入
        if (model.inheritedInterfaceClassIds.isNotEmpty()) {
            val hasImportedInterface = model.inheritedInterfaceClassIds.any { classId ->
                isClassIdAccessible(classId, filePackage, importedClassIds, importedPackages)
            }
            if (!hasImportedInterface) return false
        }

        // 4. 扩展目标类型可访问
        val targetClassId = model.targetClassId
        if (targetClassId != null &&
            !isClassIdAccessible(targetClassId, filePackage, importedClassIds, importedPackages)
        ) {
            return false
        }

        return true
    }

    private fun resolveImportedClassIds(file: CfirFile): Set<ClassId> {
        val bindings = session.importBindingStoreOrNull?.getBindings(file) ?: return emptySet()
        return bindings.imports.flatMapTo(mutableSetOf()) { binding ->
            binding.targets.filterIsInstance<CfirResolvedImportTarget.ClassLike>().map { it.classId }
        }
    }

    private fun resolveImportedPackages(file: CfirFile): Set<FqName> {
        val bindings = session.importBindingStoreOrNull?.getBindings(file) ?: return emptySet()
        return bindings.imports.flatMapTo(mutableSetOf()) { binding ->
            binding.targets.filterIsInstance<CfirResolvedImportTarget.Package>().map { it.fqName }
        }
    }

    private fun isClassIdAccessible(
        classId: ClassId,
        filePackage: FqName,
        importedClassIds: Set<ClassId>,
        importedPackages: Set<FqName>,
    ): Boolean {
        if (classId.packageFqName == filePackage) return true
        if (classId in importedClassIds) return true
        if (classId.packageFqName in importedPackages) return true
        return false
    }
}
