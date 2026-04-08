package org.cangnova.cangjie.analysis.stubs

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CaStubSnapshotAssemblerTest {
    @Test
    fun assembleSnapshotMergesPackageAndClassIndexes() {
        val packageFqName = FqName("sample.snapshot")
        val greeterClassId = ClassId(packageFqName, Name.identifier("Greeter"))
        val assembler = CaStubSnapshotAssembler()

        val snapshot = assembler.assemble(
            modificationCount = 17L,
            summaries = listOf(
                CaStubFileSummary(
                    fileKey = "file:///sample/alpha.cj",
                    stubKind = CangJieFileStubKindImpl.Facade(packageFqName, packageFqName),
                    packageFqName = packageFqName,
                    topLevelClassifierNames = setOf(Name.identifier("Greeter")),
                    topLevelCallableNames = setOf(Name.identifier("alpha")),
                    classMemberNames = mapOf(
                        greeterClassId to setOf(Name.identifier("member")),
                    ),
                ),
                CaStubFileSummary(
                    fileKey = "file:///sample/beta.cj",
                    stubKind = CangJieFileStubKindImpl.File(packageFqName),
                    packageFqName = packageFqName,
                    topLevelClassifierNames = setOf(Name.identifier("Alias")),
                    topLevelCallableNames = setOf(Name.identifier("beta")),
                    classMemberNames = mapOf(
                        greeterClassId to setOf(Name.identifier("nested")),
                    ),
                ),
            ),
        )

        assertEquals(17L, snapshot.modificationCount)
        assertEquals(listOf("Alias", "Greeter"), snapshot.packageClassifierNames.getValue(packageFqName).map(Name::asString).sorted())
        assertEquals(listOf("alpha", "beta"), snapshot.packageCallableNames.getValue(packageFqName).map(Name::asString).sorted())
        assertEquals(listOf("member", "nested"), snapshot.classMemberNames.getValue(greeterClassId).map(Name::asString).sorted())
    }
}
