package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.reportCjmpCommonSideDeclaration
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationSourceElement
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.defaultCfirCjmpCommonSideFacts
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.name.ClassId

/** CLI Platform pass 收集真实 CJO common 诊断；二进制声明不进入源码或 LL 的 visitor。 */
class CfirCjmpBinaryDiagnosticsCollectorComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    private val commonSideFacts = defaultCfirCjmpCommonSideFacts

    override fun visitFile(file: CfirFile, data: CheckerContext) {
        if (!CjmpGate.isEnabled(data) || !session.cjmpSettings.compileSpecific) return
        if (file.moduleData.session !== session) return
        val packageName = file.packageDirective.packageFqName
        if (session.cfirProvider.getCfirFilesByPackage(packageName).firstOrNull() !== file) return
        val family = session.cjmpFamilyAggregate.ensureFamilyComplete(packageName)
        for ((common, outer) in deserializedCommonDeclarations(session, packageName)) {
            val source = checkNotNull(common.cjoDeclarationSourceElement) {
                "Deserialized CJMP common declaration ${common.symbol} has no CJO source element"
            }
            reportCjmpCommonSideDeclaration(
                common, outer, source, CjmpCjoDiagnosticContext(source, data), family, reporter,
                reportMissingImplementation = !session.cjmpSettings.compileCommon,
            )
        }
    }

    private fun deserializedCommonDeclarations(
        session: org.cangnova.cangjie.cfir.session.CfirSession,
        packageFqName: org.cangnova.cangjie.name.FqName,
    ): List<Pair<CfirDeclaration, CfirDeclaration?>> {
        val refinementModules = session.moduleData.allRefinementDependencies.toSet()
        if (refinementModules.isEmpty()) return emptyList()

        val provider = session.dependenciesSymbolProvider
        val topLevel = buildList<CfirDeclaration> {
            provider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName).orEmpty().forEach { name ->
                provider.getClassLikeSymbolsByClassId(ClassId(packageFqName, name)).forEach { add(it.cfir) }
            }
            provider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName).orEmpty().forEach { name ->
                provider.getTopLevelCallableSymbols(packageFqName, name).forEach { add(it.cfir) }
            }
            addAll(session.extendProvider.getExtendsInPackage(packageFqName))
        }.distinctBy { it.symbol }.filter { declaration ->
            declaration.origin != CfirDeclarationOrigin.Source &&
                    declaration.moduleData in refinementModules &&
                    declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON)
        }
        return commonSideFacts.commonDeclarationsWithOuter(topLevel)
    }

    private class CjmpCjoDiagnosticContext(
        private val source: org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationSourceElement,
        private val context: CheckerContext,
    ) : DiagnosticContext {
        override val languageVersionSettings: org.cangnova.cangjie.LanguageVersionSettings
            get() = context.languageVersionSettings
        override val containingFilePath: String? get() = source.binaryFilePath
        override val isCrossFileDiagnostic: Boolean get() = true
        override fun isDiagnosticSuppressed(diagnostic: org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic): Boolean = false
    }
}
