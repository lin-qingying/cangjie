package org.cangnova.cangjie.jvm.codegen.api

import org.cangnova.cangjie.jvm.codegen.io.JvmArtifactWriter
import java.nio.file.Path

data class JvmClassFileArtifact(
    val internalName: String,
    val bytes: ByteArray,
) {
    val relativePath: String = "$internalName.class"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JvmClassFileArtifact) return false
        return internalName == other.internalName && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * internalName.hashCode() + bytes.contentHashCode()
}

data class ChirJvmCodegenOutput(
    val classes: List<JvmClassFileArtifact>,
    val mainClassInternalName: String? = null,
    val loweringTrace: List<String> = emptyList(),
)

fun ChirJvmCodegenOutput.writeClasses(
    outputDirectory: Path,
    writer: JvmArtifactWriter = JvmArtifactWriter(),
): List<Path> = writer.writeClasses(this, outputDirectory)

fun ChirJvmCodegenOutput.writeJar(
    jarPath: Path,
    writer: JvmArtifactWriter = JvmArtifactWriter(),
): Path = writer.writeJar(this, jarPath)

fun interface ChirToJvmCodeGenerator {
    fun generate(input: ChirJvmCodegenInput): ChirJvmCodegenOutput
}
