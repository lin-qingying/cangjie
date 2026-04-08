package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.scopes.CfirPackageScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.SCOPE_DUMP
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.util.MultiModuleInfoDumper
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.withIndent
import java.io.File

class CfirScopeDumpHandler(testServices: TestServices) : CfirAnalysisHandler(testServices) {
    private val dumper = MultiModuleInfoDumper()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            val hasScopeDumpDirective = SCOPE_DUMP in currentModule.directives
            if (!hasScopeDumpDirective) continue

            val fqNamesWithNames = currentModule.directives[SCOPE_DUMP]
            val printer = SmartPrinter(dumper.builderForModule(currentModule), indent = "  ")
            if (fqNamesWithNames.isEmpty()) {
                printer.processAllFilesScopeDump(part, currentModule)
                continue
            }

            val requests = fqNamesWithNames.map(::extractFqNameAndMemberNames)
            for ((fqName, names) in requests) {
                printer.processClassByFqName(
                    fqName = fqName,
                    namesFromDirective = names,
                    outputPart = part,
                    module = currentModule,
                )
            }
        }
    }

    private fun extractFqNameAndMemberNames(fqNameWithNames: String): Pair<String, List<String>> {
        val splitResult = fqNameWithNames.split(":").takeIf { it.size > 1 } ?: return fqNameWithNames to emptyList()
        return splitResult[0].trim() to splitResult[1].split(";").map(String::trim).filter(String::isNotEmpty)
    }

    private fun SmartPrinter.processAllFilesScopeDump(
        outputPart: CfirOutputPartForDependsOnModule,
        module: TestModule,
    ) {
        val mainFiles = outputPart.firFilesByTestFile
            .filterKeys { testFile -> !testFile.isAdditional && testFile in module.files }
            .entries
            .sortedBy { it.key.relativePath }
        if (mainFiles.isEmpty()) return

        outputPart.session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            for ((index, entry) in mainFiles.withIndex()) {
                if (index > 0) println()
                processFileScope(entry.key.relativePath, entry.value, outputPart)
            }
        }
    }

    private fun SmartPrinter.processFileScope(
        relativePath: String,
        cfirFile: CfirFile,
        outputPart: CfirOutputPartForDependsOnModule,
    ) {
        println("FILE: $relativePath")
        val packageScope = outputPart.session.cangjieScopeProvider.getPackageMemberScope(
            packageFqName = cfirFile.packageDirective.packageFqName,
            symbolProvider = outputPart.session.symbolProvider,
            useSiteSession = outputPart.session,
            scopeSession = outputPart.scopeSession,
        )
        val entries = collectTopLevelScopeEntries(
            packageScope = packageScope,
            cfirFile = cfirFile,
            outputPart = outputPart,
        )
        if (entries.isEmpty()) {
            withIndent { println("<no scope entries>") }
            return
        }
        withIndent {
            for ((index, entry) in entries.withIndex()) {
                if (index > 0) println()
                processTopLevelScopeEntry(
                    entry = entry,
                    packageScope = packageScope,
                    outputPart = outputPart,
                )
            }
        }
    }

    private fun SmartPrinter.processClassByFqName(
        fqName: String,
        namesFromDirective: List<String>,
        outputPart: CfirOutputPartForDependsOnModule,
        module: TestModule,
    ) {
        val classId = fqNameToClassId(fqName)
        val classLikeSymbol = outputPart.session.symbolProvider.getClassLikeSymbolByClassId(classId)
        val classLike = classLikeSymbol?.cfir
            ?: assertions.fail { "Class $fqName not found in module ${module.name}" }
        val classLikeDeclaration = classLike as? CfirClassLikeDeclaration
            ?: assertions.fail { "$fqName is not class-like but ${CfirRenderer.withReadability().renderElementAsString(classLike).trim()}" }

        outputPart.session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            processClassLikeScopeByDeclaration(
                classLike = classLikeDeclaration,
                namesFromDirective = namesFromDirective,
                outputPart = outputPart,
                includeClassLikeLine = false,
            )
        }
    }

    private fun SmartPrinter.processClassLikeScopeByDeclaration(
        classLike: CfirClassLikeDeclaration,
        namesFromDirective: List<String>,
        outputPart: CfirOutputPartForDependsOnModule,
        includeClassLikeLine: Boolean,
    ) {
        val fqName = classLike.symbol.classId.asFqNameString()
        println("$fqName: ")

        val scope = createUseSiteMemberScope(classLike, outputPart)
        val names = namesFromDirective.takeIf { it.isNotEmpty() }?.map { Name.identifier(it) } ?: scope.getCallableNames()

        withIndent {
            if (includeClassLikeLine) {
                println("[ClassLike]:")
                withIndent {
                    printClassLikeInfo(classLike, scope, SymbolCounter())
                }
                println("[Members]:")
                withIndent {
                    var hasMembers = false
                    for (name in names.sortedBy(Name::asString)) {
                        val printed = processMembersByName(name, scope)
                        hasMembers = hasMembers || printed
                    }
                    if (!hasMembers) {
                        println("<none>")
                    }
                }
            } else {
                for (name in names.sortedBy(Name::asString)) {
                    processMembersByName(name, scope)
                }
            }
        }

        println()
    }

    private fun createUseSiteMemberScope(
        classLike: CfirClassLikeDeclaration,
        outputPart: CfirOutputPartForDependsOnModule,
    ): CfirTypeScope {
        return when (classLike) {
            is CfirClass -> outputPart.session.cangjieScopeProvider.getUseSiteMemberScope(
                classLike,
                outputPart.session,
                outputPart.scopeSession,
            )
            else -> {
                val symbol = classLike.symbol as? CfirClassLikeSymbol<*>
                    ?: return CfirTypeScope.Empty
                CfirClassUseSiteMemberScope(
                    symbol,
                    outputPart.session.symbolProvider,
                    outputPart.session.extendProvider,
                    outputPart.session.directSupertypeProviderOrNull,
                )
            }
        }
    }

    private sealed interface TopLevelScopeEntry {
        val name: Name
        val kind: String
    }

    private data class ClassLikeScopeEntry(
        override val name: Name,
        override val kind: String,
        val declaration: CfirClassLikeDeclaration,
    ) : TopLevelScopeEntry

    private data class CallableScopeEntry(
        override val name: Name,
        override val kind: String,
        val declaration: CfirCallableDeclaration,
    ) : TopLevelScopeEntry

    private fun collectTopLevelScopeEntries(
        packageScope: CfirPackageScope,
        cfirFile: CfirFile,
        outputPart: CfirOutputPartForDependsOnModule,
    ): List<TopLevelScopeEntry> {
        val entries = mutableListOf<TopLevelScopeEntry>()
        val names = linkedSetOf<Name>().apply {
            addAll(packageScope.getClassifierNames())
            addAll(packageScope.getCallableNames())
        }

        for (name in names.sortedBy(Name::asString)) {
            val classLikeSymbols = LinkedHashSet<CfirClassLikeSymbol<*>>()
            packageScope.processClassifiersByName(name) { symbol ->
                if (belongsToFile(symbol, cfirFile, outputPart)) {
                    classLikeSymbols += symbol
                }
            }

            for (symbol in classLikeSymbols) {
                if (!symbol.isBound) continue
                val declaration = symbol.cfir as? CfirClassLikeDeclaration ?: continue
                entries += ClassLikeScopeEntry(
                    name = name,
                    kind = classLikeKind(declaration),
                    declaration = declaration,
                )
            }

            val seenCallableSymbols = LinkedHashSet<CfirCallableSymbol<*>>()
            packageScope.processCallablesByName(name) { symbol ->
                if (!symbol.isBound || !belongsToFile(symbol, cfirFile, outputPart)) return@processCallablesByName
                if (!seenCallableSymbols.add(symbol)) return@processCallablesByName
                val declaration = symbol.cfir as? CfirCallableDeclaration ?: return@processCallablesByName
                entries += CallableScopeEntry(
                    name = name,
                    kind = callableKind(declaration),
                    declaration = declaration,
                )
            }

        }

        return entries
    }

    private fun belongsToFile(
        symbol: CfirSymbol<*>,
        cfirFile: CfirFile,
        outputPart: CfirOutputPartForDependsOnModule,
    ): Boolean = outputPart.session.symbolProvider.getContainingFile(symbol) == cfirFile

    private fun classLikeKind(declaration: CfirClassLikeDeclaration): String = when (declaration) {
        is CfirInterface -> "interface"
        is CfirClass -> "class"
        is CfirStruct -> "struct"
        is CfirEnum -> "enum"
        is CfirTypeAlias -> "typealias"
        else -> "classlike"
    }

    private fun callableKind(declaration: CfirCallableDeclaration): String = when (declaration) {
        is CfirProperty -> "property"
        is CfirFieldVariable -> "field"
        is CfirPatternVariable -> "pattern"
        is CfirMacroDeclaration -> "macro"
        is CfirFunction -> "function"

        is CfirEnumConstructor -> "enum-constructor"
        else -> "callable"
    }

    private fun SmartPrinter.processTopLevelScopeEntry(
        entry: TopLevelScopeEntry,
        packageScope: CfirPackageScope,
        outputPart: CfirOutputPartForDependsOnModule,
    ) {
        println("${entry.name.asString()}[${entry.kind}]:")
        withIndent {
            when (entry) {
                is ClassLikeScopeEntry -> {
                    val scope = createUseSiteMemberScope(entry.declaration, outputPart)
                    val counter = SymbolCounter()

                    println("[ClassLike]:")
                    withIndent {
                        printClassLikeInfo(entry.declaration, scope, counter)
                    }

                    println("[Members]:")
                    withIndent {
                        var hasMembers = false
                        for (name in scope.getCallableNames().sortedBy(Name::asString)) {
                            val printed = processMembersByName(name, scope)
                            hasMembers = hasMembers || printed
                        }
                        if (!hasMembers) {
                            println("<none>")
                        }
                    }
                }

                is CallableScopeEntry -> {
                    printTopLevelCallableInfo(entry.declaration, packageScope, SymbolCounter())
                }
            }
        }
    }

    private fun fqNameToClassId(fqName: String): ClassId {
        if ('$' in fqName) {
            assertions.fail {
                "SCOPE_DUMP does not support nested class FQNs: $fqName. " +
                    "Cangjie only keeps top-level class-like declarations now."
            }
        }

        val segments = fqName.split(".")
        if (segments.isEmpty() || segments.any(String::isBlank)) {
            assertions.fail { "Invalid SCOPE_DUMP class-like FQ name: $fqName" }
        }

        // 仓颉已经移除嵌套类语义，因此这里始终将最后一段解析为顶层 class-like 名称。
        val packageFqName = FqName.fromSegments(segments.dropLast(1))
        val className = segments.last()
        return ClassId(packageFqName, Name.identifier(className))
    }

    private class SymbolCounter {
        private val map = mutableMapOf<CfirSymbol<*>, Int>()
        private var counter = 0

        fun getIndex(symbol: CfirSymbol<*>): Int = map.computeIfAbsent(symbol) { counter++ }
    }

    private fun SmartPrinter.processFunctions(name: Name, scope: CfirTypeScope) {
        val functions = mutableListOf<CfirFunctionSymbol<*>>()
        scope.processFunctionsByName(name) { functions += it }
        for (function in functions) {
            processFunction(function, scope, SymbolCounter())
        }
    }

    private fun SmartPrinter.processFunction(symbol: CfirFunctionSymbol<*>, scope: CfirTypeScope, counter: SymbolCounter) {
        printInfo(symbol.cfir, scope, counter)
        scope.processDirectOverriddenFunctionsWithBaseScope(symbol) { overridden, baseScope ->
            withIndent {
                processFunction(overridden, baseScope, counter)
            }
            ProcessorAction.NEXT
        }
    }

    private fun SmartPrinter.processProperties(name: Name, scope: CfirTypeScope) {
        val properties = mutableListOf<CfirPropertySymbol>()
        scope.processPropertiesByName(name) { properties += it }
        for (property in properties) {
            processProperty(property, scope, SymbolCounter())
        }
    }

    private fun SmartPrinter.processProperty(symbol: CfirPropertySymbol, scope: CfirTypeScope, counter: SymbolCounter) {
        printInfo(symbol.cfir, scope, counter)
        withIndent {
            scope.processDirectOverriddenPropertiesWithBaseScope(symbol) { overridden, baseScope ->
                processProperty(overridden, baseScope, counter)
                ProcessorAction.NEXT
            }
        }
    }

    private fun SmartPrinter.processMembersByName(name: Name, scope: CfirTypeScope): Boolean {
        var hasMembers = false
        val seen = LinkedHashSet<CfirCallableSymbol<*>>()
        val callables = mutableListOf<CfirCallableSymbol<*>>()
        scope.processCallablesByName(name) { symbol ->
            if (seen.add(symbol)) {
                callables += symbol
            }
        }
        if (callables.isEmpty()) return false

        hasMembers = true
        for (callable in callables) {
            when (callable) {
                is CfirFunctionSymbol<*> -> processFunction(callable, scope, SymbolCounter())
                is CfirPropertySymbol -> processProperty(callable, scope, SymbolCounter())
                is CfirVariableSymbol<*> -> printInfo(callable.cfir, scope, SymbolCounter())
                else -> printInfo(callable.cfir, scope, SymbolCounter())
            }
        }

        return hasMembers
    }

    private fun SmartPrinter.printTopLevelCallableInfo(
        declaration: CfirCallableDeclaration,
        scope: CfirPackageScope,
        counter: SymbolCounter,
    ) {
        print("[${declaration.origin}]: ")
        val renderedDeclaration = CfirRenderer.withReadability().renderElementAsString(declaration).asSingleLine()
        print(renderedDeclaration)
        print(" from $scope")
        println(" [id: ${counter.getIndex(declaration.symbol)}]")
    }

    private fun SmartPrinter.printInfo(declaration: CfirCallableDeclaration, scope: CfirTypeScope, counter: SymbolCounter) {
        print("[${declaration.origin}]: ")
        val renderedDeclaration = CfirRenderer.withReadability().renderElementAsString(declaration).asSingleLine()
        print(renderedDeclaration)
        print(" from $scope")
        println(" [id: ${counter.getIndex(declaration.symbol)}]")
    }

    private fun SmartPrinter.printClassLikeInfo(
        declaration: CfirClassLikeDeclaration,
        scope: CfirTypeScope,
        counter: SymbolCounter,
    ) {
        print("[${declaration.origin}]: ")
        print(declaration.renderHeader())
        print(" from $scope")
        println(" [id: ${counter.getIndex(declaration.symbol)}]")
    }

    private fun String.asSingleLine(): String =
        lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString(" ")

    private fun CfirClassLikeDeclaration.renderHeader(): String {
        val rendered = CfirRenderer.withReadability().renderElementAsString(this)
        val header = rendered.substringBefore("{").lineSequence().firstOrNull()?.trim().orEmpty()
        return if (header.isNotEmpty()) header else rendered.asSingleLine()
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension(".overrides.txt")
        val actualDump = dumper.generateResultingDump()
        if (dumper.isEmpty()) {
            assertions.assertFileDoesntExist(expectedFile) { CfirDiagnosticsDirectives.SCOPE_DUMP.name }
        } else {
            assertions.assertEqualsToFile(expectedFile, actualDump)
        }
    }

    private fun File.withExtension(newExtension: String): File {
        val baseName = name.substringBeforeLast('.', name)
        return parentFile.resolve(baseName + newExtension)
    }
}
