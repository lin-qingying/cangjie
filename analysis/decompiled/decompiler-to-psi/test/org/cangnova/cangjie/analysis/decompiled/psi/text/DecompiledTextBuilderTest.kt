package org.cangnova.cangjie.analysis.decompiled.psi.text

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAnnotations
import org.cangnova.cangjie.psi.CjAbstractClassBody
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieStructStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieBasicTypeStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieMacroStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieMainFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieParameterStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyAccessorStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyStubImpl
import org.cangnova.cangjie.psi.stubs.impl.ModifierMaskUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DecompiledTextBuilderTest {
    @Test
    fun zeroArgCompiledNamedFunctionWithoutParameterListStillRendersParenthesesAndBody() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val functionStub = CangJieNamedFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.FUNCTION,
                    nameRef = StringRef.fromString("toString"),
                    isTopLevel = true,
                    fqName = samplePackage.child(Name.identifier("toString")),
                    hasBlockBody = true,
                    hasBody = true,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(functionStub)
            }

            assertTrue(rendered.contains("func toString() { /* compiled code */ }"), rendered)
        }
    }

    @Test
    fun zeroArgCompiledOperatorWithoutParameterListStillRendersParenthesesAndBody() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val functionStub = CangJieNamedFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.FUNCTION,
                    nameRef = StringRef.fromString("-"),
                    isTopLevel = false,
                    fqName = null,
                    hasBlockBody = true,
                    hasBody = true,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(
                    functionStub,
                    modifierMask = computeModifierMask(operator = true),
                )
            }

            assertTrue(rendered.contains("operator func -() { /* compiled code */ }"), rendered)
        }
    }

    @Test
    fun zeroArgCompiledMainAndMacroWithoutParameterListStillRenderParentheses() {
        withCoreEnvironment {
            val mainRendered = renderManualFileStub { fileStub ->
                val mainStub = CangJieMainFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.MAIN_FUNC,
                    nameRef = StringRef.fromString("main"),
                    fqName = samplePackage.child(Name.identifier("main")),
                    origin = null,
                )
                createEmptyHeader(mainStub)
            }
            assertTrue(mainRendered.contains("main() { /* compiled code */ }"), mainRendered)

            val macroRendered = renderManualFileStub { fileStub ->
                val macroStub = CangJieMacroStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.MACRO,
                    nameRef = StringRef.fromString("trace"),
                    isTopLevel = true,
                    fqName = samplePackage.child(Name.identifier("trace")),
                    hasBlockBody = true,
                    hasBody = true,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(macroStub)
            }
            assertTrue(macroRendered.contains("macro trace() { /* compiled code */ }"), macroRendered)
        }
    }

    @Test
    fun foreignCallableRemainsBodylessAndRendersForeignModifier() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val functionStub = CangJieNamedFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.FUNCTION,
                    nameRef = StringRef.fromString("ccall"),
                    isTopLevel = true,
                    fqName = samplePackage.child(Name.identifier("ccall")),
                    hasBlockBody = false,
                    hasBody = false,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(
                    functionStub,
                    modifierMask = computeModifierMask(foreign = true),
                )
            }

            assertTrue(rendered.contains("foreign func ccall()"), rendered)
            assertFalse(rendered.contains("foreign func ccall() { /* compiled code */ }"), rendered)
        }
    }

    @Test
    fun interfaceAbstractMemberRemainsBodyless() {
        withCoreEnvironment { environment ->
            val sourceFile = PsiFileFactory.getInstance(environment.project).createFileFromText(
                "sample.cj",
                CangJieFileType.INSTANCE,
                """
                package sample

                interface Box {
                    func toString(): Int64
                }
                """.trimIndent(),
            ) as CjFile
            val rendered = buildDecompiledText(sourceFile.calcStubTree().root as CangJieFileStubImpl)

            assertTrue(rendered.contains("func toString(): Int64"), rendered)
            assertFalse(rendered.contains("func toString(): Int64 { /* compiled code */ }"), rendered)
        }
    }

    @Test
    fun compiledPropertyWithoutAccessorStubsStillRendersGetterBody() {
        withCoreEnvironment {
            val rendered = renderManualStructStub { classBodyStub ->
                val propertyStub = CangJiePropertyStubImpl(
                    parent = classBodyStub,
                    name = StringRef.fromString("first"),
                    fqName = sampleStructFqName.child(Name.identifier("first")),
                )
                createEmptyHeader(propertyStub)
                createBasicTypeReference(propertyStub, "Int64")
            }

            assertTrue(rendered.contains("prop first"), rendered)
            assertTrue(rendered.contains("get() { /* compiled code */ }"), rendered)
        }
    }

    @Test
    fun compiledMutPropertyWithoutAccessorStubsStillRendersGetterAndSetterBodies() {
        withCoreEnvironment {
            val rendered = renderManualStructStub { classBodyStub ->
                val propertyStub = CangJiePropertyStubImpl(
                    parent = classBodyStub,
                    name = StringRef.fromString("state"),
                    fqName = sampleStructFqName.child(Name.identifier("state")),
                )
                createEmptyHeader(
                    propertyStub,
                    modifierMask = computeModifierMask(mut = true),
                )
                createBasicTypeReference(propertyStub, "Int64")
            }

            assertTrue(rendered.contains("mut prop state: Int64 {"), rendered)
            assertTrue(rendered.contains("get() { /* compiled code */ }"), rendered)
            assertTrue(rendered.contains("set(value) { /* compiled code */ }"), rendered)
            assertFalse(rendered.contains("set(value: "), rendered)
        }
    }

    @Test
    fun propertyAccessorRendersUntypedSetterParameter() {
        withCoreEnvironment {
            val rendered = renderManualStructStub { classBodyStub ->
                val propertyStub = CangJiePropertyStubImpl(
                    parent = classBodyStub,
                    name = StringRef.fromString("state"),
                    fqName = sampleStructFqName.child(Name.identifier("state")),
                )
                createEmptyHeader(
                    propertyStub,
                    modifierMask = computeModifierMask(mut = true),
                )
                createBasicTypeReference(propertyStub, "Int64")
                val propertyBodyStub = CangJiePlaceHolderStubImpl<org.cangnova.cangjie.psi.CjPropertyBody>(
                    propertyStub,
                    CjStubElementTypes.PROPERTY_BODY,
                )
                CangJiePropertyAccessorStubImpl(
                    parent = propertyBodyStub,
                    isGetter = true,
                    hasBody = true,
                    hasBlockBody = true,
                )
                val setterStub = CangJiePropertyAccessorStubImpl(
                    parent = propertyBodyStub,
                    isGetter = false,
                    hasBody = true,
                    hasBlockBody = true,
                )
                val parameterListStub = CangJiePlaceHolderStubImpl<org.cangnova.cangjie.psi.CjParameterList>(
                    setterStub,
                    CjStubElementTypes.VALUE_PARAMETER_LIST,
                )
                CangJieParameterStubImpl(
                    parent = parameterListStub,
                    fqName = null,
                    name = StringRef.fromString("newValue"),
                    isMutable = false,
                    hasLetOrVar = false,
                    hasDefaultValue = false,
                    isNamed = false,
                    functionTypeParameterName = null,
                )
            }

            assertTrue(rendered.contains("set(newValue) { /* compiled code */ }"), rendered)
            assertFalse(rendered.contains("set(newValue: "), rendered)
        }
    }

    private fun withCoreEnvironment(action: (CangJieCoreEnvironment) -> Unit) {
        val disposable = Disposer.newDisposable("DecompiledTextBuilderTest")
        try {
            val environment = CangJieCoreEnvironment.createForTests(disposable)
            action(environment)
        } finally {
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction {
                    Disposer.dispose(disposable)
                }
            } else {
                Disposer.dispose(disposable)
            }
        }
    }

    private fun renderManualFileStub(build: (CangJieFileStubImpl) -> Unit): String {
        val fileStub = CangJieFileStubImpl.forFile(samplePackage)
        build(fileStub)
        return buildDecompiledText(fileStub)
    }

    private fun renderManualStructStub(build: (CangJiePlaceHolderStubImpl<CjAbstractClassBody>) -> Unit): String {
        return renderManualFileStub { fileStub ->
            val structStub = CangJieStructStubImpl(
                type = CjStubElementTypes.STRUCT,
                parent = fileStub,
                qualifiedName = StringRef.fromString(sampleStructFqName.asString()),
                classId = null,
                name = StringRef.fromString(sampleStructName.asString()),
                superNames = emptyArray(),
            )
            createEmptyHeader(structStub)
            val classBodyStub = CangJiePlaceHolderStubImpl<CjAbstractClassBody>(
                structStub,
                CjStubElementTypes.CLASS_BODY,
            )
            build(classBodyStub)
        }
    }

    private fun createEmptyHeader(parent: StubElement<*>, modifierMask: Long = 0L) {
        CangJiePlaceHolderStubImpl<CjAnnotations>(parent, CjStubElementTypes.ANNOTATIONS)
        CangJieModifierListStubImpl(parent, modifierMask, CjStubElementTypes.MODIFIER_LIST)
    }

    private fun createBasicTypeReference(parent: StubElement<*>, typeName: String) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<org.cangnova.cangjie.psi.CjTypeReference>(
            parent,
            CjStubElementTypes.TYPE_REFERENCE,
        )
        CangJieBasicTypeStubImpl(typeReferenceStub, typeName)
    }

    private fun computeModifierMask(
        operator: Boolean = false,
        foreign: Boolean = false,
        mut: Boolean = false,
    ): Long {
        return ModifierMaskUtils.computeMask(
            hasModifier = { modifier ->
                when (modifier) {
                    CjTokens.OPERATOR_KEYWORD -> operator
                    CjTokens.MUT_KEYWORD -> mut
                    else -> false
                }
            },
            hasAdditionalModifier = { keyword ->
                keyword == CjTokens.FOREIGN_KEYWORD && foreign
            },
        )
    }

    private companion object {
        val samplePackage: FqName = FqName("sample")
        val sampleStructName: Name = Name.identifier("Box")
        val sampleStructFqName: FqName = samplePackage.child(sampleStructName)
    }
}
