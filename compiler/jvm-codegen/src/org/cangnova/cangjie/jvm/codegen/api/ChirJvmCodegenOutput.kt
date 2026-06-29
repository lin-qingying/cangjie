package org.cangnova.cangjie.jvm.codegen.api

import org.cangnova.cangjie.jvm.codegen.io.JvmArtifactWriter
import java.nio.file.Path

/**
 * JVM 后端生成的单个 class 文件。
 */
data class JvmClassFileArtifact(
    /**
     * JVM internal name，例如 `sample/FooCj`。
     */
    val internalName: String,
    /**
     * class 文件字节内容。
     */
    val bytes: ByteArray,
) {
    /**
     * 写入目录或 jar 时使用的相对路径。
     */
    val relativePath: String = "$internalName.class"

    /**
     * 基于 internal name 和字节数组内容比较 class artifact。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JvmClassFileArtifact) return false
        return internalName == other.internalName && bytes.contentEquals(other.bytes)
    }

    /**
     * 结合 internal name 和字节数组内容生成 hash code。
     */
    override fun hashCode(): Int = 31 * internalName.hashCode() + bytes.contentHashCode()
}

/**
 * JVM 后端完整输出。
 */
data class ChirJvmCodegenOutput(
    /**
     * 本次 codegen 生成的所有 class 文件。
     */
    val classes: List<JvmClassFileArtifact>,
    /**
     * 可作为 jar Main-Class 的 class internal name。
     */
    val mainClassInternalName: String? = null,
    /**
     * 可选 lowering 过程跟踪文本。
     */
    val loweringTrace: List<String> = emptyList(),
)

/**
 * 将 JVM codegen 输出写入 class 文件目录。
 */
fun ChirJvmCodegenOutput.writeClasses(
    outputDirectory: Path,
    writer: JvmArtifactWriter = JvmArtifactWriter(),
): List<Path> = writer.writeClasses(this, outputDirectory)

/**
 * 将 JVM codegen 输出写入 jar 文件。
 */
fun ChirJvmCodegenOutput.writeJar(
    jarPath: Path,
    writer: JvmArtifactWriter = JvmArtifactWriter(),
): Path = writer.writeJar(this, jarPath)

/**
 * CHIR 到 JVM class 文件的后端生成器接口。
 */
fun interface ChirToJvmCodeGenerator {
    /**
     * 根据 CHIR package 输入生成 JVM class 文件输出。
     */
    fun generate(input: ChirJvmCodegenInput): ChirJvmCodegenOutput
}
