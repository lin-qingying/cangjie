package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.DiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.ComposedRegisteredDirectives
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.util.MultiModuleInfoDumper

class CfirScopeDumpHandler(
    testServices: TestServices,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives, DiagnosticsDirectives)

    private val dumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            val directives = ComposedRegisteredDirectives(currentModule.directives, module.directives)
            val scopeDumpRequests = directives[CfirDiagnosticsDirectives.SCOPE_DUMP]
            val dumpBasicScope = CfirDiagnosticsDirectives.DUMP_SCOPE in directives
            if (!dumpBasicScope && scopeDumpRequests.isEmpty()) continue

            val builder = dumper.builderForModule(currentModule)
            var wroteContent = false

            if (dumpBasicScope) {
                part.firFilesByTestFile.forEach { (testFile, cfirFile) ->
                    builder.append(renderBasicFileDump(part, testFile, cfirFile)).appendLine()
                    wroteContent = true
                }
            }

            scopeDumpRequests
                .flatMap(::parseScopeDumpEntries)
                .forEachIndexed { index, request ->
                    if (wroteContent || index > 0) {
                        builder.appendLine()
                    }
                    builder.append(renderClassScopeDump(part, request)).appendLine()
                    wroteContent = true
                }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val directives = testServices.moduleStructure.allDirectives
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.cfirSideFile("scope.txt")

        if (CfirDiagnosticsDirectives.DUMP_SCOPE !in directives && CfirDiagnosticsDirectives.SCOPE_DUMP !in directives) {
            testServices.assertNoUnexpectedSideFile(expectedFile, CfirDiagnosticsDirectives.DUMP_SCOPE)
            return
        }

        if (dumper.isEmpty() && !expectedFile.exists()) return
        testServices.assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
    }

    private fun renderBasicFileDump(part: CfirOutputPartForDependsOnModule, testFile: TestFile, cfirFile: CfirFile): String {
        val packageScope = part.session.cangjieScopeProvider.getPackageMemberScope(
            cfirFile.packageDirective.packageFqName,
            part.session.symbolProvider,
            part.session,
            part.scopeSession,
        )

        return buildString {
            appendLine("FILE: ${testFile.relativePath}")
            appendLine("package: ${cfirFile.packageDirective.packageFqName}")

            if (cfirFile.imports.isEmpty()) {
                appendLine("imports: <none>")
            } else {
                appendLine("imports:")
                cfirFile.imports.forEach { import ->
                    val fqName = import.importedFqName?.asString() ?: "<unresolved>"
                    val alias = import.aliasName?.asString()?.let { " as $it" }.orEmpty()
                    val suffix = if (import.isAllUnder) ".*" else ""
                    appendLine("  - $fqName$suffix$alias")
                }
            }

            appendLine("declarations:")
            cfirFile.declarations.forEach { declaration ->
                appendLine("  - ${renderDeclaration(declaration)}")
            }

            val declaredNames = cfirFile.declarations.mapNotNull { declaration ->
                when (declaration) {
                    is org.cangnova.cangjie.cfir.declarations.CfirFunction -> declaration.symbol.name
                    is org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration -> declaration.symbol.name
                    is org.cangnova.cangjie.cfir.declarations.CfirProperty -> declaration.name
                    is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> declaration.name
                    is org.cangnova.cangjie.cfir.declarations.CfirValueParameter -> declaration.name
                    is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> declaration.name
                    is CfirClass -> declaration.symbol.name
                    is org.cangnova.cangjie.cfir.declarations.CfirTypeAlias -> declaration.symbol.name
                    else -> null
                }
            }.distinct().sortedBy { it.asString() }
            if (declaredNames.isEmpty()) {
                appendLine("package-scope: <no named declarations>")
            } else {
                appendLine("package-scope:")
                declaredNames.forEach { name ->
                    val classifiers = mutableListOf<String>()
                    val functions = mutableListOf<String>()
                    val properties = mutableListOf<String>()
                    packageScope.processClassifiersByName(name) { classifiers += renderClassSymbol(it) }
                    packageScope.processFunctionsByName(name) { functions += renderCallableSymbol(it) }
                    packageScope.processPropertiesByName(name) { properties += renderCallableSymbol(it) }
                    appendLine("  - ${name.asString()}")
                    appendLine("    classifiers: ${renderList(classifiers)}")
                    appendLine("    functions: ${renderList(functions)}")
                    appendLine("    properties: ${renderList(properties)}")
                }
            }
        }.trimEnd()
    }

    private fun renderClassScopeDump(part: CfirOutputPartForDependsOnModule, request: ScopeDumpRequest): String {
        val classId = ClassId.topLevel(FqName(request.classFqName))
        val classSymbol = part.session.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return "CLASS: ${request.classFqName}\n<unresolved class symbol>"
        val ownerClass = classSymbol.cfir as? CfirClass ?: return "CLASS: ${request.classFqName}\n<unsupported class-like symbol>"
        val scope = part.session.cangjieScopeProvider.getUseSiteMemberScope(ownerClass, part.session, part.scopeSession)

        return buildString {
            appendLine("CLASS: ${request.classFqName}")
            appendLine("scope: ${scope::class.simpleName ?: "<anonymous scope>"}")
            appendLine("callables:")
            val targetNames = request.memberNames?.map(Name::identifier)?.toSet()
                ?: scope.getCallableNames().sortedBy(Name::asString).toSet()
            if (targetNames.isEmpty()) {
                appendLine("  <no callable names>")
            } else {
                targetNames.forEach { name ->
                    appendLine("  ${name.asString()}")
                    appendCallableDump(scope, name)
                }
            }
        }.trimEnd()
    }

    private fun StringBuilder.appendCallableDump(scope: CfirTypeScope, name: Name) {
        val functions = mutableListOf<CfirFunctionSymbol<*>>()
        val properties = mutableListOf<CfirPropertySymbol>()
        scope.processFunctionsByName(name, functions::add)
        scope.processPropertiesByName(name, properties::add)
        appendLine("    functions: ${renderList(functions.map(::renderCallableSymbol))}")
        functions.distinctBy { it.toString() }.forEach { symbol ->
            appendOverrideChain(scope, symbol, depth = 3)
        }
        appendLine("    properties: ${renderList(properties.map(::renderCallableSymbol))}")
        properties.distinctBy { it.toString() }.forEach { symbol ->
            appendOverrideChain(scope, symbol, depth = 3)
        }
    }

    private fun StringBuilder.appendOverrideChain(scope: CfirTypeScope, symbol: CfirCallableSymbol<*>, depth: Int) {
        when (symbol) {
            is CfirFunctionSymbol<*> -> {
                val overridden = mutableListOf<Pair<CfirFunctionSymbol<*>, CfirTypeScope>>()
                scope.processDirectOverriddenFunctionsWithBaseScope(symbol) { overriddenSymbol, baseScope ->
                    overridden += overriddenSymbol to baseScope
                    ProcessorAction.NEXT
                }
                appendLine("      overrides(${renderCallableSymbol(symbol)}): ${renderList(overridden.map { renderCallableSymbol(it.first) })}")
                if (depth > 0) {
                    overridden.forEach { (overriddenSymbol, baseScope) ->
                        appendOverrideChain(baseScope, overriddenSymbol, depth - 1)
                    }
                }
            }
            is CfirPropertySymbol -> {
                val overridden = mutableListOf<Pair<CfirPropertySymbol, CfirTypeScope>>()
                scope.processDirectOverriddenPropertiesWithBaseScope(symbol) { overriddenSymbol, baseScope ->
                    overridden += overriddenSymbol to baseScope
                    ProcessorAction.NEXT
                }
                appendLine("      overrides(${renderCallableSymbol(symbol)}): ${renderList(overridden.map { renderCallableSymbol(it.first) })}")
                if (depth > 0) {
                    overridden.forEach { (overriddenSymbol, baseScope) ->
                        appendOverrideChain(baseScope, overriddenSymbol, depth - 1)
                    }
                }
            }
            else -> {
                appendLine("      overrides(${renderCallableSymbol(symbol)}): <unsupported callable kind>")
            }
        }
    }

    private fun parseScopeDumpEntries(rawValue: String): List<ScopeDumpRequest> {
        return rawValue.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { entry ->
                val classAndMembers = entry.split(':', limit = 2)
                val members = classAndMembers.getOrNull(1)
                    ?.split(';')
                    ?.map(String::trim)
                    ?.filter(String::isNotEmpty)
                    ?.distinct()
                    ?.takeIf(List<String>::isNotEmpty)
                ScopeDumpRequest(classAndMembers.first(), members)
            }
    }

    private fun renderList(items: List<String>): String = items.distinct().sorted().ifEmpty { listOf("<none>") }.joinToString()

    private fun renderDeclaration(declaration: Any): String = when (declaration) {
        is org.cangnova.cangjie.cfir.declarations.CfirFunction -> "function ${declaration.symbol.name.asString()}"
        is org.cangnova.cangjie.cfir.declarations.CfirProperty -> "property ${declaration.name.asString()}"
        is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> "field ${declaration.name.asString()}"
        is CfirClass -> "class ${declaration.symbol.name.asString()}"
        is org.cangnova.cangjie.cfir.declarations.CfirTypeAlias -> "typealias ${declaration.symbol.name.asString()}"
        else -> declaration::class.simpleName ?: "<anonymous declaration>"
    }

    private fun renderClassSymbol(symbol: CfirClassLikeSymbol<*>): String {
        return symbol.name.asString()
    }

    private fun renderCallableSymbol(symbol: CfirCallableSymbol<*>): String {
        return symbol.toString()
    }

    private data class ScopeDumpRequest(
        val classFqName: String,
        val memberNames: List<String>?,
    )
}
