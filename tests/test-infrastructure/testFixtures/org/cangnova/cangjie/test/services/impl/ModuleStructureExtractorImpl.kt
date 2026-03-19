package org.cangnova.cangjie.test.services.impl

import org.cangnova.cangjie.utils.DFS

import org.cangnova.cangjie.test.Assertions
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.LanguageVersionSettingsBuilder
import org.cangnova.cangjie.test.directives.AdditionalFilesDirectives
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives
import org.cangnova.cangjie.test.directives.ModuleStructureDirectives
import org.cangnova.cangjie.test.directives.model.ComposedDirectivesContainer
import org.cangnova.cangjie.test.directives.model.ComposedRegisteredDirectives
import org.cangnova.cangjie.test.directives.model.Directive
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.ArtifactKind
import org.cangnova.cangjie.test.model.BackendKind
import org.cangnova.cangjie.test.model.DependencyDescription
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.DependencyRelation
import org.cangnova.cangjie.test.model.FrontendKind
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.model.TestModuleStructureImpl
import org.cangnova.cangjie.test.services.AbstractEnvironmentConfigurator
import org.cangnova.cangjie.test.services.AdditionalSourceProvider
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.test.services.DefaultRegisteredDirectivesProvider
import org.cangnova.cangjie.test.services.DefaultsProvider
import org.cangnova.cangjie.test.services.ExceptionFromModuleStructureTransformer
import org.cangnova.cangjie.test.services.ModuleStructureExtractor
import org.cangnova.cangjie.test.services.ModuleStructureTransformer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.defaultRegisteredDirectivesProvider
import org.cangnova.cangjie.test.services.defaultsProvider
import org.cangnova.cangjie.test.util.joinToArrayString
import java.io.File

/*
 * Rules of directives resolving:
 * - If no `MODULE` or `FILE` was declared in test then all directives belongs to module
 * - If `FILE` is declared, then all directives after it will belong to
 *   file until next `FILE` or `MODULE` directive will be declared
 * - All directives between `MODULE` and `FILE` directives belongs to module
 * - All directives before first `MODULE` are global and belongs to each declared module
 */
