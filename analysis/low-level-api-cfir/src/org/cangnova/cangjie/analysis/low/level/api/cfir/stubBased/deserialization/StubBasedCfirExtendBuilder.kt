package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.cfir.containingExtend
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.builder.buildExtend
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.source.CjRealPsiSourceElement

/**
 * 从 decompiled stub PSI 构建带 PSI 来源的 [CfirExtend]。
 *
 * `.cjo` 反序列化 extend 的 `source` 恒为 null，快速文档/转到声明没有目标。这里以 stub PSI 为
 * `source` 与成员来源；类型引用与隐式类型参数复用二进制 extend 的已解析结果
 * （`extend Array<T>` 的 `T` 不在 stub/PSI 中显式声明，单独从 PSI 无法解析）。
 */
internal class StubBasedCfirExtendBuilder(
    private val context: StubBasedCfirDeserializationContext,
) {
    /**
     * 构建 [extendPsi] 对应的 CFIR extend 声明。
     *
     * @param binaryExtend 同一 extend 的 `.cjo` 反序列化声明，提供已解析类型引用与隐式类型参数。
     */
    fun build(extendPsi: CjExtend, binaryExtend: CfirExtend): CfirExtend {
        val extendSymbol = CfirExtendSymbol()
        val local = StubBasedCfirDeserializationContext(
            moduleData = context.moduleData,
            packageFqName = context.packageFqName,
            relativeClassName = null,
            typeDeserializer = StubBasedCfirTypeDeserializer(
                moduleData = context.moduleData,
                annotationDeserializer = context.annotationDeserializer,
                parent = null,
                containingSymbol = extendSymbol,
                owner = extendPsi,
                initialOrigin = context.initialOrigin,
                additionalTypeParameters = binaryExtend.typeParameters.associate { typeParameter ->
                    typeParameter.name.asString() to typeParameter.symbol
                },
            ),
            annotationDeserializer = context.annotationDeserializer,
            containerSource = null,
            outerClassSymbol = null,
            outerTypeParameters = emptyList(),
            initialOrigin = context.initialOrigin,
        )
        val extend = buildExtend {
            source = CjRealPsiSourceElement(extendPsi)
            moduleData = local.moduleData
            origin = context.initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
            symbol = extendSymbol
            typeParameters += binaryExtend.typeParameters
            extendedTypeRef = binaryExtend.extendedTypeRef
            superTypeRefs += binaryExtend.superTypeRefs
            declarations += buildMembers(extendPsi, local)
        }
        // 语义对齐二进制声明：status/annotations 与成员的 dispatch receiver、status、annotations、deprecations。
        extend.replaceStatus(binaryExtend.status)
        extend.replaceAnnotations(binaryExtend.annotations)
        val binaryMembers = binaryExtend.declarations.filterIsInstance<CfirCallableDeclaration>().toMutableList()
        val psiMembers = extendPsi.declarations.filter { declaration ->
            declaration is CjNamedFunction || declaration is CjProperty || declaration is CjFieldVariable
        }
        psiMembers.zip(extend.declarations).forEach { (psiMember, declaration) ->
            val callable = declaration as? CfirCallableDeclaration ?: return@forEach
            callable.containingExtend = extend
            val kind = psiMember.kindKey()
            val name = psiMember.memberName()
            val arity = (psiMember as? CjNamedFunction)?.valueParameters?.size ?: 0
            // 按 (kind, name, arity) 从池中**消费**一个二进制成员：同名重载不会都配到第一个。
            val index = binaryMembers.indexOfFirst { candidate ->
                candidate.kindKey() == kind &&
                    candidate.symbol.callableId.callableName.asString() == name &&
                    ((candidate as? CfirNamedFunction)?.valueParameters?.size ?: 0) == arity
            }
            if (index < 0) return@forEach
            val binary = binaryMembers.removeAt(index)
            callable.replaceStatus(binary.status)
            callable.replaceAnnotations(binary.annotations)
            callable.replaceDispatchReceiverType(binary.dispatchReceiverType)
            callable.replaceDeprecationsProvider(binary.deprecationsProvider)
        }
        return extend
    }

    /** PSI 成员名。 */
    private fun org.cangnova.cangjie.psi.CjDeclaration.memberName(): String = when (this) {
        is CjNamedFunction -> nameAsSafeName.asString()
        is CjProperty -> nameAsSafeName.asString()
        is CjFieldVariable -> nameAsSafeName.asString()
        else -> "?"
    }

    /** PSI 成员种类键。 */
    private fun org.cangnova.cangjie.psi.CjDeclaration.kindKey(): String = when (this) {
        is CjNamedFunction -> "func"
        is CjProperty -> "prop"
        is CjFieldVariable -> "field"
        else -> "?"
    }

    /** 成员种类键，用于和二进制声明配对。 */
    private fun CfirCallableDeclaration.kindKey(): String = when (this) {
        is org.cangnova.cangjie.cfir.declarations.CfirProperty -> "prop"
        is org.cangnova.cangjie.cfir.declarations.CfirNamedFunction -> "func"
        is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> "field"
        is org.cangnova.cangjie.cfir.declarations.CfirConstructor -> "init"
        else -> "?"
    }

    /**
     * 构建 extend 成员（PSI 来源）。
     */
    private fun buildMembers(
        extendPsi: CjExtend,
        local: StubBasedCfirDeserializationContext,
    ): List<CfirDeclaration> = buildList {
        extendPsi.declarations.forEach { declaration ->
            when (declaration) {
                is CjNamedFunction -> add(
                    local.memberDeserializer.loadFunction(
                        declaration,
                        classSymbol = null,
                        session = local.session,
                    ),
                )

                is CjProperty -> add(local.memberDeserializer.loadProperty(declaration))
                is CjFieldVariable -> add(local.memberDeserializer.loadFieldVariable(declaration))
                else -> Unit
            }
        }
    }
}
