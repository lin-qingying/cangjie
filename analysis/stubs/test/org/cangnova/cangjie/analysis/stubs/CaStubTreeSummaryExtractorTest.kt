/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.analysis.stubs

import com.intellij.util.io.StringRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*
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
            isConst = false,
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
