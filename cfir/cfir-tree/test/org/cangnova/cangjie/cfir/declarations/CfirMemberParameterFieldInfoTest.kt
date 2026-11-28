package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirMemberParameterFieldInfo
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.memberParameterFieldInfo
import org.cangnova.cangjie.cfir.recordMemberParameterFieldInfo
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

/** 成员参数事实冻结全部声明状态，并禁止后续阶段覆盖原始参数声明事实。 */
class CfirMemberParameterFieldInfoTest {
    @Test
    fun snapshotPreservesEveryPublicDeclarationFlagWithoutSharingMutableStatus() {
        // 从公共状态接口读取完整 flag 集合，新增状态位也必须经过同一快照契约。
        val flags = CfirDeclarationStatus::class.java.declaredMethods
            .filter { it.parameterCount == 0 && it.returnType == java.lang.Boolean.TYPE }
            .sortedBy(Method::getName)
        assertTrue(flags.isNotEmpty())
        val combinations = listOf(emptySet<String>(), flags.map { it.name }.toSet()) + flags.map { setOf(it.name) }
        for (enabled in combinations) {
            for (useInterfaceStatus in listOf(false, true)) {
                val input = CfirDeclarationStatusImpl(Visibilities.Private, Modality.OPEN)
                setFlags(input, flags) { it.name in enabled }
                val status = if (useInterfaceStatus) object : CfirDeclarationStatus by input {} else input
                val info = CfirMemberParameterFieldInfo(isVar = useInterfaceStatus, status = status)
                setFlags(input, flags) { it.name !in enabled }

                val first = info.createFieldStatus()
                val second = info.createFieldStatus()
                assertEquals(useInterfaceStatus, info.isVar)
                assertNotSame(input, first)
                assertNotSame(first, second)
                assertEquals(Visibilities.Private, first.visibility)
                assertEquals(Modality.OPEN, first.modality)
                assertFlags(first, flags, enabled)
                assertFlags(second, flags, enabled)

                setFlags(first, flags) { it.name !in enabled }
                assertFlags(second, flags, enabled)
                val fresh = info.createFieldStatus()
                assertNotSame(first, fresh)
                assertNotSame(second, fresh)
                assertFlags(fresh, flags, enabled)
                assertEquals(Visibilities.Private, fresh.visibility)
                assertEquals(Modality.OPEN, fresh.modality)
            }
        }
    }

    @Test
    fun recordingFactsPublishesOnceAndDoesNotMutateOrRetainTheInputStatus() {
        val parameter = valueParameter("member")
        assertNull(parameter.memberParameterFieldInfo)
        assertFalse(parameter.isMemberParameter)
        val input = CfirDeclarationStatusImpl(Visibilities.Protected, null).apply {
            isVisibilityExplicit = true
            isUnsafe = true
        }

        parameter.recordMemberParameterFieldInfo(isVar = true, status = input)
        val info = requireNotNull(parameter.memberParameterFieldInfo)
        assertTrue(parameter.isMemberParameter)
        assertTrue(info.isVar)
        assertTrue(input.isUnsafe)
        input.isUnsafe = false
        assertTrue(info.createFieldStatus().isUnsafe)
        assertEquals(Visibilities.Protected, info.createFieldStatus().visibility)
        assertNull(info.createFieldStatus().modality)
        assertFalse(parameter.status.isUnsafe)

        assertThrows(IllegalStateException::class.java) {
            parameter.recordMemberParameterFieldInfo(isVar = false, status = CfirDeclarationStatusImpl())
        }
        assertSame(info, parameter.memberParameterFieldInfo)
        assertTrue(parameter.isMemberParameter)
        assertTrue(info.isVar)
        assertTrue(info.createFieldStatus().isUnsafe)
    }

    private fun assertFlags(status: CfirDeclarationStatus, flags: List<Method>, enabled: Set<String>) {
        for (getter in flags) {
            assertEquals(getter.name in enabled, getter.invoke(status), "Snapshot lost ${getter.name}; enabled flags: $enabled")
        }
    }

    private fun setFlags(status: CfirDeclarationStatusImpl, flags: List<Method>, enabled: (Method) -> Boolean) {
        for (getter in flags) {
            val setter = CfirDeclarationStatusImpl::class.java.getMethod(
                "set${getter.name.removePrefix("is")}", java.lang.Boolean.TYPE,
            )
            setter.invoke(status, enabled(getter))
        }
    }

    private fun valueParameter(parameterName: String): CfirValueParameter = buildValueParameter {
        source = TestParameterSourceElement(parameterName)
        moduleData = CfirSourceModuleData(
            name = Name.identifier("member-parameter-info-test"),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
            platform = CfirPlatform.DEFAULT,
        )
        resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        dispatchReceiverType = null
        symbol = CfirValueParameterSymbol(CallableId(Name.identifier(parameterName)))
        containingDeclarationSymbol = CfirNamedFunctionSymbol(CallableId(Name.identifier("constructor")))
        isNamed = false
        status = CfirDeclarationStatusImpl()
        returnTypeRef = buildImplicitTypeRef()
        name = Name.identifier(parameterName)
    }

    /** 复用 CfirDeclarationToStringTest 的轻量测试来源模式，不伪造 PSI，也不放宽 Source 声明不变量。 */
    private class TestParameterSourceElement(parameterName: String) : CjBinarySourceElement(
        debugText = "parameter $parameterName",
        binaryFilePath = null,
        stableIdentity = parameterName,
    )
}
