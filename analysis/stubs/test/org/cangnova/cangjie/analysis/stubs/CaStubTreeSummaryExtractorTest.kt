package org.cangnova.cangjie.analysis.stubs

import com.intellij.util.io.StringRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieBindingPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieClassStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieExtendStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieTuplePatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieTypeAliasStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieVarOrEnumPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieVariableStubImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CaStubTreeSummaryExtractorTest {
    @Test
    fun extractSummaryFromManualStubTree() {
        val packageFqName = FqName("sample.manual")
        val fileStub = CangJieFileStubImpl.forFileFacadeStub(packageFqName)

        val greeterClassId = ClassId(packageFqName, Name.identifier("Greeter"))
        val greeterStub = CangJieClassStubImpl(
            type = CjStubElementTypes.CLASS,
            parent = fileStub,
            qualifiedName = StringRef.fromString(greeterClassId.asString()),
            classId = greeterClassId,
            name = StringRef.fromString("Greeter"),
            superNames = emptyArray(),
        )
        CangJieNamedFunctionStubImpl(
            parent = greeterStub,
            element = CjStubElementTypes.FUNCTION,
            nameRef = StringRef.fromString("member"),
            isTopLevel = false,
            fqName = FqName("${greeterClassId.asString()}.member"),
            hasBlockBody = true,
            hasBody = true,
            hasTypeParameterListBeforeFunctionName = false,
            origin = null,
        )
        CangJiePropertyStubImpl(
            parent = greeterStub,
            name = StringRef.fromString("stored"),
            fqName = FqName("${greeterClassId.asString()}.stored"),
        )
        CangJieClassStubImpl(
            type = CjStubElementTypes.CLASS,
            parent = greeterStub,
            qualifiedName = StringRef.fromString("${greeterClassId.asString()}.Nested"),
            classId = null,
            name = StringRef.fromString("Nested"),
            superNames = emptyArray(),
        )

        CangJieTypeAliasStubImpl(
            parent = fileStub,
            name = StringRef.fromString("Alias"),
            qualifiedName = StringRef.fromString("sample.manual.Alias"),
            classId = ClassId(packageFqName, Name.identifier("Alias")),
        )
        CangJieNamedFunctionStubImpl(
            parent = fileStub,
            element = CjStubElementTypes.FUNCTION,
            nameRef = StringRef.fromString("topLevel"),
            isTopLevel = true,
            fqName = FqName("sample.manual.topLevel"),
            hasBlockBody = true,
            hasBody = true,
            hasTypeParameterListBeforeFunctionName = false,
            origin = null,
        )
        CangJiePropertyStubImpl(
            parent = fileStub,
            name = StringRef.fromString("count"),
            fqName = FqName("sample.manual.count"),
        )

        val topLevelVariable = CangJieVariableStubImpl(
            parent = fileStub,
            patternKind = PatternKind.TUPLE,
            isVar = true,
            isTopLevel = true,
            hasInitializer = true,
            hasReturnTypeRef = false,
            origin = null,
        )
        val tuplePattern = CangJieTuplePatternStubImpl(topLevelVariable)
        CangJieBindingPatternStubImpl(
            parent = tuplePattern,
            nameRef = StringRef.fromString("left"),
            fqName = FqName("sample.manual.left"),
        )
        CangJieVarOrEnumPatternStubImpl(
            parent = tuplePattern,
            nameRef = StringRef.fromString("candidate"),
        )

        CangJieExtendStubImpl(
            type = CjStubElementTypes.EXTEND,
            parent = fileStub,
            qualifiedName = StringRef.fromString("sample.manual.Int64Extend"),
            classId = null,
            name = StringRef.fromString("Int64Extend"),
            extendIdRef = StringRef.fromString("sample.manual#Int64"),
            superNames = emptyArray(),
            receiverTypeName = "Int64",
        )

        val summary = CaStubTreeSummaryExtractor().extract(
            fileKey = "manual://sample.manual/package.cjo",
            fallbackPackageFqName = packageFqName,
            fileStub = fileStub,
        )

        assertEquals(listOf("Alias", "Greeter"), summary.topLevelClassifierNames.map(Name::asString).sorted())
        assertEquals(listOf("candidate", "count", "left", "topLevel"), summary.topLevelCallableNames.map(Name::asString).sorted())
        assertEquals(
            listOf("Nested", "member", "stored"),
            summary.classMemberNames.getValue(greeterClassId).map(Name::asString).sorted(),
        )
    }
}
