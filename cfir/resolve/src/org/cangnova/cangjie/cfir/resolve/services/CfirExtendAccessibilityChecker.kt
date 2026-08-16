package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageInternalDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageProtectedDeclaration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
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
 *
 * 可见性判定所需的语义信息优先取 extend 语义索引模型（source extend 在 EXTENSIONS 阶段建立，
 * 含 typealias 归一化）；deserialized（库）extend 没有语义模型，回退到 [ExtendAccessView.fromDeclaration]
 * 从声明节点即时派生。两条路径的字段语义等价：库 extend 的类型引用在反序列化时已解析，
 * 与 source 路径在 EXTENSIONS 阶段之后的解析状态一致。已知差异（均不影响合法场景）：
 * 1. 派生视图省略 resolver 的“声明 kind 非接口”校验，依赖 cone 层的 interface 标记
 *    （反序列化保留该标记，实际行为一致）；
 * 2. 派生视图的 targetClassId 使用 cone 层的 alias 展开（expandedClassIdOrPrimitiveClassId），
 *    与语义模型经 semanticExpandedType 展开的结果等价；
 * 3. 拿不到声明包名时维持宽松放行（与无索引 store 时的行为一致）。
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
        val view = session.extendIndexStoreOrNull?.modelForDeclaration(extend)
            ?.let(ExtendAccessView.Companion::fromModel)
            ?: ExtendAccessView.fromDeclaration(session, extend)
            ?: return true

        if (filePackage == view.packageFqName) return true

        val imports = ImportAccessibility(file)
        if (!extend.isExported(view)) return false
        if (!extend.allUpperBoundsAccessible(imports)) return false

        val targetClassId = view.targetClassId
        val isTargetInSamePackageAsExtend = view.isTargetInSamePackageAsExtend()

        return if (isTargetInSamePackageAsExtend) {
            targetClassId == null || isClassIdVisibleFromPackage(targetClassId, filePackage)
        } else {
            view.inheritedInterfaceClassIds.any(imports::isTypeAccessible) &&
                    (targetClassId == null || imports.isTypeAccessible(targetClassId))
        }
    }

    /**
     * 判断 extend 声明是否满足跨包导出的最低条件。
     */
    private fun CfirExtend.isExported(view: ExtendAccessView): Boolean {
        val targetClassId = view.targetClassId
        val isTargetInSamePackageAsExtend = view.isTargetInSamePackageAsExtend()

        if (view.inheritedInterfaceClassIds.isEmpty()) {
            if (view.packageFqName.asString() == STDLIB_CORE_PACKAGE) return true
            if (!isTargetInSamePackageAsExtend) return false
            return targetClassId == null || isClassIdExported(targetClassId) && allUpperBoundsExported()
        }

        if (isTargetInSamePackageAsExtend) {
            return targetClassId == null || isClassIdExported(targetClassId)
        }

        return view.inheritedInterfaceClassIds.any(::isClassIdExported) && allUpperBoundsExported()
    }

    /**
     * 可访问性判定所需的 extend 语义视图。
     *
     * 字段语义与 [CfirExtendSemanticModel] 一致；source 路径由 model 直取，
     * 库路径由声明节点派生（见 [fromDeclaration]）。
     */
    private class ExtendAccessView(
        /**
         * extend 所在包名。
         */
        val packageFqName: FqName,
        /**
         * 被扩展真实目标的 classId。
         */
        val targetClassId: ClassId?,
        /**
         * extend 继承接口的 classId 列表。
         */
        val inheritedInterfaceClassIds: List<ClassId>,
        /**
         * 被扩展目标的规范化 key。
         */
        val targetKey: CfirExtendTargetKey?,
    ) {
        /**
         * 判断 extend 目标是否与 extend 声明处于同一包。
         *
         * 对齐官方 `IsExtendAccessible` 的 `isInSamePkg`：有目标声明时比较包名；
         * 无目标声明（primitive 目标）时按 std.core 特例处理。
         */
        fun isTargetInSamePackageAsExtend(): Boolean =
            targetClassId?.packageFqName == packageFqName ||
                targetClassId == null && targetKey != null && packageFqName.asString() == STDLIB_CORE_PACKAGE

        companion object {
            /**
             * 从语义模型直取（source 路径，EXTENSIONS 阶段已建立）。
             */
            fun fromModel(model: CfirExtendSemanticModel): ExtendAccessView = ExtendAccessView(
                packageFqName = model.packageFqName,
                targetClassId = model.targetClassId,
                inheritedInterfaceClassIds = model.inheritedInterfaceClassIds,
                targetKey = model.targetKey,
            )

            /**
             * 从声明节点即时派生（deserialized 库 extend 无语义模型）。
             *
             * 包名取自组合 extend provider（库 provider 反查声明包索引）；拿不到时返回
             * `null`，调用方维持宽松放行。库 extend 的类型引用已解析，cone 层展开等价于
             * 语义模型的 resolver 展开（见类 KDoc 差异说明）。
             */
            fun fromDeclaration(session: CfirSession, extend: CfirExtend): ExtendAccessView? {
                val packageFqName = session.extendProviderOrNull?.getPackageFqName(extend) ?: return null
                val extendedConeType = (extend.extendedTypeRef as? CfirResolvedTypeRef)?.coneType
                val inheritedInterfaceClassIds = extend.superTypeRefs.mapNotNull { superTypeRef ->
                    val coneType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
                    coneType.takeIf { it.isInterfaceShape() }?.expandedClassIdOrPrimitiveClassId
                }
                return ExtendAccessView(
                    packageFqName = packageFqName,
                    targetClassId = extendedConeType?.expandedClassIdOrPrimitiveClassId,
                    inheritedInterfaceClassIds = inheritedInterfaceClassIds,
                    targetKey = extendedConeType?.expandedExtendTargetKey,
                )
            }
        }
    }

    /**
     * 判断已解析类型自身是否携带接口形状。
     *
     * 与 `CfirExtendIndexStore.isInterfaceTypeShape` 语义一致的无 resolver 版本：
     * 库 extend 的 super type ref 在反序列化时已解析，cone 层的 interface 标记即最终判定依据。
     */
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

/**
 * 判断已解析类型自身是否携带接口形状。
 *
 * 与 `CfirExtendIndexStore.isInterfaceTypeShape` 语义一致的无 resolver 版本：
 * 库 extend 的 super type ref 在反序列化时已解析，cone 层的 interface 标记即最终判定依据。
 * 定义为顶层扩展：`ExtendAccessView.Companion` 的派生路径（companion 内无类实例）需要调用它。
 */
private fun ConeCangJieType.isInterfaceShape(): Boolean = when (this) {
    is ConeClassLikeType -> isInterface
    is ConeTypeAliasType -> expandedType?.isInterfaceShape() == true
    else -> false
}
