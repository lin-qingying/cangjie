package org.cangnova.cangjie.test.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.TestMetadata
import org.cangnova.cangjie.utils.convertLineSeparators
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.regex.Pattern
import kotlin.io.path.createTempDirectory

/**
 * 提供 `CjTestUtil` 单例，集中承载测试工具的共享状态、常量或默认行为。
 */
object CjTestUtil {
    /**
     * 保存 `PLEASE_REGENERATE_TESTS`，供测试工具在测试执行期间读取或传递。
     */
    private const val PLEASE_REGENERATE_TESTS = "Please regenerate tests"
    /**
     * 保存 `homeDir`，供测试工具在测试执行期间读取或传递。
     */
    private val homeDir: String = computeHomeDirectory()

    /**
     * 执行 `tmpDirForTest` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun tmpDirForTest(testClassName: String, testName: String): File {
        return normalizeFile(createTempDirectory("${testClassName}_${testName}_").toFile())
    }

    /**
     * 执行 `tmpDir` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun tmpDir(name: String): File {
        return normalizeFile(createTempDirectory("${name}_").toFile())
    }

    /**
     * 执行 `tmpDir` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun tmpDir(parentDir: File, name: String): File {
        mkdirs(parentDir)
        return normalizeFile(createTempDirectory(parentDir.toPath(), "${name}_").toFile())
    }

    /**
     * 执行 `tmpDirForReusableFolder` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun tmpDirForReusableFolder(name: String): File {
        val tmpRoot = File(System.getProperty("java.io.tmpdir"))
        mkdirs(tmpRoot)
        val dir = File(tmpRoot, name)
        mkdirs(dir)
        return normalizeFile(dir)
    }

    /**
     * 执行 `createFile` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun createFile(name: String, text: String, project: Project): CjFile {
        val shortName = name.substringAfterLast('/').substringAfterLast('\\')
        val virtualFile = LightVirtualFile(shortName, CangJieLanguage, text.convertLineSeparators())
        virtualFile.charset = StandardCharsets.UTF_8

        val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
        return factory.trySetupPsiForFile(virtualFile, CangJieLanguage, true, false) as CjFile
    }

    /**
     * 执行 `doLoadFile` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun doLoadFile(basePath: String, name: String): String = doLoadFile(File(basePath, name))

    /**
     * 执行 `doLoadFile` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun doLoadFile(file: File): String {
        try {
            return file.readText(Charsets.UTF_8)
        } catch (e: FileNotFoundException) {
            val fullPath = file.absolutePath
            throw IOException(
                "Ensure working directory is project root.\n\t$fullPath (No such file or directory)",
                e,
            )
        } catch (e: IOException) {
            throw e
        }
    }

    /**
     * 执行 `getFilePath` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun getFilePath(file: File): String = file.path.replace('\\', '/')

    /**
     * 执行 `getTestDataPathBase` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun getTestDataPathBase(): String = "$homeDir/compiler/testData"

    /**
     * 执行 `getHomeDirectory` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun getHomeDirectory(): String = homeDir

    /**
     * 执行 `mkdirs` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun mkdirs(file: File) {
        if (file.isDirectory) return
        if (!file.mkdirs()) {
            if (file.exists()) error("Failed to create $file: file exists and is not a directory")
            error("Failed to create $file")
        }
    }

    /**
     * 执行 `assertAllTestsPresentByMetadataWithExcluded` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun assertAllTestsPresentByMetadataWithExcluded(
        testCaseClass: Class<*>,
        testDataDir: File,
        filenamePattern: Pattern,
        excludedPattern: Pattern?,
        recursive: Boolean,
        vararg excludeDirs: String,
    ) {
        val rootFile = File(getTestsRoot(testCaseClass))
        val filePaths = collectPathsMetadata(testCaseClass)
        val exclude = excludeDirs.toSet()

        val files = testDataDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (recursive && containsTestData(file, filenamePattern, excludedPattern) && file.name !in exclude) {
                    assertTestClassPresentByMetadata(testCaseClass, file)
                }
            } else {
                val excluded = excludedPattern?.matcher(file.name)?.matches() == true
                if (!excluded && filenamePattern.matcher(file.name).matches()) {
                    assertFilePathPresent(file, rootFile, filePaths)
                }
            }
        }
    }

    /**
     * 执行 `assertAllTestsPresentByMetadata` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun assertAllTestsPresentByMetadata(
        testCaseClass: Class<*>,
        testDataDir: File,
        filenamePattern: Pattern,
        recursive: Boolean,
        vararg excludeDirs: String,
    ) {
        assertAllTestsPresentByMetadataWithExcluded(
            testCaseClass = testCaseClass,
            testDataDir = testDataDir,
            filenamePattern = filenamePattern,
            excludedPattern = null,
            recursive = recursive,
            excludeDirs = excludeDirs,
        )
    }

    /**
     * 执行 `assertAllTestsPresentInSingleGeneratedClass` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun assertAllTestsPresentInSingleGeneratedClass(
        testCaseClass: Class<*>,
        testDataDir: File,
        filenamePattern: Pattern,
    ) {
        assertAllTestsPresentInSingleGeneratedClass(
            testCaseClass = testCaseClass,
            testDataDir = testDataDir,
            filenamePattern = filenamePattern,
            excludePattern = null,
        )
    }

    /**
     * 执行 `assertAllTestsPresentInSingleGeneratedClass` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun assertAllTestsPresentInSingleGeneratedClass(
        testCaseClass: Class<*>,
        testDataDir: File,
        filenamePattern: Pattern,
        excludePattern: Pattern?,
    ) {
        val rootFile = File(getTestsRoot(testCaseClass))
        val filePaths = collectPathsMetadata(testCaseClass)

        testDataDir.walkTopDown().forEach { file ->
            val excluded = excludePattern?.matcher(file.name)?.matches() == true
            if (file.isFile && !excluded && filenamePattern.matcher(file.name).matches()) {
                assertFilePathPresent(file, rootFile, filePaths)
            }
        }
    }

    /**
     * 执行 `getTestsRoot` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun getTestsRoot(testCaseClass: Class<*>): String {
        val metadata = testCaseClass.getAnnotation(TestMetadata::class.java)
            ?: error("No metadata for class: $testCaseClass")
        return metadata.value
    }

    /**
     * 执行 `nameToCompare` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun nameToCompare(name: String): String {
        val normalized = name.replace('\\', '/')
        return if (isFileSystemCaseSensitive()) normalized else normalized.lowercase(Locale.ROOT)
    }

    /**
     * 执行 `getMethodMetadata` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    @JvmStatic
    fun getMethodMetadata(method: java.lang.reflect.Method): String? {
        return method.getAnnotation(TestMetadata::class.java)?.value
    }

    /**
     * 提供 `computeHomeDirectory` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun computeHomeDirectory(): String {
        val userDir = System.getProperty("user.dir") ?: "."
        return File(userDir).absoluteFile.normalize().path.replace('\\', '/')
    }

    /**
     * 提供 `normalizeFile` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun normalizeFile(file: File): File {
        return file.canonicalFile
    }

    /**
     * 提供 `isFileSystemCaseSensitive` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun isFileSystemCaseSensitive(): Boolean {
        val os = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
        return !os.contains("windows")
    }

    /**
     * 提供 `assertFilePathPresent` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun assertFilePathPresent(file: File, rootFile: File, filePaths: Set<String>) {
        val relative = relativePath(rootFile, file) ?: return
        val normalized = nameToCompare(relative)
        check(normalized in filePaths) {
            "Test data file missing from generated test class: $file\n$PLEASE_REGENERATE_TESTS"
        }
    }

    /**
     * 提供 `collectPathsMetadata` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun collectPathsMetadata(testCaseClass: Class<*>): Set<String> {
        return collectMethodsMetadata(testCaseClass).mapTo(linkedSetOf(), ::nameToCompare)
    }

    /**
     * 提供 `collectMethodsMetadata` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun collectMethodsMetadata(testCaseClass: Class<*>): Set<String> {
        return testCaseClass.declaredMethods
            .mapNotNull(::getMethodMetadata)
            .toSet()
    }

    /**
     * 提供 `containsTestData` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun containsTestData(dir: File, filenamePattern: Pattern, excludedPattern: Pattern?): Boolean {
        val files = dir.listFiles() ?: return false
        for (file in files) {
            if (file.isDirectory) {
                if (containsTestData(file, filenamePattern, excludedPattern)) return true
            } else {
                val excluded = excludedPattern?.matcher(file.name)?.matches() == true
                if (!excluded && filenamePattern.matcher(file.name).matches()) return true
            }
        }
        return false
    }

    /**
     * 提供 `assertTestClassPresentByMetadata` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun assertTestClassPresentByMetadata(outerClass: Class<*>, testDataDir: File) {
        for (nestedClass in outerClass.declaredClasses) {
            val metadata = nestedClass.getAnnotation(TestMetadata::class.java) ?: continue
            if (getFilePath(testDataDir).endsWith(metadata.value)) {
                return
            }
        }
        error("Test data directory missing from generated test class: $testDataDir\n$PLEASE_REGENERATE_TESTS")
    }

    /**
     * 提供 `relativePath` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun relativePath(root: File, file: File): String? {
        val rootPath = root.canonicalFile.toPath()
        val filePath = file.canonicalFile.toPath()
        if (!filePath.startsWith(rootPath)) return null
        return rootPath.relativize(filePath).toString().replace('\\', '/')
    }
}
