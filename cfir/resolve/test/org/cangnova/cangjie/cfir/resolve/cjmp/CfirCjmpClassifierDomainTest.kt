@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildClassCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildFileCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirectiveCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeParameter
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirFileImpl
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.session.CfirCjmpClassifierDomain
import org.cangnova.cangjie.cfir.session.CfirCjmpPreparedGroup
import org.cangnova.cangjie.cfir.session.CfirCjmpStructureSnapshot
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.mpp.CjmpContainerKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMergeContainer
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMergeMember
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * R1 原结构选择域的纯模型回归：真实模块、绑定声明及原 evaluator 共同提供 nominal 证明。
 * Source 模型持真实 PSI source，Library 模型持原 CJO header；不模拟 SOURCE 阶段推进或发布事务。
 */
class CfirCjmpClassifierDomainTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun selectedNominalProofRetainsTheActualStructuralTargetAndAuthority() {
        val fixture = Fixture()
        val common = fixture.common()
        val earlier = fixture.specific()
        val selected = fixture.specific()
        val structure = group(fixture.actualSession, listOf(common, earlier, selected)).evaluateStructure()
        val seed = structure.merge.initialBindings.single()

        assertSame(common.symbol, seed.common)
        assertSame(selected.symbol, seed.specific)
        assertNotSame(earlier.symbol, seed.specific)
        val proof = checkNotNull(structure.classifierRelationFor(common.symbol, fixture.actualSession))
        assertSame(common.symbol, proof.common)
        assertSame(seed.specific, proof.specific)
        assertSame(structure, proof.authority)
        assertEquals(CfirResolvePhase.TYPES, common.resolvePhase)
        assertEquals(CfirResolvePhase.TYPES, selected.resolvePhase)
        assertSame(fixture.actualSession, structure.classifierDomain.actualSession)
        assertSame(fixture.actualSession.moduleData, structure.classifierDomain.actualModule)
        assertEquals(setOf(fixture.commonSession.moduleData), structure.classifierDomain.refinementModules)
        assertTrue(seed.typeParameterMapping.isEmpty())
    }

    @Test
    fun equallyNamedClassifierFromAnotherModuleCannotReplaceTheCapturedCommon() {
        val fixture = Fixture()
        val common = fixture.common()
        val specific = fixture.specific()
        val otherSession = CallResolutionTestFixtures.newTestSession()
        val unrelated = nominal(otherSession, common = true)
        val structure = group(fixture.actualSession, listOf(common, specific)).evaluateStructure()

        assertEquals(common.symbol.classId, unrelated.symbol.classId)
        assertEquals(common.moduleData.name, unrelated.moduleData.name)
        assertNotSame(common.moduleData, unrelated.moduleData)
        assertNotSame(common.symbol, unrelated.symbol)
        assertThrows(IllegalStateException::class.java) {
            structure.classifierRelationFor(unrelated.symbol, fixture.actualSession)
        }
        assertSame(specific.symbol, checkNotNull(structure.classifierRelationFor(common.symbol, fixture.actualSession)).specific)
    }

    @Test
    fun commonFromEquallyNamedOrdinaryDependencyIsNotRefinementAuthority() {
        val commonSession = CallResolutionTestFixtures.newTestSession()
        val ordinarySession = CallResolutionTestFixtures.newTestSession()
        val actualSession = CallResolutionTestFixtures.newTestSession(
            dependencies = listOf(commonSession.moduleData, ordinarySession.moduleData),
            refinementDependencies = listOf(commonSession.moduleData),
        )
        val wrongCommon = nominal(ordinarySession, common = true)
        val specific = nominal(actualSession, specific = true)
        val structure = group(actualSession, listOf(wrongCommon, specific)).evaluateStructure()

        assertEquals(commonSession.moduleData.name, ordinarySession.moduleData.name)
        assertNotSame(commonSession.moduleData, ordinarySession.moduleData)
        assertSame(specific.symbol, structure.merge.initialBindings.single().specific)
        assertThrows(IllegalStateException::class.java) {
            structure.classifierRelationFor(wrongCommon.symbol, actualSession)
        }
    }

    @Test
    fun commonInActualModuleIsRejectedEvenWithARealNominalSeed() {
        val fixture = Fixture()
        val common = nominal(fixture.actualSession, common = true)
        val specific = fixture.specific()
        val structure = group(fixture.actualSession, listOf(common, specific)).evaluateStructure()

        assertSame(structure.classifierDomain.actualModule, common.moduleData)
        assertTrue(common.moduleData !in structure.classifierDomain.refinementModules)
        assertSame(specific.symbol, structure.merge.initialBindings.single().specific)
        assertThrows(IllegalStateException::class.java) {
            structure.classifierRelationFor(common.symbol, fixture.actualSession)
        }
    }

    @Test
    fun selectedSpecificMustBelongToTheActualModuleObject() {
        val fixture = Fixture()
        val common = fixture.common()
        val otherSession = CallResolutionTestFixtures.newTestSession()
        val wrongSpecific = nominal(otherSession, specific = true)
        val structure = group(fixture.actualSession, listOf(common, wrongSpecific)).evaluateStructure()

        assertEquals(fixture.actualSession.moduleData.name, wrongSpecific.moduleData.name)
        assertNotSame(fixture.actualSession.moduleData, wrongSpecific.moduleData)
        assertSame(wrongSpecific.symbol, structure.merge.initialBindings.single().specific)
        assertThrows(IllegalStateException::class.java) {
            structure.classifierRelationFor(common.symbol, fixture.actualSession)
        }
    }

    @Test
    fun anotherSourceSessionCannotConsumeTheProofEvenWithTheSameRefinementObjects() {
        val fixture = Fixture()
        val common = fixture.common()
        val specific = fixture.specific()
        val structure = group(fixture.actualSession, listOf(common, specific)).evaluateStructure()
        val anotherActual = CallResolutionTestFixtures.newTestSession(
            dependencies = listOf(fixture.commonSession.moduleData),
            refinementDependencies = listOf(fixture.commonSession.moduleData),
        )

        assertEquals(fixture.actualSession.kind, anotherActual.kind)
        assertEquals(fixture.actualSession.moduleData.allRefinementDependencies, anotherActual.moduleData.allRefinementDependencies)
        assertNotSame(fixture.actualSession, anotherActual)
        assertNotSame(fixture.actualSession.moduleData, anotherActual.moduleData)
        assertThrows(IllegalStateException::class.java) {
            structure.classifierRelationFor(common.symbol, anotherActual)
        }
        assertSame(specific.symbol, checkNotNull(structure.classifierRelationFor(common.symbol, fixture.actualSession)).specific)
    }

    @Test
    fun completedCommonWithoutASelectedSeedHasNoClassifierRelation() {
        val fixture = Fixture()
        val common = fixture.common()
        val structure = group(fixture.actualSession, listOf(common)).evaluateStructure()

        assertEquals(CfirResolvePhase.TYPES, common.resolvePhase)
        assertTrue(structure.merge.initialBindings.isEmpty())
        assertNull(structure.classifierRelationFor(common.symbol, fixture.actualSession))
    }

    @Test
    fun genericAndNonGenericNominalsDoNotManufactureASeed() {
        for (commonArity in listOf(0, 1)) {
            val fixture = Fixture()
            val common = fixture.common(arity = commonArity)
            val specific = fixture.specific(arity = 1 - commonArity)
            val structure = group(fixture.actualSession, listOf(common, specific)).evaluateStructure()

            assertTrue(structure.merge.initialBindings.isEmpty())
            assertNull(structure.classifierRelationFor(common.symbol, fixture.actualSession))
        }
    }

    @Test
    fun unequalNonZeroAritiesKeepTheNominalSeedButHaveNoParameterMapping() {
        for ((commonArity, specificArity) in listOf(1 to 2, 2 to 1)) {
            val fixture = Fixture()
            val common = fixture.common(arity = commonArity)
            val specific = fixture.specific(arity = specificArity)
            val structure = group(fixture.actualSession, listOf(common, specific)).evaluateStructure()
            val seed = structure.merge.initialBindings.single()

            assertEquals(commonArity, common.typeParameters.size)
            assertEquals(specificArity, specific.typeParameters.size)
            assertSame(common.symbol, seed.common)
            assertSame(specific.symbol, seed.specific)
            assertTrue(seed.typeParameterMapping.isEmpty())
            assertTrue(structure.merge.merges.single().typeParameterMapping.isEmpty())
            assertSame(specific.symbol, checkNotNull(structure.classifierRelationFor(common.symbol, fixture.actualSession)).specific)
        }
    }

    @Test
    fun nestedClassifierCannotBorrowTheTopLevelSelectionEvenIfRawMergeMadeAnEdge() {
        for (appendToDiscoveryOrder in listOf(false, true)) {
            val fixture = Fixture()
            // 仓颉 ClassId 不表示嵌套类型；反例仅把另一个平面身份声明错误放入成员槽。
            val nested = nominal(fixture.commonSession, common = true, classId = CLASS_ID)
            val common = nominal(fixture.commonSession, common = true, members = listOf(nested))
            val specific = fixture.specific()
            val originalTopLevel = listOf(common, specific)
            val files = sourceFilesFor(originalTopLevel)
            val discoveryOrder = originalTopLevel.map { it.symbol } +
                    if (appendToDiscoveryOrder) listOf(nested.symbol) else emptyList()
            val structure = group(
                fixture.actualSession,
                originalTopLevel,
                // firstByName 先保存实际 specific，才能让随后两个 common 产生本反例要求的原始边。
                nominals = listOf(specific, nested, common),
                sourceFiles = files,
                topLevelOrder = discoveryOrder,
            ).evaluateStructure()

            assertSame(nested, common.declarations.single())
            assertSame(common, structure.physicalOwnerFor(nested))
            assertTrue(files.all { file -> file.declarations.none { it === nested } })
            assertEquals(appendToDiscoveryOrder, structure.topLevelDeclarationsForMatching().any { it === nested })
            assertTrue(structure.merge.initialBindings.any { it.common === nested.symbol && it.specific === specific.symbol })
            assertSame(specific.symbol, checkNotNull(structure.classifierRelationFor(common.symbol, fixture.actualSession)).specific)
            val failure = assertThrows(IllegalStateException::class.java) {
                structure.classifierRelationFor(nested.symbol, fixture.actualSession)
            }
            assertEquals("Classifier has no original physical top-level root proof", failure.message)
        }
    }

    @Test
    fun unknownPhysicalRootCannotUseDiscoveryOrderAsSourceOrCjoProof() {
        for (unknownCommon in listOf(true, false)) {
            val fixture = Fixture()
            val common = fixture.common()
            val specific = fixture.specific()
            val unknown = if (unknownCommon) common else specific
            val knownRoots = if (unknownCommon) listOf(specific) else listOf(common)
            val files = sourceFilesFor(knownRoots)
            val structure = group(
                fixture.actualSession,
                listOf(common, specific),
                sourceFiles = files,
            ).evaluateStructure()

            assertNull(unknown.cjoDeclarationOrigin)
            assertTrue(files.all { file -> file.declarations.none { it === unknown } })
            assertTrue(structure.topLevelDeclarationsForMatching().any { it === unknown })
            assertSame(specific.symbol, structure.merge.initialBindings.single().specific)
            val failure = assertThrows(IllegalStateException::class.java) {
                structure.classifierRelationFor(common.symbol, fixture.actualSession)
            }
            assertEquals("Classifier has no original physical top-level root proof", failure.message)
        }
        val fixture = Fixture()
        val common = fixture.common()
        val noSeed = group(fixture.actualSession, listOf(common), sourceFiles = emptyList()).evaluateStructure()
        assertTrue(noSeed.merge.initialBindings.isEmpty())
        assertNull(common.cjoDeclarationOrigin)
        val failure = assertThrows(IllegalStateException::class.java) {
            noSeed.classifierRelationFor(common.symbol, fixture.actualSession)
        }
        assertEquals("Classifier has no original physical top-level root proof", failure.message)
    }

    @Test
    fun cjoNonTopLevelOriginCannotBeUpgradedAndLoadedRootRetainsHeaderIdentity() {
        for (isTopLevel in listOf(false, true)) {
            val commonModule = libraryModule()
            val actualSession = CallResolutionTestFixtures.newTestSession(
                dependencies = listOf(commonModule),
                refinementDependencies = listOf(commonModule),
            )
            val common = libraryNominal(commonModule, common = true)
            val specific = nominal(actualSession, specific = true)
            // 这里只构造 wire header 的模型产品，不声称已经反序列化或重放任何 SOURCE。
            val origin = CfirCjoDeclarationOrigin(
                packagePath = "classifier-domain/common.cjo",
                declarationIndex = 0,
                isTopLevel = isTopLevel,
            )
            common.cjoDeclarationOrigin = origin
            val files = sourceFilesFor(listOf(specific))
            val structure = group(actualSession, listOf(common, specific), sourceFiles = files).evaluateStructure()

            assertEquals(CfirSession.Kind.Library, commonModule.session.kind)
            assertSame(CfirDeclarationOrigin.Library, common.origin)
            assertSame(commonModule, structure.classifierDomain.refinementModules.single())
            assertSame(origin, common.cjoDeclarationOrigin)
            assertTrue(files.all { file -> file.declarations.none { it === common } })
            assertTrue(structure.topLevelDeclarationsForMatching().any { it === common })
            assertSame(specific.symbol, structure.merge.initialBindings.single().specific)
            if (isTopLevel) {
                assertSame(specific.symbol, checkNotNull(structure.classifierRelationFor(common.symbol, actualSession)).specific)
                val replacement = origin.copy()
                assertEquals(origin, replacement)
                assertNotSame(origin, replacement)
                common.cjoDeclarationOrigin = replacement
                val failure = assertThrows(IllegalStateException::class.java) {
                    structure.classifierRelationFor(common.symbol, actualSession)
                }
                assertEquals("Original loaded classifier root header changed after structural preparation", failure.message)
            } else {
                val failure = assertThrows(IllegalStateException::class.java) {
                    structure.classifierRelationFor(common.symbol, actualSession)
                }
                assertEquals("Classifier has no original physical top-level root proof", failure.message)
                assertSame(origin, common.cjoDeclarationOrigin)
                assertEquals(false, origin.isTopLevel)
            }
        }
    }

    @Test
    fun originalSourceFileSlotRejectsReplacementAndReorderingOnEitherSide() {
        for (changeCommon in listOf(true, false)) {
            for (moveOriginal in listOf(false, true)) {
                val fixture = Fixture()
                val common = fixture.common()
                val specific = fixture.specific()
                val target = if (changeCommon) common else specific
                val targetSession = if (changeCommon) fixture.commonSession else fixture.actualSession
                val spare = field(targetSession)
                val files = sourceFilesFor(listOf(common, specific, spare))
                val file = files.single { it.moduleData === target.moduleData }
                val structure = group(
                    fixture.actualSession,
                    listOf(common, specific, spare),
                    sourceFiles = files,
                ).evaluateStructure()

                assertSame(target, file.declarations[0])
                assertSame(spare, file.declarations[1])
                assertSame(specific.symbol, checkNotNull(structure.classifierRelationFor(common.symbol, fixture.actualSession)).specific)
                val slots = (file as CfirFileImpl).declarations
                if (moveOriginal) {
                    slots.removeAt(0)
                    slots.add(target)
                    assertSame(target, slots[1])
                } else {
                    val replacement = nominal(targetSession, common = changeCommon, specific = !changeCommon)
                    assertEquals(target.symbol.classId, replacement.symbol.classId)
                    assertSame(target.moduleData, replacement.moduleData)
                    assertNotSame(target, replacement)
                    slots[0] = replacement
                }
                assertSame(target, target.symbol.cfir)
                val failure = assertThrows(IllegalStateException::class.java) {
                    structure.classifierRelationFor(common.symbol, fixture.actualSession)
                }
                assertEquals("Original classifier file root changed after structural preparation", failure.message)
            }
        }
    }

    @Test
    fun capturePreservesIncompleteHeadersAndQueryRejectsEitherSideBelowTypes() {
        for (commonIncomplete in listOf(true, false)) {
            val fixture = Fixture()
            val common = fixture.common(phase = if (commonIncomplete) CfirResolvePhase.SUPER_TYPES else CfirResolvePhase.TYPES)
            val specific = fixture.specific(phase = if (commonIncomplete) CfirResolvePhase.TYPES else CfirResolvePhase.SUPER_TYPES)
            val prepared = group(fixture.actualSession, listOf(common, specific))
            val incomplete = if (commonIncomplete) common else specific

            assertEquals(CfirResolvePhase.SUPER_TYPES, incomplete.resolvePhase)
            val structure = prepared.evaluateStructure()
            assertSame(specific.symbol, structure.merge.initialBindings.single().specific)
            assertThrows(IllegalStateException::class.java) {
                structure.classifierRelationFor(common.symbol, fixture.actualSession)
            }
            assertEquals(CfirResolvePhase.SUPER_TYPES, incomplete.resolvePhase)
        }
        val fixture = Fixture()
        val rawCommon = fixture.common(phase = CfirResolvePhase.RAW_CFIR)
        val noSeed = group(fixture.actualSession, listOf(rawCommon)).evaluateStructure()
        assertTrue(noSeed.merge.initialBindings.isEmpty())
        assertThrows(IllegalStateException::class.java) {
            noSeed.classifierRelationFor(rawCommon.symbol, fixture.actualSession)
        }
    }

    @Test
    fun reboundClassifierDeclarationInvalidatesCaptureAndExistingProof() {
        for (rebindCommon in listOf(true, false)) {
            val fixture = Fixture()
            val common = fixture.common()
            val specific = fixture.specific()
            val structure = group(fixture.actualSession, listOf(common, specific)).evaluateStructure()
            val original = if (rebindCommon) common else specific
            val replacement = nominal(
                if (rebindCommon) fixture.commonSession else fixture.actualSession,
                common = rebindCommon,
                specific = !rebindCommon,
            )
            original.symbol.bind(replacement)

            assertNotSame(original, original.symbol.cfir)
            assertThrows(IllegalStateException::class.java) {
                structure.classifierRelationFor(common.symbol, fixture.actualSession)
            }
            assertThrows(IllegalStateException::class.java) {
                CfirCjmpClassifierDomain.capture(fixture.actualSession, listOf(original), sourceFilesFor(listOf(original)))
            }
        }
    }

    @Test
    fun roleChangesAreRejectedBeforeReturningEitherAProofOrNoSeed() {
        for (selected in listOf(true, false)) {
            for (changeCommon in if (selected) listOf(true, false) else listOf(true)) {
                val fixture = Fixture()
                val common = fixture.common()
                val specific = fixture.specific()
                val structure = group(fixture.actualSession, if (selected) listOf(common, specific) else listOf(common)).evaluateStructure()
                assertOriginalSelection(structure, fixture, common, specific, selected)
                val target = if (changeCommon) common else specific
                val status = target.status as CfirDeclarationStatusImpl
                if (changeCommon) status.isCommon = false else status.isSpecific = false

                assertThrows(IllegalStateException::class.java) {
                    structure.classifierRelationFor(common.symbol, fixture.actualSession)
                }
            }
        }
    }

    @Test
    fun sameNameAndArityDoNotHideChangedTypeParameterIdentityEvenWithoutASeed() {
        for (selected in listOf(true, false)) {
            for (changeCommon in if (selected) listOf(true, false) else listOf(true)) {
                val fixture = Fixture()
                val common = fixture.common(arity = 1)
                val specific = fixture.specific(arity = 1)
                val structure = group(fixture.actualSession, if (selected) listOf(common, specific) else listOf(common)).evaluateStructure()
                assertOriginalSelection(structure, fixture, common, specific, selected)
                val target = if (changeCommon) common else specific
                val original = target.typeParameters.single()
                val replacement = parameter(target.symbol, target.moduleData, original.name, original.origin, original.source)
                (target as CfirClassImpl).typeParameters[0] = replacement

                assertEquals(original.name, replacement.name)
                assertEquals(1, target.typeParameters.size)
                assertNotSame(original.symbol, replacement.symbol)
                assertSame(target.symbol, replacement.containingDeclarationSymbol)
                assertThrows(IllegalStateException::class.java) {
                    structure.classifierRelationFor(common.symbol, fixture.actualSession)
                }
            }
        }
    }

    @Test
    fun realClassCannotBePresentedAsStructEvenWhenRawMergeHasASeed() {
        for (selected in listOf(true, false)) {
            val fixture = Fixture()
            val common = fixture.common()
            val specific = fixture.specific()
            val originalTopLevel = if (selected) listOf(common, specific) else listOf(common)
            val files = sourceFilesFor(originalTopLevel)
            val original = group(fixture.actualSession, originalTopLevel, sourceFiles = files).evaluateStructure()
            assertOriginalSelection(original, fixture, common, specific, selected)

            // 两个原声明仍是真实 class；只把显式结构输入错误地描述成 STRUCT，不改原 seed 算法。
            val mislabeled = originalTopLevel.map { container(it).copy(kind = CjmpContainerKind.STRUCT) }
            val structure = group(
                fixture.actualSession,
                originalTopLevel,
                sourceFiles = files,
                containers = mislabeled,
            ).evaluateStructure()

            assertSame(common, common.symbol.cfir)
            assertSame(common, files.single { it.moduleData === common.moduleData }.declarations.single())
            if (selected) {
                val seed = structure.merge.initialBindings.single()
                assertSame(common.symbol, seed.common)
                assertSame(specific.symbol, seed.specific)
            } else {
                assertTrue(structure.merge.initialBindings.isEmpty())
            }
            val failure = assertThrows(IllegalStateException::class.java) {
                structure.classifierRelationFor(common.symbol, fixture.actualSession)
            }
            assertEquals("Common container kind does not describe its real classifier", failure.message)
        }
    }

    @Test
    fun libraryWithoutCjoOriginCannotUseAnExistingFileSlot() {
        val commonModule = libraryModule()
        val actualSession = CallResolutionTestFixtures.newTestSession(
            dependencies = listOf(commonModule),
            refinementDependencies = listOf(commonModule),
        )
        val common = libraryNominal(commonModule, common = true)
        val specific = nominal(actualSession, specific = true)
        // 明确提供非 Source 的真实文件槽作负例；它不能替代该 Library 声明缺失的 loaded header。
        val libraryFile = ExtendTestFixtures.newFile(commonModule, PACKAGE, listOf(common))
        val files = listOf(libraryFile) + sourceFilesFor(listOf(specific))
        val structure = group(actualSession, listOf(common, specific), sourceFiles = files).evaluateStructure()

        assertSame(CfirDeclarationOrigin.Library, common.origin)
        assertNull(common.cjoDeclarationOrigin)
        assertSame(common, libraryFile.declarations.single())
        assertSame(common.moduleData, libraryFile.moduleData)
        assertSame(specific.symbol, structure.merge.initialBindings.single().specific)
        val failure = assertThrows(IllegalStateException::class.java) {
            structure.classifierRelationFor(common.symbol, actualSession)
        }
        assertEquals("Classifier has no original physical top-level root proof", failure.message)
    }

    @Test
    fun sourceWithoutAFileSlotCannotUseEvenATopLevelCjoOrigin() {
        val fixture = Fixture()
        val common = fixture.common()
        val specific = fixture.specific()
        common.cjoDeclarationOrigin = CfirCjoDeclarationOrigin(
            packagePath = "classifier-domain/source-must-use-file.cjo",
            declarationIndex = 0,
            isTopLevel = true,
        )
        val files = sourceFilesFor(listOf(specific))
        val structure = group(fixture.actualSession, listOf(common, specific), sourceFiles = files).evaluateStructure()

        assertSame(CfirDeclarationOrigin.Source, common.origin)
        assertTrue(common.source is CjPsiSourceElement)
        assertSame(specific.symbol, structure.merge.initialBindings.single().specific)
        val failure = assertThrows(IllegalStateException::class.java) {
            structure.classifierRelationFor(common.symbol, fixture.actualSession)
        }
        assertEquals("Classifier has no original physical top-level root proof", failure.message)
        val withOriginalFile = group(
            fixture.actualSession, listOf(common, specific),
            sourceFiles = sourceFilesFor(listOf(common, specific)),
        ).evaluateStructure()
        assertSame(specific.symbol, checkNotNull(withOriginalFile.classifierRelationFor(common.symbol, fixture.actualSession)).specific)
    }

    @Test
    fun rawNameMergeCannotAuthorizeACommonOrSpecificFromAnotherPackage() {
        for (outsideCommon in listOf(true, false)) {
            val fixture = Fixture()
            val foreignId = ClassId.topLevel(FqName("other_classifier_domain.Box"))
            val common = nominal(fixture.commonSession, common = true, classId = if (outsideCommon) foreignId else CLASS_ID)
            val specific = nominal(fixture.actualSession, specific = true, classId = if (outsideCommon) CLASS_ID else foreignId)
            val files = sourceFilesFor(listOf(common, specific))
            val structure = group(fixture.actualSession, listOf(common, specific), sourceFiles = files).evaluateStructure()
            val outside = if (outsideCommon) common else specific
            val file = files.single { it.declarations.any { root -> root === outside } }

            assertEquals(common.name, specific.name)
            assertSame(common, common.symbol.cfir)
            assertSame(specific, specific.symbol.cfir)
            assertSame(fixture.commonSession.moduleData, common.moduleData)
            assertSame(fixture.actualSession.moduleData, specific.moduleData)
            assertSame(outside.moduleData, file.moduleData)
            assertEquals(foreignId.packageFqName, ((file.source as CjPsiSourceElement).psi as CjFile).packageFqName)
            assertEquals(foreignId.packageFqName, file.packageDirective.packageFqName)
            assertEquals(PACKAGE, structure.packageFqName)
            val seed = structure.merge.initialBindings.single()
            assertSame(common.symbol, seed.common)
            assertSame(specific.symbol, seed.specific)
            val failure = assertThrows(IllegalStateException::class.java) {
                structure.classifierRelationFor(common.symbol, fixture.actualSession)
            }
            assertEquals(
                if (outsideCommon) "Common classifier is outside the original package domain"
                else "Specific classifier is outside the original package domain",
                failure.message,
            )
        }
    }

    @Test
    fun sourceFilePackageChangeAfterCaptureInvalidatesEitherRoot() {
        for (changeCommon in listOf(true, false)) {
            val fixture = Fixture()
            val common = fixture.common()
            val specific = fixture.specific()
            val files = sourceFilesFor(listOf(common, specific))
            val target = if (changeCommon) common else specific
            val file = files.single { it.moduleData === target.moduleData }
            val structure = group(fixture.actualSession, listOf(common, specific), sourceFiles = files).evaluateStructure()

            assertEquals(target.symbol.classId.packageFqName, file.packageDirective.packageFqName)
            assertSame(specific.symbol, checkNotNull(structure.classifierRelationFor(common.symbol, fixture.actualSession)).specific)
            val changedPackage = FqName("changed_classifier_file_package")
            val originalHeader = file.packageDirective
            // 只替换原文件的真实 package header；声明绑定、模块和原 root 槽均不变。
            (file as CfirFileImpl).packageDirective = buildPackageDirectiveCopy(originalHeader) {
                packageFqName = changedPackage
            }

            assertSame(file, file.symbol.cfir)
            assertSame(target, target.symbol.cfir)
            assertSame(target, file.declarations.single())
            assertSame(target.moduleData, file.moduleData)
            assertEquals(PACKAGE, target.symbol.classId.packageFqName)
            assertEquals(changedPackage, file.packageDirective.packageFqName)
            val failure = assertThrows(IllegalStateException::class.java) {
                structure.classifierRelationFor(common.symbol, fixture.actualSession)
            }
            assertEquals("Original classifier file package does not describe its type header", failure.message)
        }
    }

    @Test
    fun validFieldOnlyGroupCapturesItsActualSessionButAuthorizesNoClassifier() {
        val fixture = Fixture()
        val field = field(fixture.actualSession)
        val prepared = group(fixture.actualSession, listOf(field))
        val structure = prepared.evaluateStructure()
        val unrelatedCommon = fixture.common()

        assertTrue(field.symbol.isBound)
        assertSame(field, field.symbol.cfir)
        assertSame(fixture.actualSession, prepared.classifierDomain.actualSession)
        assertSame(field.moduleData, prepared.classifierDomain.actualModule)
        assertSame(prepared.classifierDomain, structure.classifierDomain)
        assertEquals(listOf(field), structure.topLevelDeclarationsForMatching())
        assertTrue(structure.merge.initialBindings.isEmpty())
        assertThrows(IllegalStateException::class.java) {
            structure.classifierRelationFor(unrelatedCommon.symbol, fixture.actualSession)
        }
    }

    /** 原 fixture 创建并注册一次模块组件；依赖图只传真实模块对象，不按模块名重建。 */
    private inner class Fixture {
        val commonSession = CallResolutionTestFixtures.newTestSession()
        val actualSession = CallResolutionTestFixtures.newTestSession(
            dependencies = listOf(commonSession.moduleData),
            refinementDependencies = listOf(commonSession.moduleData),
        )

        fun common(arity: Int = 0, phase: CfirResolvePhase = CfirResolvePhase.TYPES): CfirClass =
            nominal(commonSession, common = true, arity = arity, phase = phase)

        fun specific(arity: Int = 0, phase: CfirResolvePhase = CfirResolvePhase.TYPES): CfirClass =
            nominal(actualSession, specific = true, arity = arity, phase = phase)
    }

    private val PACKAGE = FqName("classifier_domain")
    private val CLASS_ID = ClassId.topLevel(FqName("classifier_domain.Box"))

    /**
     * 复用完整 fixture 数据，明确创建持真实 PSI 的 Source 模型；Library 构造入口独立。
     * newClass 自身末尾重置为 RAW_CFIR；不修改它，phase 仅传给新声明的正式构造器。
     * TP 在构造时绑定真实 owner；Impl 构造器同时初始化 resolveState，不能再调用 RAW 初始化器。
     */
    private fun nominal(
        session: CallResolutionTestFixtures.TestSession,
        common: Boolean = false,
        specific: Boolean = false,
        arity: Int = 0,
        phase: CfirResolvePhase = CfirResolvePhase.TYPES,
        classId: ClassId = CLASS_ID,
        members: List<CfirDeclaration> = emptyList(),
    ): CfirClass {
        val role = when {
            common -> "common "
            specific -> "specific "
            else -> ""
        }
        val parameters = if (arity == 0) "" else (0 until arity).joinToString(", ", "<", ">") { "T$it" }
        val children = members.joinToString("\n") { (it.source as CjPsiSourceElement).psi.text }
        val psi = (createPsiFile("classifierDomain", "package ${classId.packageFqName.asString()}\n${role}class Box$parameters {\n$children\n}") as CjFile)
            .declarations.single() as CjClass
        val original = ExtendTestFixtures.newClass(session.moduleData, "Box", classId, declarations = members)
        val owner = CfirClassSymbol(classId)
        return buildClassCopy(original) {
            source = psi.toCjPsiSourceElement()
            origin = CfirDeclarationOrigin.Source
            symbol = owner
            resolvePhase = phase
            status = CfirDeclarationStatusImpl().apply {
                isCommon = common
                isSpecific = specific
            }
            typeParameters += psi.typeParameters.map { typeParameterPsi ->
                parameter(owner, session.moduleData, Name.identifier(checkNotNull(typeParameterPsi.name)),
                    CfirDeclarationOrigin.Source, typeParameterPsi.toCjPsiSourceElement())
            }
        }
    }

    private fun libraryNominal(
        module: CfirModuleData,
        common: Boolean = false,
        specific: Boolean = false,
        arity: Int = 0,
        phase: CfirResolvePhase = CfirResolvePhase.TYPES,
        classId: ClassId = CLASS_ID,
        members: List<CfirDeclaration> = emptyList(),
    ): CfirClass {
        val original = ExtendTestFixtures.newClass(module, "Box", classId, declarations = members)
        val owner = CfirClassSymbol(classId)
        return buildClassCopy(original) {
            source = null
            origin = CfirDeclarationOrigin.Library
            symbol = owner
            resolvePhase = phase
            status = CfirDeclarationStatusImpl().apply {
                isCommon = common
                isSpecific = specific
            }
            typeParameters += (0 until arity).map { index -> parameter(owner, module, Name.identifier("T$index"), CfirDeclarationOrigin.Library, null) }
        }
    }

    /** Library 模型使用真实二进制模块并一次绑定、注册原组件，不替代 actual Source caller。 */
    private fun libraryModule(): CfirModuleData {
        val module = CfirBinaryDependenciesModuleData(Name.identifier("classifier-domain-cjo"))
        val session = object : CfirSession(CfirSession.Kind.Library) {}
        module.bindSession(session)
        session.register(CfirModuleData::class, module)
        return module
    }

    /** 原物理文件仅从原顶层声明构造；按实际 module 对象分文件，不收 nested 或发现顺序追加项。 */
    private fun sourceFilesFor(originalTopLevel: List<CfirDeclaration>): List<CfirFile> =
        originalTopLevel.map { it.moduleData }.distinct().flatMap { module ->
            val moduleRoots = originalTopLevel.filter { it.moduleData === module }
            check(moduleRoots.all { it.origin.fromSource })
            fun sourcePackage(root: CfirDeclaration): FqName =
                ((root.source as CjPsiSourceElement).psi.containingFile as CjFile).packageFqName
            moduleRoots.map(::sourcePackage).distinct().map { packageName ->
                val roots = moduleRoots.filter { sourcePackage(it) == packageName }
                val text = roots.joinToString("\n") { (it.source as CjPsiSourceElement).psi.text }
                val psiFile = createPsiFile("classifierDomainRoots", "package ${packageName.asString()}\n$text") as CjFile
                val original = ExtendTestFixtures.newFile(module, packageName, roots)
                buildFileCopy(original) {
                    source = psiFile.toCjPsiSourceElement()
                    origin = CfirDeclarationOrigin.Source
                    symbol = CfirFileSymbol()
                }.also { file ->
                    assertSame(CfirDeclarationOrigin.Source, file.origin)
                    assertEquals(packageName, psiFile.packageFqName)
                    assertEquals(packageName, file.packageDirective.packageFqName)
                    roots.forEachIndexed { index, root ->
                        assertSame(root.moduleData, file.moduleData)
                        assertSame(root, file.declarations[index])
                        assertSame(CfirDeclarationOrigin.Source, root.origin)
                        if (root is CfirClass) {
                            assertEquals(root.symbol.classId.packageFqName, file.packageDirective.packageFqName)
                        }
                    }
                }
            }
        }

    private fun parameter(
        owner: CfirClassSymbol,
        module: CfirModuleData,
        parameterName: Name,
        declarationOrigin: CfirDeclarationOrigin,
        parameterSource: CjSourceElement?,
    ): CfirTypeParameter =
        buildTypeParameter {
            source = parameterSource
            moduleData = module
            resolvePhase = CfirResolvePhase.TYPES
            origin = declarationOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            containingDeclarationSymbol = owner
            symbol = CfirTypeParameterSymbol()
            name = parameterName
        }

    /** 失效负例先证明原输入合法，避免其它未满足的前提冒充动态身份检查。 */
    private fun assertOriginalSelection(
        structure: CfirCjmpStructureSnapshot,
        fixture: Fixture,
        common: CfirClass,
        specific: CfirClass,
        selected: Boolean,
    ) {
        val proof = structure.classifierRelationFor(common.symbol, fixture.actualSession)
        if (selected) {
            assertSame(specific.symbol, checkNotNull(proof).specific)
        } else {
            assertNull(proof)
        }
    }

    private fun field(session: CallResolutionTestFixtures.TestSession): CfirFieldVariable {
        val id = CallableId(PACKAGE, Name.identifier("value"))
        val psi = (createPsiFile("classifierDomainField", "package ${PACKAGE.asString()}\nvar value: Int64") as CjFile).declarations.single()
        return buildFieldVariable {
            source = psi.toCjPsiSourceElement()
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.TYPES
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            deprecationsProvider = EmptyDeprecationsProvider
            status = CfirDeclarationStatusImpl()
            isLocal = false
            isVar = true
            symbol = CfirFieldVariableSymbol(id)
            name = id.callableName
            returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
        }
    }

    /** 物理文件产品与发现次序分别传入；空 root plans 不声称已经完成最终匹配。 */
    private fun group(
        session: CallResolutionTestFixtures.TestSession,
        originalTopLevel: List<CfirDeclaration>,
        nominals: List<CfirClass> = originalTopLevel.filterIsInstance<CfirClass>(),
        sourceFiles: List<CfirFile> = sourceFilesFor(originalTopLevel),
        topLevelOrder: List<CfirBasedSymbol<*>> = originalTopLevel.map { it.symbol },
        containers: List<CjmpMergeContainer<CfirBasedSymbol<*>, CfirTypeParameterSymbol>> = nominals.map(::container),
    ): CfirCjmpPreparedGroup {
        val declarations = ArrayList<CfirDeclaration>()
        fun collect(declaration: CfirDeclaration) {
            declarations += declaration
            if (declaration is CfirClass) declaration.declarations.forEach(::collect)
        }
        originalTopLevel.forEach(::collect)
        val children: Map<CfirBasedSymbol<*>, List<CfirBasedSymbol<*>>> = declarations.filterIsInstance<CfirClass>()
            .associate { it.symbol to it.declarations.map { child -> child.symbol } }
        return CfirCjmpPreparedGroup(
            packageFqName = PACKAGE,
            roots = emptyList(),
            topLevelOrder = topLevelOrder,
            originalChildren = children,
            commonDeclarations = declarations.filter { (it as? CfirMemberDeclaration)?.status?.isCommon == true }.map { it.symbol },
            containers = containers,
            extensions = emptyList(),
            commonExtendOrder = emptyList(),
            originalDoNotExport = emptySet(),
            classifierDomain = CfirCjmpClassifierDomain.capture(session, declarations, sourceFiles),
        )
    }

    private fun container(declaration: CfirClass): CjmpMergeContainer<CfirBasedSymbol<*>, CfirTypeParameterSymbol> =
        CjmpMergeContainer(
            symbol = declaration.symbol,
            name = declaration.name.asString(),
            kind = CjmpContainerKind.CLASS,
            isCommon = declaration.status.isCommon,
            isSpecific = declaration.status.isSpecific,
            hasGeneric = declaration.typeParameters.isNotEmpty(),
            typeParameters = declaration.typeParameters.map { it.symbol },
            isNonExhaustive = false,
            doNotExport = false,
            members = declaration.declarations.mapIndexed { index, member ->
                val status = (member as CfirMemberDeclaration).status
                CjmpMergeMember(
                    symbol = member.symbol,
                    name = (member as CfirClass).name.asString(),
                    isInstanceVariable = false,
                    isMemberParameter = false,
                    isCommon = status.isCommon,
                    isSpecific = status.isSpecific,
                    hasCommonDefault = false,
                    doNotExport = false,
                    dependencies = CjmpInitializationDependencies.NotCollected,
                    isFromCommonPart = declaration.status.isCommon,
                    sourceOrder = index,
                )
            },
        )
}
