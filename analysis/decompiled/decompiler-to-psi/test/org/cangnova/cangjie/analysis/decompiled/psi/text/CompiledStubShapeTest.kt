package org.cangnova.cangjie.analysis.decompiled.psi.text

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.stubs.StubElement
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.decompiler.stub.CjoFileStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/** 二进制 Stub 与其反编译文本重建的源码 Stub 必须具有相同的节点类型及父子结构。 */
class CompiledStubShapeTest {
    @Test
    fun standardCoreBinaryMatchesRenderedSourceStubShape() {
        val disposable = Disposer.newDisposable("CompiledStubShapeTest")
        try {
            val environment = CangJieCoreEnvironment.createForTests(disposable)
            val repository = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
            val library = repository.resolve("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative")
            val manager = CjoManager(CjoSearchPath { if (it == "CANGJIE_LIBRARY") library.toString() else null })
            val snapshot = requireNotNull(manager.loadPackageSnapshot("std.core"))
            val module = Module()
            module.session.register(CfirSymbolProvider::class,
                CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module))
            val binary = CjoFileStubBuilder.buildFileStub(LoadedCjoPackage(
                LightVirtualFile("std.core.cjo"), FqName("std.core"), snapshot.pkg, snapshot.header,
                manager, true, snapshot.sourcePath,
            ), module)
            assertNotNull(binary.findChildStubByType(org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.CLASS),
                "必须实际物化标准库声明，不能用错误文件 Stub 通过空树比较")
            val rendered = buildDecompiledText(binary)
            val source = PsiFileFactory.getInstance(environment.project)
                .createFileFromText("stdCoreReparsed.cj", CangJieFileType.INSTANCE, rendered) as CjFile
            val rebuilt = source.calcStubTree().root as CangJieFileStubImpl
            val binaryShape = shape(binary)
            val sourceShape = shape(rebuilt)
            val firstDifference = (0 until minOf(binaryShape.size, sourceShape.size))
                .firstOrNull { binaryShape[it] != sourceShape[it] } ?: minOf(binaryShape.size, sourceShape.size)
            val start = (firstDifference - 5).coerceAtLeast(0)
            val end = firstDifference + 8
            val differingPath = binaryShape.getOrNull(firstDifference)?.substringBefore(':')
            val binaryAncestors = buildList {
                var current: StubElement<*> = binary
                differingPath?.split('/')?.forEach { index ->
                    current = current.childrenStubs[index.toInt()]
                    add(current.toString())
                }
            }
            assertTrue(binaryShape == sourceShape,
                "First difference at $firstDifference; binary=${binaryShape.size}, source=${sourceShape.size}\n" +
                    "binary ancestors: ${binaryAncestors.joinToString(" -> ")}\n" +
                    "binary:\n${binaryShape.drop(start).take(end - start).joinToString("\n")}\n" +
                    "source:\n${sourceShape.drop(start).take(end - start).joinToString("\n")}")
        } finally {
            ApplicationManager.getApplication().runWriteAction { Disposer.dispose(disposable) }
        }
    }

    private fun shape(root: StubElement<*>): List<String> = buildList {
        fun visit(stub: StubElement<*>, path: String) {
            add("$path:${stub.stubType}")
            stub.childrenStubs.forEachIndexed { index, child -> visit(child, "$path/$index") }
        }
        root.childrenStubs.forEachIndexed { index, child -> visit(child, "$index") }
    }

    private class Module : CfirModuleData() {
        override val name = Name.identifier("compiled-shape-test")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "compiled-shape-test"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }
}