@OptIn(TestInfrastructureInternals::class)
class ModuleStructureExtractorImpl(
    testServices: TestServices,
    additionalSourceProviders: List<AdditionalSourceProvider>,
    moduleStructureTransformers: List<ModuleStructureTransformer>,
    private val environmentConfigurators: List<AbstractEnvironmentConfigurator>
) : ModuleStructureExtractor(testServices, additionalSourceProviders, moduleStructureTransformers) {
    companion object {
        private val allowedExtensionsForFiles = listOf(".kt", ".kts", ".java", ".js", ".mjs", ".config", ".xml",
            ".def", ".h", ".modulemap"  // native cinterop file extensions
        ) + CINTEROP_SOURCE_EXTENSIONS

        /*
         * ([^()\n]+) module name
         * \((.*?)\) module dependencies
         * (\((.*?)\))? module friendDependencies
         */
        private val moduleDirectiveRegex = """([^()\n]+)(\((.*?)\)(\((.*?)\))?)?""".toRegex()

        /**
         * This method could be used by tests which are not based on the [TestServices] infrastructure,
         * but still need to support multi-file multi-module tests.
         */
        fun parseModuleStructureWithoutService(testDataFile: File, vararg directivesContainers: DirectivesContainer): TestModuleStructure {
            val testServices = TestServices().apply {
                register(AssertionsService::class, JUnit5Assertions)
                val defaultsProvider = DefaultsProvider(
                    frontendKind = FrontendKind.NoFrontend,
                    backendKind = BackendKind.NoBackend,
                    defaultLanguageSettingsBuilder = LanguageVersionSettingsBuilder(),
                    targetPlatform = null,
                    artifactKind = ArtifactKind.NoArtifact,
                    targetBackend = null,
                    defaultDependencyKind = DependencyKind.Source
                )
                register(DefaultsProvider::class, defaultsProvider)
                register(DefaultRegisteredDirectivesProvider::class, DefaultRegisteredDirectivesProvider(RegisteredDirectives.Empty))
            }
            val directivesContainer = ComposedDirectivesContainer(
                listOf(
                    ModuleStructureDirectives,
                    LanguageSettingsDirectives,
                    *directivesContainers,
                )
            )
            val extractor = ModuleStructureExtractorImpl(
                testServices,
                additionalSourceProviders = emptyList(),
                moduleStructureTransformers = emptyList(),
                environmentConfigurators = emptyList()
            )
            return extractor.splitTestDataByModules(testDataFile.canonicalPath, directivesContainer)
        }
    }

    override fun splitTestDataByModules(
        testDataFileName: String,
        directivesContainer: DirectivesContainer,
    ): TestModuleStructure {
        val testDataFile = File(testDataFileName)
        val extractor = ModuleStructureExtractorWorker(listOf(testDataFile), directivesContainer)
        var result = extractor.splitTestDataByModules()
        for (transformer in moduleStructureTransformers) {
            result = try {
                transformer.transformModuleStructure(result, testServices.defaultsProvider)
            } catch (e: Throwable) {
                throw ExceptionFromModuleStructureTransformer(e, result)
            }
        }
        return result
    }

    private inner class ModuleStructureExtractorWorker(
        private val testDataFiles: List<File>,
        private val directivesContainer: DirectivesContainer,
    ) {
        private val assertions: Assertions
            get() = testServices.assertions

        private val defaultsProvider: DefaultsProvider
            get() = testServices.defaultsProvider

        private lateinit var currentTestDataFile: File

        private val defaultFileName: String
            get() = currentTestDataFile.name

        private var currentModuleName: String? = null
        private var currentModuleLanguageVersionSettingsBuilder: LanguageVersionSettingsBuilder = initLanguageSettingsBuilder()
        private var dependenciesOfCurrentModule = mutableListOf<DependencyDescription>()
        private var filesOfCurrentModule = mutableListOf<TestFile>()
        private val mutableFilesListPerModule = mutableMapOf<TestModule, MutableList<TestFile>>()

        private var currentFileName: String? = null
        private var currentSnippetNumber: Int = 1
        private var firstFileInModule: Boolean = true
        private var linesOfCurrentFile = mutableListOf<String>()
        private var endLineNumberOfLastFile = -1

        private var directivesBuilder = RegisteredDirectivesParser(directivesContainer, assertions)
        private var moduleDirectivesBuilder: RegisteredDirectivesParser = directivesBuilder
        private var fileDirectivesBuilder: RegisteredDirectivesParser? = null

        private var globalDirectives: RegisteredDirectives? = null

        private val modules = mutableListOf<TestModule>()

        private val moduleStructureDirectiveBuilder = RegisteredDirectivesParser(ModuleStructureDirectives, assertions)

        fun splitTestDataByModules(): TestModuleStructure {
            for (testDataFile in testDataFiles) {
                currentTestDataFile = testDataFile
                val lines = testDataFile.readLines()
                lines.forEachIndexed { lineNumber, line ->
                    val rawDirective = RegisteredDirectivesParser.parseDirective(line)
                    if (tryParseStructureDirective(rawDirective, lineNumber + 1)) {
                        linesOfCurrentFile.add(line)
                        return@forEachIndexed
                    }
                    tryParseRegularDirective(rawDirective)
                    linesOfCurrentFile.add(line)
                }
            }
            finishModule(lineNumber = -1)
            val sortedModules = sortModules(modules)
            checkCycles(modules)
            return TestModuleStructureImpl(sortedModules, testDataFiles).also {
                generateAdditionalFiles(it)
            }
        }

        private fun sortModules(modules: List<TestModule>): List<TestModule> {
            val moduleByName = modules.groupBy { it.name }.mapValues { (name, modules) ->
                modules.singleOrNull() ?: error("Duplicated modules with name $name")
            }
            return DFS.topologicalOrder(modules) { module ->
                module.allDependencies.map {
                    val moduleName = it.dependencyModuleName
                    moduleByName[moduleName] ?: error("Module \"$moduleName\" not found while observing dependencies of \"${module.name}\"")
                }
            }.asReversed()
        }

        private fun checkCycles(modules: List<TestModule>) {
            val visited = mutableSetOf<String>()
            for (module in modules) {
                val moduleName = module.name
                visited.add(moduleName)
                for (dependency in module.allDependencies) {
                    val dependencyName = dependency.dependencyModuleName
                    if (dependencyName == moduleName) {
                        error("Module $moduleName has dependency to itself")
                    }
                    if (dependencyName !in visited) {
                        error("There is cycle in modules dependencies. See modules: $dependencyName, $moduleName")
                    }
                }
            }
        }

        /*
         * returns [true] means that passed directive was module directive and line is processed
         */
        private fun tryParseStructureDirective(rawDirective: RegisteredDirectivesParser.RawDirective?, lineNumber: Int): Boolean {
            if (rawDirective == null) return false
            val (directive, values) = moduleStructureDirectiveBuilder.convertToRegisteredDirective(rawDirective) ?: return false
            when (directive) {
                ModuleStructureDirectives.MODULE -> {
                    /*
                     * There was previous module, so we should save it
                     */
                    if (currentModuleName != null) {
                        finishModule(lineNumber)
                    } else {
                        if (currentFileName != null) {
                            error("Defining `// FILE` before `// MODULE` is prohibited: it's unclear if the directives before the first `// FILE` are global- or module-specific")
                        }
                        finishGlobalDirectives()
                    }
                    val (moduleName, dependencies, friends) = splitRawModuleStringToNameAndDependencies(
                        values.joinToString(separator = " ")
                    )
                    currentModuleName = moduleName
                    val kind = defaultsProvider.defaultDependencyKind

                    fun String.toDependencyDescription(relation: DependencyRelation): DependencyDescription {
                        val dependantModule = modules.find { it.name == this } ?: error("Module $this not found")
                        return DependencyDescription(dependantModule, kind, relation)
                    }

                    dependencies.mapTo(dependenciesOfCurrentModule) { it.toDependencyDescription(DependencyRelation.RegularDependency) }
                    friends.mapTo(dependenciesOfCurrentModule) { it.toDependencyDescription(DependencyRelation.FriendDependency) }
                }
                ModuleStructureDirectives.SNIPPET -> {
                    fun snippetName() = "snippet_${"%03d".format(currentSnippetNumber)}"
                    if (currentModuleName == null) {
                        finishGlobalDirectives()
                    } else {
                        finishModule(lineNumber)

                        dependenciesOfCurrentModule.add(
                            DependencyDescription(modules.last(), DependencyKind.Source, DependencyRelation.FriendDependency)
                        )
                        currentSnippetNumber++
                    }
                    currentModuleName = snippetName()
                    currentFileName = "$currentModuleName.kts"
                }
                ModuleStructureDirectives.FILE -> {
                    if (currentFileName != null) {
                        finishFile(lineNumber)
                    } else {
                        resetFileCaches()
                    }
                    currentFileName = (values.first() as String).also(::validateFileName)
                }
                else -> return false
            }

            return true
        }

        private fun splitRawModuleStringToNameAndDependencies(moduleDirectiveString: String): ModuleNameAndDependencies {
            val matchResult = moduleDirectiveRegex.matchEntire(moduleDirectiveString)
                ?: error("\"$moduleDirectiveString\" doesn't matches with pattern \"moduleName(dep1, dep2)\"")
            val (name, _, dependencies, _, friends) = matchResult.destructured
            var dependenciesNames = dependencies.takeIf { it.isNotBlank() }?.split(" ") ?: emptyList()
            globalDirectives?.let { directives ->
                /*
                 * In old tests coroutine helpers was added as separate module named `support`
                 *   instead of additional files for current module. So to safe compatibility with
                 *   old testdata we need to filter this dependency
                 */
                if (AdditionalFilesDirectives.WITH_COROUTINES in directives) {
                    dependenciesNames = dependenciesNames.filter { it != "support" }
                }
            }
            val friendsNames = friends.takeIf { it.isNotBlank() }?.split(" ") ?: emptyList()

            val intersection = buildSet {
                addAll(dependenciesNames intersect friendsNames)
            }
            require(intersection.isEmpty()) {
                val m = if (intersection.size == 1) "module" else "modules"
                val names = if (intersection.size == 1) "`${intersection.first()}`" else intersection.joinToArrayString()
                """Module `$name` depends on $m $names with different kinds simultaneously"""
            }

            return ModuleNameAndDependencies(
                name,
                dependenciesNames,
                friendsNames,
            )
        }

        private fun finishGlobalDirectives() {
            globalDirectives = directivesBuilder.build().onEach { it.checkDirectiveApplicability(contextIsGlobal = true) }
            resetModuleCaches()
            resetFileCaches()
        }

        private fun Directive.checkDirectiveApplicability(
            contextIsGlobal: Boolean = false,
            contextIsModule: Boolean = false,
            contextIsFile: Boolean = false
        ) {
            when {
                applicability.forGlobal && contextIsGlobal -> return
                applicability.forModule && contextIsModule -> return
                applicability.forFile && contextIsFile -> return
            }
            val context = buildList {
                if (contextIsGlobal) add("Global")
                if (contextIsModule) add("Module")
                if (contextIsFile) add("File")
            }.joinToString("|")
            error("Directive $this has $applicability applicability but it declared in $context")
        }

        private fun finishModule(lineNumber: Int) {
            finishFile(lineNumber)
            val isImplicitModule = currentModuleName == null

            val defaultDirectives = testServices.defaultRegisteredDirectivesProvider.defaultDirectives
            val moduleDirectives = defaultDirectives + globalDirectives + moduleDirectivesBuilder.build()

            moduleDirectives.forEach { it.checkDirectiveApplicability(contextIsGlobal = isImplicitModule, contextIsModule = true) }

            val frontendKind = defaultsProvider.frontendKind

            currentModuleLanguageVersionSettingsBuilder.configureUsingDirectives(
                moduleDirectives, environmentConfigurators, useK2 = frontendKind == FrontendKinds.CFIR
            )
            val moduleName = currentModuleName
                ?: defaultDirectives[ModuleStructureDirectives.MODULE].firstOrNull()
                ?: DEFAULT_MODULE_NAME
            val testModule = TestModule(
                name = moduleName,
                files = filesOfCurrentModule,
                allDependencies = dependenciesOfCurrentModule,
                directives = moduleDirectives,
                languageVersionSettings = currentModuleLanguageVersionSettingsBuilder.build()
            )
            mutableFilesListPerModule[testModule] = filesOfCurrentModule
            modules += testModule
            firstFileInModule = true
            resetModuleCaches()
        }

        private fun finishFile(lineNumber: Int) {
            val actualDefaultFileName = if (currentModuleName == null) {
                defaultFileName
            } else {
                "module_${currentModuleName}_$defaultFileName"
            }
            val filename = currentFileName ?: actualDefaultFileName
            if (filesOfCurrentModule.any { it.name == filename }) {
                error("File with name \"$filename\" already defined in module ${currentModuleName ?: actualDefaultFileName}")
            }
            val directives = fileDirectivesBuilder?.build()?.onEach { it.checkDirectiveApplicability(contextIsFile = true) }
            val fileContent = buildString {
                for (i in 0 until endLineNumberOfLastFile) {
                    appendLine()
                }
                appendLine(linesOfCurrentFile.joinToString("\n"))
            }
            filesOfCurrentModule.add(
                TestFile(
                    relativePath = filename,
                    originalContent = fileContent,
                    originalFile = currentTestDataFile,
                    startLineNumberInOriginalFile = endLineNumberOfLastFile,
                    isAdditional = false,
                    directives = directives ?: RegisteredDirectives.Empty
                )
            )
            firstFileInModule = false
            endLineNumberOfLastFile = lineNumber - 1
            resetFileCaches()
        }

        private fun resetModuleCaches() {
            firstFileInModule = true
            currentModuleName = null
            currentModuleLanguageVersionSettingsBuilder = initLanguageSettingsBuilder()
            filesOfCurrentModule = mutableListOf()
            dependenciesOfCurrentModule = mutableListOf()
            resetDirectivesBuilder()
            moduleDirectivesBuilder = directivesBuilder
        }

        private fun resetDirectivesBuilder() {
            directivesBuilder = RegisteredDirectivesParser(directivesContainer, assertions)
        }

        private fun resetFileCaches() {
            if (!firstFileInModule) {
                linesOfCurrentFile = mutableListOf()
            }
            if (firstFileInModule) {
                moduleDirectivesBuilder = directivesBuilder
            }
            currentFileName = null
            resetDirectivesBuilder()
            fileDirectivesBuilder = directivesBuilder
        }

        private fun tryParseRegularDirective(rawDirective: RegisteredDirectivesParser.RawDirective?) {
            if (rawDirective == null) return
            val parsedDirective = directivesBuilder.convertToRegisteredDirective(rawDirective) ?: return
            directivesBuilder.addParsedDirective(parsedDirective)
        }

        private fun validateFileName(fileName: String) {
            if (!allowedExtensionsForFiles.any { fileName.endsWith(it) }) {
                assertions.fail {
                    "Filename $fileName is not valid. Allowed extensions: ${allowedExtensionsForFiles.joinToArrayString()}"
                }
            }
        }

        private fun initLanguageSettingsBuilder(): LanguageVersionSettingsBuilder {
            return defaultsProvider.newLanguageSettingsBuilder()
        }

        private fun generateAdditionalFiles(testModuleStructure: TestModuleStructure) {
            for ((module, files) in mutableFilesListPerModule) {
                additionalSourceProviders.flatMapTo(files) { additionalSourceProvider ->
                    additionalSourceProvider.produceAdditionalFiles(
                        globalDirectives ?: RegisteredDirectives.Empty,
                        module,
                        testModuleStructure
                    ).also { additionalFiles ->
                        require(additionalFiles.all { it.isAdditional }) {
                            "Files produced by ${additionalSourceProvider::class.qualifiedName} should have flag `isAdditional = true`"
                        }
                    }
                }
            }
        }
    }

    private data class ModuleNameAndDependencies(
        val name: String,
        val dependencies: List<String>,
        val friends: List<String>
    )
}

private operator fun RegisteredDirectives.plus(other: RegisteredDirectives?): RegisteredDirectives {
    return when {
        other == null -> this
        other.isEmpty() -> this
        this.isEmpty() -> other
        else -> ComposedRegisteredDirectives(this, other)
    }
}

inline fun <reified T : Enum<T>> valueOfOrNull(value: String): T? {
    for (enumValue in enumValues<T>()) {
        if (enumValue.name == value) {
            return enumValue
        }
    }
    return null
}
