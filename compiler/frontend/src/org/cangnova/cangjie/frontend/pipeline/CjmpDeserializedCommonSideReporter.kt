package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationPosition
import org.cangnova.cangjie.cfir.session.CfirCjmpCommonSideFacts
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpMappingStorageOrNull
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.messages.CompilerMessageLocation
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.name.ClassId

/**
 * CLI specific 编译中针对**反序列化 common part** 的 common 方向 CJMP 报告（计划 G20）。
 *
 * 源码 common（多模块测试、IDE）由 `CfirCjmpCommonSideChecker` 在 common 源上报告；CLI specific 编译时
 * common 来自 `--common-part-cjo`，声明没有 PSI/LightTree source。官方用 cjo 内嵌位置输出
 * `common.cj:行:列`（cjc 1.1.3 实测 `'common' function 'b' can not find 'specific' match ==> common.cj:4:1`），
 * 本仓库从 cjo 恢复的 [org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationPosition] 以编译消息外显，
 * 判据与源码路径共用 [CfirCjmpCommonSideFacts]。
 */
internal object CjmpDeserializedCommonSideReporter {
    /** 报告并返回是否存在错误。 */
    fun report(session: CfirSession, files: Collection<CfirFile>, messageCollector: MessageCollector): Boolean {
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)) return false
        if (session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return false
        val storage = session.cjmpMappingStorageOrNull ?: return false
        val refinementModules = session.moduleData.allRefinementDependencies.mapTo(HashSet()) { it.name }
        if (refinementModules.isEmpty()) return false

        val provider = session.dependenciesSymbolProvider
        var hasErrors = false
        for (packageFqName in files.map { it.packageDirective.packageFqName }.distinct()) {
            val names = provider.symbolNamesProvider
            val topLevel = buildList<CfirDeclaration> {
                names.getTopLevelClassifierNamesInPackage(packageFqName).orEmpty().forEach { name ->
                    provider.getClassLikeSymbolByClassId(ClassId(packageFqName, name))?.cfir?.let(::add)
                }
                names.getTopLevelCallableNamesInPackage(packageFqName).orEmpty().forEach { name ->
                    provider.getTopLevelCallableSymbols(packageFqName, name).forEach { add(it.cfir) }
                }
                addAll(session.extendProvider.getExtendsInPackage(packageFqName))
            }.distinct().filter { declaration ->
                declaration.origin != CfirDeclarationOrigin.Source &&
                        declaration.moduleData.name in refinementModules &&
                        (declaration as? CfirMemberDeclaration)?.status?.isCommon == true
            }

            for ((common, outer) in CfirCjmpCommonSideFacts.commonDeclarationsWithOuter(topLevel)) {
                val message = when {
                    CfirCjmpCommonSideFacts.hasMultipleImplementations(common, storage) ->
                        "'common' ${CfirCjmpCommonSideFacts.implementationKind(common)} has several specific implementations"

                    CfirCjmpCommonSideFacts.mustReportNotMatched(common, outer, storage) ->
                        "'common' ${CfirCjmpCommonSideFacts.declInfo(common, outer)} can not find 'specific' match"

                    else -> continue
                }
                val position = common.cjoDeclarationPosition
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    message,
                    position?.let { CompilerMessageLocation.create(it.filePath, it.line, it.column, null) },
                )
                hasErrors = true
            }
        }
        return hasErrors
    }
}
