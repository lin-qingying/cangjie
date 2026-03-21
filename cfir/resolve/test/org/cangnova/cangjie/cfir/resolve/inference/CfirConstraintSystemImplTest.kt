@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.asResolveState
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirConstraintSystemImplTest {

    private val subtypeChecker = ConeSubtypeChecker(ConstraintTestTypeContext())

    @Test
    fun `decompose invariant class type arguments and infer type variable`() {
        val system = CfirConstraintSystemImpl(subtypeChecker = subtypeChecker)
        val t = newTypeVariable(system, "T")
        val boxId = ClassId(FqName("test"), Name.identifier("Box"))

        val argType = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConePrimitiveType.INT32))
        val paramType = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConeTypeParameterType(t.lookupTag)))

        system.addSubtypeConstraint(argType, paramType, CfirConstraintPosition.ArgumentPosition(0))
        system.fixAllVariables()

        assertEquals(ConePrimitiveType.INT32, t.fixedType)
        assertTrue(system.errors.isEmpty())
    }

    @Test
    fun `function subtype constraints should infer from contravariant parameter`() {
        val system = CfirConstraintSystemImpl(subtypeChecker = subtypeChecker)
        val t = newTypeVariable(system, "T")

        val sub = ConeFuncType(
            parameterTypes = listOf(ConePrimitiveType.INT32),
            returnType = ConePrimitiveType.INT32,
        )
        val sup = ConeFuncType(
            parameterTypes = listOf(ConeTypeParameterType(t.lookupTag)),
            returnType = ConePrimitiveType.INT32,
        )

        system.addSubtypeConstraint(sub, sup, CfirConstraintPosition.ArgumentPosition(0))
        system.fixAllVariables()

        assertEquals(ConePrimitiveType.INT32, t.fixedType)
        assertTrue(system.errors.isEmpty())
    }

    @Test
    fun `fixation should respect variable dependency order`() {
        val system = CfirConstraintSystemImpl(subtypeChecker = subtypeChecker)
        val t = newTypeVariable(system, "T")
        val u = newTypeVariable(system, "U")

        val tType = ConeTypeParameterType(t.lookupTag)
        val uType = ConeTypeParameterType(u.lookupTag)

        system.addSubtypeConstraint(uType, tType, CfirConstraintPosition.ArgumentPosition(0))
        system.addSubtypeConstraint(ConePrimitiveType.INT32, uType, CfirConstraintPosition.ArgumentPosition(1))
        system.fixAllVariables()

        assertEquals(ConePrimitiveType.INT32, u.fixedType)
        assertEquals(ConePrimitiveType.INT32, t.fixedType)
        assertTrue(system.errors.isEmpty())
    }

    @Test
    fun `bound should be propagated through other variable bounds`() {
        val system = CfirConstraintSystemImpl(subtypeChecker = subtypeChecker)
        val t = newTypeVariable(system, "T")
        val u = newTypeVariable(system, "U")
        val boxId = ClassId(FqName("test"), Name.identifier("Box"))

        val tType = ConeTypeParameterType(t.lookupTag)
        val boxOfT = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(tType))
        val uType = ConeTypeParameterType(u.lookupTag)

        system.addSubtypeConstraint(uType, boxOfT, CfirConstraintPosition.ArgumentPosition(0))
        system.addSubtypeConstraint(tType, ConePrimitiveType.INT32, CfirConstraintPosition.ArgumentPosition(1))

        val propagated = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConePrimitiveType.INT32))
        assertTrue(u.upperBounds.any { it == propagated })
    }

    @Test
    fun `conflicting constraints should be reported`() {
        val system = CfirConstraintSystemImpl(subtypeChecker = subtypeChecker)
        val t = newTypeVariable(system, "T")
        val tType = ConeTypeParameterType(t.lookupTag)

        system.addSubtypeConstraint(ConePrimitiveType.INT32, tType, CfirConstraintPosition.ArgumentPosition(0))
        system.addSubtypeConstraint(tType, ConePrimitiveType.BOOLEAN, CfirConstraintPosition.ArgumentPosition(1))
        system.fixAllVariables()

        assertTrue(system.hasErrors)
    }

    private fun newTypeVariable(system: CfirConstraintSystemImpl, name: String): CfirTypeVariable {
        val symbol = CfirTypeParameterSymbol()
        val typeParameter = CfirTypeParameterImpl(
            source = null,
            moduleData = CallResolutionTestFixtures.TEST_MODULE_DATA,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Source,
            attributes = CfirDeclarationAttributes.EMPTY,
            name = Name.identifier(name),
            bounds = emptyList(),
        )
        typeParameter.resolveState = CfirResolvePhase.BODY_RESOLVE.asResolveState()
        symbol.bind(typeParameter)
        return CfirTypeVariable(
            typeParameter = symbol,
            freshTypeId = system.nextFreshTypeId(),
            lookupTag = ConeTypeParameterLookupTag(name),
        ).also(system::registerTypeVariable)
    }
}

private class ConstraintTestTypeContext : ConeTypeContext {
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}
