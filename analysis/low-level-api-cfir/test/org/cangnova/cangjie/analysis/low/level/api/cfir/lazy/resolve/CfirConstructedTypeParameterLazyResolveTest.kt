package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirModifiablePsiSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * constructed-class 引用跨 LL 阶段保持原名义声明身份，而不是构造器的自有泛型声明。
 *
 * 仓颉输入对应官方1.1.3的 review-20261110/type-parameter-capture/positive.cj。
 * Kotlin LLFirLazyResolver 只检查自有 FirTypeParameter，借用引用由实际 class scope 提供；
 * 这里通过构造器优先的真实懒解析入口验证 TYPES、STATUS 和 BODY_RESOLVE 的完整消费链。
 */
class CfirConstructedTypeParameterLazyResolveTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/constructedTypeParameterLazyResolve",
) {
    override val configurator = analysisApiCfirModifiablePsiSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun classPrimaryFirst(mainFile: CjFile) = checkConstructors(mainFile, primaryFirst = true, expectedCount = 2)

    @Test
    fun classSecondaryFirst(mainFile: CjFile) = checkConstructors(mainFile, primaryFirst = false, expectedCount = 2)

    @Test
    fun structPrimaryFirst(mainFile: CjFile) = checkConstructors(mainFile, primaryFirst = true, expectedCount = 2)

    @Test
    fun structSecondaryFirst(mainFile: CjFile) = checkConstructors(mainFile, primaryFirst = false, expectedCount = 2)

    @Test
    fun implicitClass(mainFile: CjFile) = checkConstructors(mainFile, primaryFirst = true, expectedCount = 1)

    @Test
    fun implicitStruct(mainFile: CjFile) = checkConstructors(mainFile, primaryFirst = true, expectedCount = 1)

    @Test
    fun enumPayloadFirst(mainFile: CjFile) = checkEnum(mainFile, payloadFirst = true)

    @Test
    fun enumEmptyFirst(mainFile: CjFile) = checkEnum(mainFile, payloadFirst = false)

    private fun checkConstructors(mainFile: CjFile, primaryFirst: Boolean, expectedCount: Int) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val owner = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val constructors = owner.declarations.filterIsInstance<CfirConstructor>()
        assertEquals(expectedCount, constructors.size)
        assertTrue(constructors.none { it.status.isStatic })
        constructors.forEach { constructor ->
            val designation = constructor.collectDesignation(file)
            assertEquals(2, designation.path.size)
            assertSame(file, designation.path.first())
            assertSame(owner, designation.path.last())
            assertSame(constructor, designation.target)
        }
        checkPhases(owner, constructors.sortedBy { it.isPrimary != primaryFirst })
    }

    private fun checkEnum(mainFile: CjFile, payloadFirst: Boolean) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val owner = file.declarations.filterIsInstance<CfirEnum>().single()
        assertTrue(owner.declarations.filterIsInstance<CfirConstructor>().isEmpty())
        val cases = owner.declarations.filterIsInstance<CfirEnumConstructor>()
        assertEquals(2, cases.size)
        assertEquals(listOf(0, 1), cases.map { it.valueParameters.size }.sorted())
        checkPhases(owner, cases.sortedBy { it.valueParameters.isNotEmpty() != payloadFirst })
    }

    /** 不先解析 owner；每轮分别从目标构造器进入，检查真实声明、引用和已解析类型。 */
    private fun checkPhases(owner: CfirClassLikeDeclaration, targets: List<CfirCallableDeclaration>) {
        val ownerParameters = owner.typeParameters.toList()
        assertEquals(1, ownerParameters.size)
        val references = targets.map { it.typeParameters.toList() }
        targets.zip(references).forEach { (_, refs) ->
            assertEquals(ownerParameters.size, refs.size)
            assertTrue(refs.all { it is CfirConstructedClassTypeParameterRef })
        }

        for (phase in listOf(CfirResolvePhase.TYPES, CfirResolvePhase.STATUS, CfirResolvePhase.BODY_RESOLVE)) {
            targets.zip(references).forEach { (target, refs) ->
                target.lazyResolveToPhase(phase)
                assertTrue(target.resolvePhase >= phase)
                assertSame(target, target.symbol.cfir)
                ownerParameters.zip(owner.typeParameters).forEach { (original, current) ->
                    assertSame(original, current)
                    assertSame(owner.symbol, (current as CfirTypeParameter).containingDeclarationSymbol)
                    assertSame(current, current.symbol.cfir)
                }
                assertEquals(refs.size, target.typeParameters.size)
                assertEquals(ownerParameters.size, owner.typeParameters.size)
                refs.zip(target.typeParameters).forEach { (original, current) -> assertSame(original, current) }
                refs.zip(ownerParameters).forEach { (ref, parameter) -> assertSame(parameter.symbol, ref.symbol) }

                val constructedType = target.returnTypeRef.coneType as ConeLookupTagBasedType
                assertSame(owner.symbol.toLookupTag(), constructedType.lookupTag, "Constructor owner at $phase: $constructedType")
                assertEquals(ownerParameters.size, constructedType.typeArguments.size)
                constructedType.typeArguments.zip(ownerParameters).forEach { (argument, parameter) ->
                    assertSame(parameter.symbol, (argument.type as ConeTypeParameterType).lookupTag.typeParameterSymbol)
                }

                val valueParameters = when (target) {
                    is CfirConstructor -> target.valueParameters
                    is CfirEnumConstructor -> target.valueParameters
                    else -> error("Expected an actual constructor target")
                }
                valueParameters.firstOrNull()?.let { parameter ->
                    val parameterType = parameter.returnTypeRef.coneType as ConeTypeParameterType
                    assertSame(ownerParameters.single().symbol, parameterType.lookupTag.typeParameterSymbol)
                    assertSame(target.symbol, parameter.containingDeclarationSymbol)
                }
                if (phase == CfirResolvePhase.BODY_RESOLVE && target is CfirConstructor) {
                    assertFalse(target.body is CfirLazyBlock)
                }
            }
        }
    }
}
