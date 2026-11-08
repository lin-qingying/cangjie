package org.cangnova.cangjie.analysis.decompiled.psi.text

import PackageFormat.Package as CjoPackage
import com.intellij.testFramework.LightVirtualFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.decompiler.stub.CjoFileStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.common.DetachedCjoModuleData
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.DetachedCjoSession
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path

/**
 * 真实 SDK `.cjo` 的反编译**自足**契约（不依赖 IDE、不依赖宿主 project）。
 *
 * IDE（STUBS）把 `.cjo` 反编译成 stub 后交给 Analysis API；因此反编译文本与 stub 的语义
 * 必须由本仓库自己保证正确，而不是让 IDE 侧补信息。这里直接对 SDK 的 `std.io` / `std.core`
 * 走 `CjoFileStubBuilder` + `buildDecompiledText`，锁定两条契约：
 *
 * 1. `std.io.ByteBuffer` 的签名里不得出现 `std.core.Any`。`Byte` 是 std.core 的 typealias，
 *    `.cjo` 用 `TypeKind.Type` 记录它；一旦反序列化把 typealias 当错误类型，反编译文本就会把
 *    `Array<Byte>` 渲染成 `Array<Any>`（症状：使用点报 RETURN_TYPE_MISMATCH）。
 * 2. `std.core` 的泛型 extend（如 `Array<T>` 上的 `size`）必须出现在反编译文本里，
 *    否则 STUBS 模式下 `array.size` 会报 not-a-member。
 */
class RealSdkDecompiledSignatureTest {
    /**
     * `std.io.ByteBuffer` 的方法签名不得退化成 `std.core.Any`。
     */
    @Test
    fun byteBufferSignatureKeepsByteAlias() = withCoreEnvironment {
        val text = decompile("std", "std.io.cjo", "std.io")
        dump("std.io.decompiled.cj", text)

        val byteBuffer = sectionOf(text, "class ByteBuffer")
        assumeTrue(byteBuffer != null, "反编译文本里没有 ByteBuffer：$text")
        assertFalse(
            byteBuffer!!.contains("std.core.Any"),
            "ByteBuffer 签名出现 std.core.Any（Byte typealias 未解析）：\n$byteBuffer",
        )
        assertTrue(
            Regex("func bytes\\(\\)\\s*:\\s*[^\\n]*Array<").containsMatchIn(byteBuffer),
            "ByteBuffer.bytes() 的返回类型缺失：\n$byteBuffer",
        )
        assertFalse(
            byteBuffer.contains("typealias "),
            "反编译文本出现了不可解析的 `typealias ` 关键字（别名应渲染为限定名 + 实参）：\n$byteBuffer",
        )
        assertFalse(
            byteBuffer.contains("std/core/"),
            "反编译文本出现 `/` 分隔的包路径（不可解析）：\n$byteBuffer",
        )
    }

    /**
     * `std.core` 的 `Array<T>` 泛型 extend（提供 `size` 等成员）必须出现在反编译文本里。
     */
    @Test
    fun arrayExtendMembersAreDecompiled() = withCoreEnvironment {
        val text = decompile("std", "std.core.cjo", "std.core")
        dump("std.core.decompiled.cj", text)

        // 真实 std.core 里 Array 有多个 extend（无条件 extend / `<: Collection` / `<: ToString` / `<: Equatable`）。
        // `size` 声明在 `<: Collection` 的那个 extend 里，必须逐段扫描，不能只看第一个。
        val arrayExtends = Regex("extend\\s+[^\\n]*\\bArray\\b").findAll(text).map { match ->
            val start = match.range.first
            val end = text.indexOf("\n}\n", start).let { if (it < 0) text.length else it + 2 }
            text.substring(start, end)
        }.toList()
        assertTrue(arrayExtends.isNotEmpty(), "反编译文本里没有 Array 的 extend 声明：${text.take(2000)}")
        val collectionExtend = arrayExtends.firstOrNull { extend -> extend.contains("<:") && extend.contains("Collection") }
        assertTrue(
            collectionExtend != null,
            "Array 的 extend 缺少 `<: Collection` 超类型（否则 size/isEmpty 不可见）：\n" +
                arrayExtends.joinToString("\n---\n"),
        )
        assertTrue(
            collectionExtend!!.contains("prop size") || collectionExtend.contains("func size"),
            "Array <: Collection 的 extend 里没有 size 成员（数组长度访问会报 not-a-member）：\n$collectionExtend",
        )
    }

    /** 取以 [marker] 开头、到下一个顶层 `}` 或下一个同类声明为止的片段，便于按声明断言。 */
    private fun sectionOf(text: String, marker: String): String? {
        val start = text.indexOf(marker)
        if (start < 0) return null
        val end = text.indexOf("\n}\n", start).let { if (it < 0) text.length else it + 2 }
        return text.substring(start, end)
    }

    /** 反编译 SDK 中指定 `.cjo` 文件。 */
    private fun decompile(dir: String, fileName: String, packageName: String): String {
        val modulesRoot = System.getenv("CANGJIE_MODULES_113") ?: DEFAULT_SDK_MODULES
        val root = File(modulesRoot)
        assumeTrue(root.isDirectory, "SDK modules not found: $modulesRoot")
        val cjo = Path.of(modulesRoot, dir, fileName).toFile()
        assumeTrue(cjo.isFile, "SDK .cjo not found: $cjo")

        val bytes = cjo.readBytes()
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        val fqName = FqName(packageName)
        val loaded = LoadedCjoPackage(
            binaryFile = LightVirtualFile(fileName),
            packageFqName = fqName,
            pkg = pkg,
            header = CjoPackageHeader.fromPackage(pkg),
            cjoManager = CjoManager(
                CjoSearchPath { key -> if (key == "CANGJIE_STDLIB_MODULE") modulesRoot else null },
            ),
            isVersionSupported = true,
        )
        val moduleData = DetachedCjoModuleData(
            fqName,
            DetachedCjoSession(fqName, LanguageVersionSettingsImpl.DEFAULT),
        )
        return buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, moduleData))
    }

    /** 把反编译文本落盘，便于人工核对（不参与断言）。 */
    private fun dump(name: String, text: String) {
        runCatching { File("../.dsh-tmp", name).writeText(text) }
    }

    /**
     * 反编译文本渲染需要 IntelliJ `Application`（stub `getPsi` 链路），测试内创建最小核心环境。
     */
    private fun withCoreEnvironment(action: () -> Unit) {
        val disposable = Disposer.newDisposable("RealSdkDecompiledSignatureTest")
        try {
            CangJieCoreEnvironment.createForTests(disposable)
            action()
        } finally {
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction { Disposer.dispose(disposable) }
            } else {
                Disposer.dispose(disposable)
            }
        }
    }

    private companion object {
        const val DEFAULT_SDK_MODULES =
            "C:\\Users\\lin17\\.cangjie\\sdks\\cangjie-sdk-windows-x64-1.1.3\\cangjie\\modules\\windows_x86_64_cjnative"
    }
}


