package org.cangnova.cangjie.jvm.codegen.io

import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenOutput
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class JvmArtifactWriter {
    fun writeClasses(output: ChirJvmCodegenOutput, outputDirectory: Path): List<Path> {
        Files.createDirectories(outputDirectory)
        return output.classes.map { artifact ->
            val target = outputDirectory.resolve(artifact.relativePath.replace('/', targetSeparator))
            Files.createDirectories(target.parent)
            Files.write(target, artifact.bytes)
            target
        }
    }

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
        val targetSeparator: Char = java.io.File.separatorChar
    }
}
