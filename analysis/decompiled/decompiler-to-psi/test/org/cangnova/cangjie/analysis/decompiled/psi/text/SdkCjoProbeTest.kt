package org.cangnova.cangjie.analysis.decompiled.psi.text

import PackageFormat.Package as CjoPackage
import com.intellij.testFramework.LightVirtualFile
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.decompiler.stub.CjoDeclarationLoader
import org.cangnova.cangjie.analysis.decompiler.stub.CjoFileStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.common.DetachedCjoModuleData
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.DetachedCjoSession
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path

/** 临时探针：确认 SDK std.core.cjo 的 extend 索引与反编译文本是否包含 extend。 */
class SdkCjoProbeTest {
    @Test
    fun probe() {
        val modulesRoot = System.getenv("CANGJIE_MODULES_113")
            ?: "C:\\Users\\lin17\\.cangjie\\sdks\\cangjie-sdk-windows-x64-1.1.3\\cangjie\\modules\\windows_x86_64_cjnative"
        val cjo = Path.of(modulesRoot, "std", "std.core.cjo").toFile()
        val bytes = cjo.readBytes()
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        val loaded = LoadedCjoPackage(
            binaryFile = LightVirtualFile("std.core.cjo"),
            packageFqName = FqName("std.core"),
            pkg = pkg,
            header = CjoPackageHeader.fromPackage(pkg),
            cjoManager = CjoManager(CjoSearchPath { key -> if (key == "CANGJIE_STDLIB_MODULE") modulesRoot else null }),
            isVersionSupported = true,
        )
        val moduleData = DetachedCjoModuleData(
            FqName("std.core"),
            DetachedCjoSession(FqName("std.core"), LanguageVersionSettingsImpl.DEFAULT),
        )
        val header = loaded.header
        val hdr = "topLevelExtendIndices=" + header.topLevelExtendIndices.size +
            " nameToIndices=" + header.topLevelNameToIndices.size

        val declInfo = runCatching {
            val list = CjoDeclarationLoader.loadDeclarations(loaded, moduleData)
            list.groupingBy { it::class.simpleName ?: "?" }.eachCount().entries
                .sortedByDescending { it.value }.take(12)
                .joinToString { entry -> entry.key + "=" + entry.value }
        }.fold({ it }, { "FAILED: " + it::class.simpleName + ": " + it.message })

        val text = runCatching {
            val disposable = Disposer.newDisposable("probe")
            CangJieCoreEnvironment.createForTests(disposable)
            buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, moduleData))
        }.fold({ it }, { "FAILED: " + it::class.simpleName + ": " + it.message })

        val markerExtend = "extend "
        val markerArray = "extend Array"
        val out = buildString {
            appendLine(hdr)
            appendLine("declarations: " + declInfo)
            appendLine("textLength=" + text.length)
            appendLine("containsExtend=" + text.contains(markerExtend))
            appendLine("containsArrayExtend=" + text.contains(markerArray))
            val arrayIdx = text.indexOf("struct Array")
            appendLine("arrayDeclIdx=" + arrayIdx)
            if (arrayIdx >= 0) {
                appendLine("--- Array decl ---")
                appendLine(text.substring(arrayIdx, minOf(text.length, arrayIdx + 600)))
            }
            val extends = Regex("extend [A-Za-z0-9_.]+").findAll(text).map { it.value }.toList()
            appendLine("extends(first 40)=" + extends.take(40).joinToString(" | "))
            appendLine("--- first 600 chars ---")
            appendLine(text.take(600))
        }
        File("D:/code/intellij/cangjie/.dsh-tmp/sdk-probe.txt").writeText(out)
        File("D:/code/intellij/cangjie/.dsh-tmp/std.core.decompiled.full.cj").writeText(text)
    }
}
