package org.cangnova.cangjie.jvm.codegen.io

import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenOutput
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

/**
 * 将 JVM codegen artifact 写入文件系统或 jar 的工具。
 */
class JvmArtifactWriter {
    /**
     * 把所有 class artifact 写入指定输出目录，并返回写出的文件路径。
     */
    fun writeClasses(output: ChirJvmCodegenOutput, outputDirectory: Path): List<Path> {
        Files.createDirectories(outputDirectory)
        return output.classes.map { artifact ->
            val target = outputDirectory.resolve(artifact.relativePath.replace('/', targetSeparator))
            Files.createDirectories(target.parent)
            Files.write(target, artifact.bytes)
            target
        }
    }

    /**
     * 把所有 class artifact 打包为 jar，并在存在 main class 时写入 manifest。
     */
    fun writeJar(output: ChirJvmCodegenOutput, jarPath: Path): Path {
        jarPath.parent?.let(Files::createDirectories)
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            output.mainClassInternalName?.let { mainClass ->
                mainAttributes[Attributes.Name.MAIN_CLASS] = mainClass.replace('/', '.')
            }
        }
        JarOutputStream(Files.newOutputStream(jarPath), manifest).use { jar ->
            output.classes.forEach { artifact ->
                val entry = JarEntry(artifact.relativePath)
                jar.putNextEntry(entry)
                jar.write(artifact.bytes)
                jar.closeEntry()
            }
        }
        return jarPath
    }

    private companion object {
        /**
         * 当前宿主文件系统路径分隔符，用于将 JVM internal path 映射为本地路径。
         */
        val targetSeparator: Char = java.io.File.separatorChar
    }
}
