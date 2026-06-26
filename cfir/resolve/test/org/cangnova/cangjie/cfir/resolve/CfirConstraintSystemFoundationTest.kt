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

/**
 * [CfirConstraintSystemImpl] 基础约束传播测试。
 */
class CfirConstraintSystemFoundationTest {

    /**
     * 测试使用的类型关系服务。
     */
    private val typeRelations = CfirTypeRelations(FoundationTypeContext())

    /**
     * 验证 subtype 约束会把实际类型记录为类型变量下界。
     */
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

    /**
     * 验证 class-like 类型相等时会继续分解并传播匹配的类型实参。
     */
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

    /**
     * 验证 expected type 约束按 subtype 规则传播。
     */
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

    /**
     * 验证 upper bound 约束会写入变量上界。
     */
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

    /**
     * 验证兼容性检查失败时会记录 incompatible issue。
     */
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

    /**
     * 验证 quest fallback 兼容关系不会产生 issue。
     */
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

    /**
     * 验证 expected type 不兼容时会报告 incompatible issue。
     */
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

    /**
     * 验证 id 函数调用场景的端到端类型变量固定。
     */
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

    /**
     * 验证函数类型参数位置的逆变传播会记录变量上界。
     */
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

/**
 * foundation 测试使用的最小类型上下文。
 */
private class FoundationTypeContext : ConeTypeContext {
    /**
     * 测试上下文不提供额外父类型。
     */
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    /**
     * 按 primitive kind 或 class id 判断类型构造器是否相同。
     */
    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}
