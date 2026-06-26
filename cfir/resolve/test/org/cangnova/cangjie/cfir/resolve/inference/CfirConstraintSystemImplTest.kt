@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.asResolveState
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.resolve.CfirConstraintSystemImpl
import org.cangnova.cangjie.cfir.resolve.CfirTypeRelations
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
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
 * [CfirConstraintSystemImpl] 在 inference 包中的端到端求解测试。
 */
class CfirConstraintSystemImplTest {

    /**
     * 测试使用的类型关系服务。
     */
    private val typeRelations = CfirTypeRelations(ConstraintTestTypeContext())

    /**
     * 验证 invariant class 类型实参可以分解并推断类型变量。
     */
    @Test
    fun `decompose invariant class type arguments and infer type variable`() {
        val system = CfirConstraintSystemImpl(typeRelations)
        val t = newTypeVariable(system, "T")
        val boxId = ClassId(FqName("test"), Name.identifier("Box"))

        val argType = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConePrimitiveType.INT32))
        val paramType = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConeTypeParameterType(t.lookupTag)))

        system.addSubtypeConstraint(argType, paramType, CfirConstraintPosition.ArgumentPosition(0))
        system.fixAllVariables()

        assertEquals(ConePrimitiveType.INT32, t.fixedType)
        assertTrue(system.errors.isEmpty())
    }

    /**
     * 验证函数参数逆变位置可以参与类型变量推断。
     */
    @Test
    fun `function subtype constraints should infer from contravariant parameter`() {
        val system = CfirConstraintSystemImpl(typeRelations)
        val t = newTypeVariable(system, "T")

        val sub = ConeFunctionType(
            parameterTypes = listOf(ConePrimitiveType.INT32),
            returnType = ConePrimitiveType.INT32,
        )
        val sup = ConeFunctionType(
            parameterTypes = listOf(ConeTypeParameterType(t.lookupTag)),
            returnType = ConePrimitiveType.INT32,
        )

        system.addSubtypeConstraint(sub, sup, CfirConstraintPosition.ArgumentPosition(0))
        system.fixAllVariables()

        assertEquals(ConePrimitiveType.INT32, t.fixedType)
        assertTrue(system.errors.isEmpty())
    }

    /**
     * 验证变量固定按依赖顺序传播。
     */
    @Test
    fun `fixation should respect variable dependency order`() {
        val system = CfirConstraintSystemImpl(typeRelations)
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

    /**
     * 验证一个变量的 bound 会通过其他变量 bound 继续传播。
     */
    @Test
    fun `bound should be propagated through other variable bounds`() {
        val system = CfirConstraintSystemImpl(typeRelations)
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

    /**
     * 验证冲突约束会记录错误。
     */
    @Test
    fun `conflicting constraints should be reported`() {
        val system = CfirConstraintSystemImpl(typeRelations)
        val t = newTypeVariable(system, "T")
        val tType = ConeTypeParameterType(t.lookupTag)

        system.addSubtypeConstraint(ConePrimitiveType.INT32, tType, CfirConstraintPosition.ArgumentPosition(0))
        system.addSubtypeConstraint(tType, ConePrimitiveType.BOOLEAN, CfirConstraintPosition.ArgumentPosition(1))
        system.fixAllVariables()

        assertTrue(system.hasErrors)
    }

    /**
     * 构造并注册测试类型变量。
     */
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

/**
 * constraint system 测试使用的类型上下文。
 */
private class ConstraintTestTypeContext : ConeTypeContext {
    /**
     * 测试上下文不提供额外父类型。
     */
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    /**
     * 按 primitive kind 或 class id 判断类型构造器一致性。
     */
    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}
