package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.diagnostic.VisibilityError
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.FqName

/**
 * 可见性检查阶段。
 * 基于仓颉官方可见性规则实现：
 * - private: 同文件（顶级声明）或同类型（成员声明）可见
 * - internal: 同包及子包可见（仓颉默认可见性）
 * - protected: 同模块可见（简化为同包/子包 + 继承关系）
 * - public: 总是可见
 * 对齐官方编译器 `TypeCheckUtil.cpp#IsLegalAccess`。
 */
object CfirCheckVisibility : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val symbol = candidate.symbol
        if (!symbol.isBound) return

        val visibility = extractVisibility(symbol.cfir) ?: return

        // public 总是可见
        if (visibility === Visibilities.Public) return

        val declaringFilePath = extractDeclaringFilePath(symbol.cfir)
        val declaringPackage = extractDeclaringPackage(symbol.cfir)

        when {
            Visibilities.isPrivate(visibility) -> {
                // private 顶级声明：同文件可见
                // private 成员声明：同类型可见
                val isSameFile = context.containingFilePath != null &&
                    context.containingFilePath == declaringFilePath
                if (!isSameFile) {
                    sink.reportDiagnostic(VisibilityError(symbol))
                }
            }
            visibility === Visibilities.Internal -> {
                // 同包或子包可见
                if (!isInPackageOrSubpackage(context.containingPackageFqName, declaringPackage)) {
                    sink.reportDiagnostic(VisibilityError(symbol))
                }
            }
            visibility === Visibilities.Protected -> {
                // 同模块可见（简化：同包/子包可见）
                if (!isInPackageOrSubpackage(context.containingPackageFqName, declaringPackage)) {
                    sink.reportDiagnostic(VisibilityError(symbol))
                }
            }
        }
    }

    /** 从声明上提取可见性。 */
    private fun extractVisibility(declaration: CfirDeclaration): Visibility? {
        return when (declaration) {
            is CfirFunction -> declaration.status.visibility
            is CfirProperty -> declaration.status.visibility
            is CfirConstructor -> declaration.status.visibility
            is CfirVariable -> declaration.status.visibility
            is CfirClass -> declaration.status.visibility
            else -> null
        }
    }

    /** 从声明中提取所属文件路径。 */
    private fun extractDeclaringFilePath(declaration: CfirDeclaration): String? {
        return when (declaration) {
            is CfirFile -> declaration.sourceFile?.path
            else -> null // 成员声明暂无直接文件引用，后续通过反向映射补全
        }
    }

    /** 从声明中提取所属包名。 */
    private fun extractDeclaringPackage(declaration: CfirDeclaration): FqName? {
        // 当前 CFIR 树中声明没有直接引用所属包名的方式，
        // 后续可通过 moduleData 或声明到文件的反向映射补全。
        return null
    }

    /**
     * 检查使用点的包名是否在声明所属包或其子包中。
     * 当无法确定声明包名时，默认放行（不报错）。
     */
    private fun isInPackageOrSubpackage(useSite: FqName?, declaringPackage: FqName?): Boolean {
        if (useSite == null || declaringPackage == null) return true
        val useSiteStr = useSite.asString()
        val declStr = declaringPackage.asString()
        return useSiteStr == declStr || useSiteStr.startsWith("$declStr.")
    }
}
