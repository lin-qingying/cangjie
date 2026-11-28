package org.cangnova.cangjie.cfir.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** 借用变量经过 subtyping stub 后仍保留原身份，不能在 PCLA 的公共父类型计算中变成 Any。 */
class ConeSubtypingStubRestorationTest {
    private val context = object : ConeInferenceContext {}

    @Test
    fun `restores the original borrowed variable constructor`() {
        val variable = ConeTypeVariable("borrowed")
        val stub = ConeStubType(variable.typeConstructor, ConeStubType.Kind.FOR_SUBTYPING)
        val restored = context.createSubstitutionFromSubtypingStubTypesToTypeVariables().substituteOrSelf(stub)
        assertEquals(variable.defaultType, restored)
        assertSame(variable.typeConstructor, (restored as ConeTypeVariableType).typeConstructor)
    }

    @Test
    fun `restores borrowed variables inside tuple and function types`() {
        val variable = ConeTypeVariable("borrowed")
        val stub = ConeStubType(variable.typeConstructor, ConeStubType.Kind.FOR_SUBTYPING)
        val builderStub = ConeStubType(variable.typeConstructor, ConeStubType.Kind.FOR_BUILDER_INFERENCE)
        val function = ConeFunctionType(listOf(stub), stub)
        val tuple = ConeTupleType(listOf(function, builderStub))
        val restored = context.createSubstitutionFromSubtypingStubTypesToTypeVariables().substituteOrSelf(tuple) as ConeTupleType
        val restoredFunction = restored.elementTypes[0] as ConeFunctionType
        assertEquals(variable.defaultType, restoredFunction.parameterTypes.single())
        assertEquals(variable.defaultType, restoredFunction.returnType)
        assertSame(builderStub, restored.elementTypes[1])
    }
}
