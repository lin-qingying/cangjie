package org.cangnova.cangjie.test.testFramework

import com.intellij.lang.ParserDefinition
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile

abstract class CjParsingTestCase(
    private val dataPath: String,
    private val fileExt: String,
    private val fileType: com.intellij.openapi.fileTypes.LanguageFileType,
    private vararg val definitions: ParserDefinition,
) : CjPlatformLiteFixture() {

    protected fun createPsiFile(name: String, text: String): PsiFile {
        val fileName = if (name.endsWith(".$fileExt")) name else "$name.$fileExt"
        val virtualFile = LightVirtualFile(fileName, fileType, text)
        return (psiFileFactory as PsiFileFactoryImpl).trySetupPsiForFile(virtualFile, fileType.language, true, false)!!
    }

    protected fun getTestName(): String {
        return name.removePrefix("test")
    }

    protected open fun getTestDataPath(): String = dataPath

    override fun setUp() {
        super.setUp()
        definitions.forEach { definition ->
            val language = definition.fileNodeType.language
            com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(language, definition)
        }
    }
}
