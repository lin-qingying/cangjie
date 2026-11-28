package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.source.psi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

/** 只遍历已物化的 RAW 树；不调用 symbol/type API 或语义解析来选定测试目标。 */
internal fun collectEnumImplicitBindings(root: CfirElement): List<CfirPatternBindingVariable> {
    val bindings = mutableListOf<CfirPatternBindingVariable>()
    root.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (element is CfirPatternBindingVariable) bindings += element
            element.acceptChildren(this)
        }
    })
    return bindings
}

/** 每个 active leaf 的 symbol 必须绑定该真实节点；相同类型的兄弟也不能共享 symbol。 */
internal fun assertEnumLeafSymbols(bindings: List<CfirPatternBindingVariable>) {
    bindings.forEachIndexed { index, binding ->
        assertSame(binding, binding.symbol.cfir)
        for (other in index + 1 until bindings.size) {
            assertNotSame(binding, bindings[other])
            assertNotSame(binding.symbol, bindings[other].symbol)
        }
    }
}

/** root 是可变语义树；替换保留 source、当前 owner 与每个真实声明，并解除旧 root 归属。 */
internal fun assertEnumResolvedRoot(
    owner: CfirPatternTreeOwner,
    rawPattern: CfirPattern,
    currentPattern: CfirPattern,
    rawBindings: List<CfirPatternBindingVariable>,
) {
    assertTrue(currentPattern is CfirEnumPattern)
    assertSame(currentPattern, owner.patternTreeRoot())
    assertSame(owner, currentPattern.patternTreeOwner)
    assertSame(rawPattern.source, currentPattern.source)
    assertSame(rawPattern.source?.psi, currentPattern.source?.psi)
    if (currentPattern !== rawPattern) assertNull(rawPattern.patternTreeOwner)
    val active = currentPattern.bindingVariables()
    assertEquals(rawBindings.size, active.size)
    active.forEachIndexed { index, binding ->
        assertSame(rawBindings[index], binding)
        assertSame(rawBindings[index].symbol, binding.symbol)
        assertSame(owner, binding.patternTreeOwner)
    }
    assertEnumLeafSymbols(active)
}

/** 类型 oracle 来自实际 source enum 的 ClassId 和官方确认的泛型实参，不能接受错误或默认类型。 */
internal fun assertEnumImplicitType(type: ConeCangJieType, enum: CfirEnum) {
    // 两种正规 enum use-site 表示都走既有归一化；Error/Stub/TV 不进入该入口。
    assertTrue(type is ConeEnumType || type is ConeClassLikeType)
    val actual = requireNotNull(type.expandedPatternEnumType(enum.moduleData.session))
    assertEquals(enum.symbol.classId, actual.classId)
    assertEquals(1, actual.typeArguments.size)
    assertEquals(ConePrimitiveType.INT64, actual.typeArguments.single().type)
}

/** 记录实际对象及直接字段，不从名字/offset 恢复身份，也不为日志请求更高解析阶段。 */
internal fun recordEnumImplicitState(
    stage: String,
    fixtureMarker: String,
    owner: CfirPatternTreeOwner,
    target: CfirPatternBindingVariable,
    bindings: List<CfirPatternBindingVariable>,
    container: CfirElementWithResolveState? = null,
) {
    fun identity(value: Any?): String =
        if (value == null) "null" else value::class.simpleName + "@" + System.identityHashCode(value).toString(16)
    val ownerElement = owner as CfirElement
    val phase = (owner as? CfirElementWithResolveState)?.resolvePhase?.name ?: "NO_DECLARATION_PHASE"
    println("ENUM_COLD_IMPLICIT stage=$stage marker=$fixtureMarker requested=IMPLICIT_TYPES " +
        "target=${identity(target)} symbol=${identity(target.symbol)} targetPhase=${target.resolvePhase} " +
        "owner=${identity(owner)} ownerPhase=$phase ownerPsi=${identity(ownerElement.source?.psi)} " +
        "root=${identity(owner.patternTreeRoot())} container=${identity(container)} containerPhase=${container?.resolvePhase}")
    bindings.forEachIndexed { index, binding ->
        val typeRef = binding.returnTypeRef
        val type = (typeRef as? CfirResolvedTypeRef)?.coneType
        println("ENUM_COLD_LEAF stage=$stage index=$index leaf=${identity(binding)} " +
            "symbol=${identity(binding.symbol)} phase=${binding.resolvePhase} " +
            "owner=${identity(binding.patternTreeOwner)} psi=${identity(binding.source?.psi)} " +
            "typeRef=${identity(typeRef)} type=$type")
    }
}
