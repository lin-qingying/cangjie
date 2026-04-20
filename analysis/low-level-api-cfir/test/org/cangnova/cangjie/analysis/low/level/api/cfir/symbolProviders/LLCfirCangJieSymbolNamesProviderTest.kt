package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import kotlin.test.Test
import kotlin.test.assertEquals

class LLCfirCangJieSymbolNamesProviderTest {
    @Test
    fun `does not filter stdlib package names`() {
        val declarationProvider = FakeDeclarationProvider(
            packageNames = linkedSetOf("std.core", "stdx.effect", "demo.pkg"),
            classifierPackages = linkedSetOf("std.core", "demo.pkg"),
            callablePackages = linkedSetOf("stdx.effect", "demo.pkg"),
            classifiersByPackage = mapOf(
                FqName("std.core") to linkedSetOf(Name.identifier("String"), Name.identifier("Any")),
            ),
            callablesByPackage = mapOf(
                FqName("stdx.effect") to linkedSetOf(Name.identifier("spawn")),
            ),
        )

        val provider = LLCfirCangJieSymbolNamesProvider(declarationProvider)

        assertEquals(linkedSetOf("std.core", "stdx.effect", "demo.pkg"), provider.getPackageNames())
        assertEquals(linkedSetOf("std.core", "demo.pkg"), provider.getPackageNamesWithTopLevelClassifiers())
        assertEquals(linkedSetOf("stdx.effect", "demo.pkg"), provider.getPackageNamesWithTopLevelCallables())
        assertEquals(
            linkedSetOf(Name.identifier("String"), Name.identifier("Any")),
            provider.getTopLevelClassifierNamesInPackage(FqName("std.core"))
        )
        assertEquals(
            linkedSetOf(Name.identifier("spawn")),
            provider.getTopLevelCallableNamesInPackage(FqName("stdx.effect"))
        )
    }
}

private class FakeDeclarationProvider(
    private val packageNames: Set<String>? = null,
    private val classifierPackages: Set<String>? = null,
    private val callablePackages: Set<String>? = null,
    private val classifiersByPackage: Map<FqName, Set<Name>> = emptyMap(),
    private val callablesByPackage: Map<FqName, Set<Name>> = emptyMap(),
) : CangJieDeclarationProvider {
    override fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration? = null

    override fun getAllClassesByClassId(classId: ClassId): Collection<CjTypeStatement> = emptyList()

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<CjTypeAlias> = emptyList()

    override fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> =
        classifiersByPackage[packageFqName].orEmpty()

    override fun getTopLevelProperties(callableId: CallableId): Collection<CjProperty> = emptyList()

    override fun getTopLevelFunctions(callableId: CallableId): Collection<CjNamedFunction> = emptyList()

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<CjFile> = emptyList()

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
        callablesByPackage[packageFqName].orEmpty()

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<CjFile> = emptyList()

    override fun findFilesForFacade(facadeFqName: FqName): Collection<CjFile> = emptyList()

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<CjFile> = emptyList()

    override fun computePackageNames(): Set<String>? = packageNames

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = true

    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? = classifierPackages

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = true

    override fun computePackageNamesWithTopLevelCallables(): Set<String>? = callablePackages
}
