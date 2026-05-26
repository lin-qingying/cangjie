@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaImplementationDetail::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.session.builder

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBuiltinsModuleImpl
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneSourceModule
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneSessionBuilder
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

/**
 * 对位 Kotlin `StandaloneSessionBuilderAgainstStdlibTest`。
 *
 * Kotlin 把 stdlib 当普通 library binary root 挂进 standalone graph；
 * 仓颉则把 stdlib 视作随 SDK 自动注册的 builtins/binary 模块。
 *
 * 因而这里锁定仓颉自己的真实契约：
 * 1. source module 只要挂上主干 builtins module owner，就能恢复 `std.objectpool.ObjectPool` 与 `std.core.String`；
 * 2. standalone graph 里不能把 builtins module 丢掉，稳定模块名必须可找回。
 */
class StandaloneSessionBuilderAgainstStdlibTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-standalone/testData/stdlib",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(
            CaAnalysisApiDecompiledTestServiceRegistrar,
            StandaloneBuilderPlatformTestServiceRegistrar,
        )

    @Test
    fun sourceModuleResolvesStdlibBinaryAndBuiltinTypes(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        withStandaloneBuiltinsContext(mainFile, mainModule) { sourceFile, sourceModule, builtinsModule ->
            val context = CaStandaloneSessionBuilder(mainFile.project).build(sourceModule, builtinsModule)
            assertStdlibUsageResolves(sourceFile, context)
        }
    }

    @Test
    fun standaloneGraphRetainsBuiltinsModule(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        withStandaloneBuiltinsContext(mainFile, mainModule) { sourceFile, sourceModule, builtinsModule ->
            val context = CaStandaloneSessionBuilder(mainFile.project).build(sourceModule, builtinsModule)
            assertSame(builtinsModule, context.findModuleByStableName(builtinsModule.builtinsName))
            assertEquals(
                setOf("source", builtinsModule.builtinsName),
                context.allModules.mapNotNull { module -> module.stableModuleName }.toSet(),
            )
            assertStdlibUsageResolves(sourceFile, context)
        }
    }

    private fun withStandaloneBuiltinsContext(
        mainFile: CjFile,
        mainModule: CjTestModule,
        action: (sourceFile: CjFile, sourceModule: CaStandaloneSourceModule, builtinsModule: CaBuiltinsModule) -> Unit,
    ) {
        val languageVersionSettings = (mainModule.caModule as? CaSourceModule)?.languageVersionSettings
            ?: error("Standalone stdlib test expects a source module, got `${mainModule.caModule::class.qualifiedName}`.")
        val psiManager = PsiManager.getInstance(mainFile.project)
        val tempRoot = Files.createTempDirectory("standalone-stdlib")

        try {
            val sourceDirPath = Files.createDirectories(tempRoot.resolve("src"))
            val sourceFilePath = sourceDirPath.resolve("usage.cj")

            Files.writeString(
                sourceFilePath,
                """
                    package sample.standalone.stdlib

                    import std.objectpool.ObjectPool

                    func useStdlib(pool: ObjectPool<String>): String {
                        return ""
                    }
                """.trimIndent(),
            )

            val sourceRootItem = psiItem(psiManager, sourceDirPath)
            val sourceFile = psiFile(psiManager, sourceFilePath)
            /**
             * standalone source module 需要显式挂上 Analysis API 主干的 builtins module。
             *
             * `CaBuiltinsModuleImpl` 与 low-level builtins owner 共享同一逻辑模块身份：
             * 1. builtins contentScope 统一来自 `BuiltinsVirtualFileProvider`
             * 2. `equals/hashCode` 以 `CaBuiltinsModule` 语义收敛，而不是实例身份
             */
            val builtinsModule = CaBuiltinsModuleImpl(mainFile.project)
            val sourceModule = CaStandaloneSourceModule(
                name = "source",
                languageVersionSettings = languageVersionSettings,
                project = mainFile.project,
                psiRoots = listOf(sourceRootItem),
            ).apply {
                directRegularDependencies += builtinsModule
            }

            action(sourceFile, sourceModule, builtinsModule)
        } finally {
            Files.walk(tempRoot)
                .sorted(Comparator.reverseOrder())
                .forEach { path -> Files.deleteIfExists(path) }
        }
    }

    private fun assertStdlibUsageResolves(
        sourceFile: CjFile,
        context: org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneAnalysisContext,
    ) {
        assertBuiltinsBinaryVisible(context.project)
        val diagnosticsText = collectDiagnosticsText(sourceFile, context)
        val function = sourceFile.declarations.filterIsInstance<CjNamedFunction>().single { declaration ->
            declaration.name == "useStdlib"
        }

        context.analyze(function) {
            val functionSymbol = function.symbol as? CaNamedFunctionSymbol
                ?: error("Declaration `${function.text}` does not resolve to a named function symbol.")
            val parameterType = functionSymbol.valueParameters.single().returnType
            val returnType = functionSymbol.returnType

            val objectPoolType = assertTypeClassId(
                actualType = parameterType,
                expectedClassId = ClassId.fromString("std/objectpool/ObjectPool"),
                debugText = parameterType.debugText(diagnosticsText),
            )
            assertEquals(1, objectPoolType.typeArguments.size, objectPoolType.debugText(diagnosticsText))
            assertTypeClassId(
                actualType = objectPoolType.typeArguments.single(),
                expectedClassId = ClassId.fromString("std/core/String"),
                debugText = objectPoolType.typeArguments.single().debugText(diagnosticsText),
            )
            assertTypeClassId(
                actualType = returnType,
                expectedClassId = ClassId.fromString("std/core/String"),
                debugText = returnType.debugText(diagnosticsText),
            )
        }
    }

    private fun assertBuiltinsBinaryVisible(project: com.intellij.openapi.project.Project) {
        val binaryIndex = project.getService(CaDecompiledBinaryIndex::class.java)
        val objectPoolBinary = binaryIndex.findBuiltinsBinaryFile(FqName("std.objectpool"))
        val stringBinary = binaryIndex.findBuiltinsBinaryFile(FqName("std.core"))

        assertTrue(
            objectPoolBinary != null,
            "Standalone stdlib test expects builtins binary index to expose `std.objectpool`.",
        )
        assertTrue(
            stringBinary != null,
            "Standalone stdlib test expects builtins binary index to expose `std.core`.",
        )
    }

    private fun assertTypeClassId(
        actualType: org.cangnova.cangjie.analysis.api.types.CaType,
        expectedClassId: ClassId,
        debugText: String,
    ): CaClassLikeType {
        assertTrue(
            actualType is CaClassLikeType,
            "actualType=${actualType::class.qualifiedName}, rendered=$debugText",
        )
        actualType as CaClassLikeType
        assertEquals(expectedClassId, actualType.classId, "rendered=$debugText")
        return actualType
    }

    private fun collectDiagnosticsText(
        sourceFile: CjFile,
        context: org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneAnalysisContext,
    ): String {
        return context.analyze(sourceFile) {
            sourceFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .joinToString(
                    prefix = "[",
                    postfix = "]",
                ) { diagnostic ->
                    "${diagnostic.factoryName}@${diagnostic.psi.text}:${diagnostic.defaultMessage}"
                }
        }
    }

    private fun org.cangnova.cangjie.analysis.api.types.CaType.debugText(diagnosticsText: String): String {
        val classIdText = (this as? CaClassLikeType)?.classId?.asString()
        return buildString {
            append(this@debugText::class.qualifiedName)
            append('(')
            if (classIdText != null) {
                append("classId=")
                append(classIdText)
                append(", ")
            }
            append("text=")
            append(this@debugText)
            append(", diagnostics=")
            append(diagnosticsText)
            append(')')
        }
    }

    private fun psiItem(
        psiManager: PsiManager,
        path: Path,
    ): PsiFileSystemItem {
        val normalizedPath = path.toAbsolutePath().normalize().toString().replace('\\', '/')
        val virtualFile = StandardFileSystems.local().findFileByPath(normalizedPath)
            ?: StandardFileSystems.local().refreshAndFindFileByPath(normalizedPath)
            ?: error("Cannot restore VirtualFile for `${path}`.")

        return if (path.toFile().isDirectory) {
            psiManager.findDirectory(virtualFile)
                ?: error("Cannot restore PSI directory for `${path}`.")
        } else {
            psiManager.findFile(virtualFile)
                ?: error("Cannot restore PSI file for `${path}`.")
        }
    }

    private fun psiFile(
        psiManager: PsiManager,
        path: Path,
    ): CjFile {
        val psiFile = psiItem(psiManager, path)
        return psiFile as? CjFile
            ?: error("Expected `${path}` to restore as CjFile, got `${psiFile::class.qualifiedName}`.")
    }
}
