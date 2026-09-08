package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.PersistentCheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.services.reachesClassId
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.extendRuleQueryService
import org.cangnova.cangjie.cfir.session.importBindingStore
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 消费包导入扩展复查的语义边界与诊断归属。
 *
 * [participatingExtends] 已经按消费包可见性过滤，源码包中的本地扩展不能反向污染库扩展
 * 的一致性检查。源码声明仍标记原声明；CJO 没有可标记的源文件时，标记使该扩展进入
 * 当前消费包的实际 import item。该位置选择不决定扩展是否参加语义检查。
 */
internal class CfirImportedExtendCheckContext(
    val participatingExtends: Set<CfirExtend>,
    private val extend: CfirExtend,
    private val useSiteFile: CfirFile,
    private val context: CheckerContext,
) : DiagnosticContext {
    private data class Location(val file: CfirFile, val importSource: CjSourceElement?)

    /** 只在确实产生诊断时定位导入，不要求无冲突的隐式标准库扩展拥有源码 import。 */
    private val location: Location by lazy(LazyThreadSafetyMode.NONE) {
        val declarationFile = context.session.extendProvider.getContainingFile(extend)
        if (declarationFile != null) {
            Location(declarationFile, null)
        } else {
            locateImport()
        }
    }

    private val declarationContext: CheckerContext by lazy(LazyThreadSafetyMode.NONE) {
        PersistentCheckerContext(context.sessionHolder, context.returnTypeCalculator)
            .enterFile(location.file)
            .addDeclaration(extend)
    }

    override val languageVersionSettings: LanguageVersionSettings get() = context.languageVersionSettings
    override val containingFilePath: String? get() = location.file.sourceFile?.path
    override val isCrossFileDiagnostic: Boolean get() = location.file !== useSiteFile
    override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean =
        declarationContext.isDiagnosticSuppressed(diagnostic)

    fun sourceForDeclaration(source: AbstractCjSourceElement?): AbstractCjSourceElement? =
        if (location.importSource != null) location.importSource else source

    /** 普通目标成员始终属于目标声明图；扩展输入只能来自本次可见导入集合。 */
    fun accepts(sourceExtend: CfirExtend?): Boolean =
        sourceExtend == null || sourceExtend in participatingExtends

    private fun locateImport(): Location {
        val session = context.session
        val provider = session.extendProvider
        val ownerPackage = provider.getPackageFqName(extend)
        val ownerTypes = exportedTypeIds(extend).filter { it.packageFqName == ownerPackage }.toSet()
        val groupTypes = participatingExtends.flatMap(::exportedTypeIds).toSet()
        val bindings = session.importBindingStore.requireBindings(useSiteFile).imports
        // 优先定位当前库扩展的导出入口；隐式标准库扩展发生冲突时定位引入同组冲突的导入。
        val binding = bindings.asSequence()
            .filter { it.lookupOrigin == CfirLookupOrigin.EXPLICIT_IMPORT && it.importDirective.source != null }
            .mapNotNull { candidate ->
                val priority = when {
                    ownerTypes.any { candidate.reachesClassId(session, it) } -> 0
                    groupTypes.any { candidate.reachesClassId(session, it) } -> 1
                    else -> return@mapNotNull null
                }
                candidate to priority
            }
            .minByOrNull { it.second }
            ?.first
        checkNotNull(binding) { "Visible imported extend conflict has no importing source: $ownerPackage" }
        val import = binding.importDirective
        val ownerFile = if (import in useSiteFile.imports) {
            useSiteFile
        } else {
            checkNotNull(session.cfirProvider.getCfirFilesByPackage(useSiteFile.packageDirective.packageFqName)
                .firstOrNull { import in it.imports }) {
                "Package-visible import has no source owner: ${import.importedFqName}"
            }
        }
        return Location(ownerFile, checkNotNull(import.source))
    }

    private fun exportedTypeIds(declaration: CfirExtend): List<ClassId> = buildList {
        declaration.extendedTypeRef.coneTypeOrNull?.classIdOrPrimitiveClassId?.let(::add)
        addAll(context.session.extendRuleQueryService.inheritedInterfaceClassIdsOf(declaration))
    }
}
