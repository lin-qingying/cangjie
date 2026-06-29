package org.cangnova.cangjie.test.testFramework

import com.intellij.lang.ParserDefinition
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile

/**
 * 表示 `CjParsingTestCase`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
abstract class CjParsingTestCase(
    /**
     * 保存 `dataPath`，供测试基础设施在测试执行期间读取或传递。
     */
    private val dataPath: String,
    /**
     * 保存 `fileExt`，供测试基础设施在测试执行期间读取或传递。
     */
    private val fileExt: String,
    /**
     * 保存 `fileType`，供测试基础设施在测试执行期间读取或传递。
     */
    private val fileType: com.intellij.openapi.fileTypes.LanguageFileType,
    private vararg val definitions: ParserDefinition,
) : CjPlatformLiteFixture() {

    /**
     * 提供 `createPsiFile` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected fun createPsiFile(name: String, text: String): PsiFile {
        val fileName = if (name.endsWith(".$fileExt")) name else "$name.$fileExt"
        val virtualFile = LightVirtualFile(fileName, fileType, text)
        return (psiFileFactory as PsiFileFactoryImpl).trySetupPsiForFile(virtualFile, fileType.language, true, false)!!
    }

    /**
     * 提供 `getTestName` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected fun getTestName(): String {
        return name.removePrefix("test")
    }

    /**
     * 提供 `getTestDataPath` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected open fun getTestDataPath(): String = dataPath

    /**
     * 执行 `setUp` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun setUp() {
        super.setUp()
        definitions.forEach { definition ->
            val language = definition.fileNodeType.language
            com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(language, definition)
        }
    }
}
