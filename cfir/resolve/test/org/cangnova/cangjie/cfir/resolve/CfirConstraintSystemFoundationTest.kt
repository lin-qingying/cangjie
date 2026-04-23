package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraint
import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirConstraintSystemFoundationTest {

    private val typeRelations = CfirTypeRelations(FoundationTypeContext())

    @Test
    fun `subtype constraint binds lower bound for type variable`() {
        val system = CfirConstraintSystemImpl(typeRelations)
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = system.nextFreshTypeId(),
            lookupTag = ConeTypeParameterLookupTag("T"),
        )
        system.registerTypeVariable(variable)

        system.addSubtypeConstraint(
            ConePrimitiveType.INT32,
            ConeTypeParameterType(variable.lookupTag, isPlaceholder = true),
            CfirConstraintPosition.ArgumentPosition(0),
        )

        assertEquals(listOf(ConePrimitiveType.INT32), variable.lowerBounds)
    }

    @Test
    fun `class like equality propagates to matching type arguments`() {
        val system = CfirConstraintSystemImpl(typeRelations)
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = system.nextFreshTypeId(),
            lookupTag = ConeTypeParameterLookupTag("T"),
        )
        system.registerTypeVariable(variable)

        val boxId = ClassId(FqName("test"), Name.identifier("Box"))
        val concrete = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConePrimitiveType.INT32))
        val generic = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)))

        system.addSubtypeConstraint(concrete, generic, CfirConstraintPosition.ArgumentPosition(0))

        assertTrue(variable.lowerBounds.contains(ConePrimitiveType.INT32) || variable.upperBounds.contains(ConePrimitiveType.INT32))
    }

    @Test
    fun `expected type constraint propagates like subtype`() {
        val system = CfirConstraintSystemImpl(typeRelations)
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = system.nextFreshTypeId(),
            lookupTag = ConeTypeParameterLookupTag("E"),
        )
        system.registerTypeVariable(variable)

        system.addConstraint(
            CfirConstraint.ExpectedType(
                actual = ConePrimitiveType.INT32,
                expected = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true),
            ),
        )

        assertEquals(listOf(ConePrimitiveType.INT32), variable.lowerBounds)
    }

    @Test
    fun `upper bound constraint records bound on variable`() {
        val system = CfirConstraintSystemImpl(typeRelations)
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = system.nextFreshTypeId(),
            lookupTag = ConeTypeParameterLookupTag("U"),
        )
        system.registerTypeVariable(variable)

        system.addConstraint(
            CfirConstraint.UpperBound(
                variable = variable,
                bound = ConePrimitiveType.INT32,
            ),
        )

        assertEquals(listOf(ConePrimitiveType.INT32), variable.upperBounds)
    }

    @Test
    fun `compatible constraint records incompatibility issue when relation fails`() {
        val system = CfirConstraintSystemImpl(typeRelations)

        system.addConstraint(
            CfirConstraint.Compatibility(
                source = ConePrimitiveType.BOOLEAN,
                target = ConePrimitiveType.INT32,
                position = CfirConstraintPosition.ArgumentPosition(0),
            ),
        )

        assertTrue(system.errors.any { it is CfirConstraintIssue.IncompatibleTypes })
    }

    @Test
    fun `compatible constraint allows quest fallback without issue`() {
        val system = CfirConstraintSystemImpl(typeRelations)

        system.addConstraint(
            CfirConstraint.Compatibility(
                source = ConeQuestType(),
                target = ConePrimitiveType.INT32,
                position = CfirConstraintPosition.ArgumentPosition(0),
            ),
        )

        assertTrue(system.errors.isEmpty())
    }

    @Test
    fun `expected type incompatible relation reports issue`() {
        val system = CfirConstraintSystemImpl(typeRelations)

        system.addConstraint(
            CfirConstraint.ExpectedType(
                actual = ConePrimitiveType.BOOLEAN,
                expected = ConePrimitiveType.INT32,
                position = CfirConstraintPosition.ExpectedType,
            ),
        )

        assertTrue(system.errors.any { it is CfirConstraintIssue.IncompatibleTypes })
    }

    @Test
    fun `end to end id function inference`() {
        // fun id<T>(x: T): T  调用 id(42)
        val system = CfirConstraintSystemImpl(typeRelations)
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = system.nextFreshTypeId(),
            lookupTag = ConeTypeParameterLookupTag("T"),
        )
        system.registerTypeVariable(variable)

        // 参数约束：Int64 <: T
        system.addSubtypeConstraint(
            ConePrimitiveType.INT64,
            ConeTypeParameterType(variable.lookupTag, isPlaceholder = true),
            CfirConstraintPosition.ArgumentPosition(0),
        )

        system.fixAllVariables()
        val result = system.buildResult()

        assertTrue(result.isFullyResolved)
        assertEquals(ConePrimitiveType.INT64, variable.fixedType)
    }

    @Test
    fun `function type propagation with type variable`() {
        val system = CfirConstraintSystemImpl(typeRelations)
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = system.nextFreshTypeId(),
            lookupTag = ConeTypeParameterLookupTag("T"),
        )
        system.registerTypeVariable(variable)

        val placeholderT = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)

        // (Int32) -> Unit  <:  (T) -> Unit
        system.addSubtypeConstraint(
            ConeFunctionType(listOf(ConePrimitiveType.INT32), ConePrimitiveType.UNIT),
            ConeFunctionType(listOf(placeholderT), ConePrimitiveType.UNIT),
            CfirConstraintPosition.ArgumentPosition(0),
        )

        // 函数参数逆变：T <: Int32
        assertTrue(variable.upperBounds.contains(ConePrimitiveType.INT32))
    }
}

private class FoundationTypeContext : ConeTypeContext {
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}
