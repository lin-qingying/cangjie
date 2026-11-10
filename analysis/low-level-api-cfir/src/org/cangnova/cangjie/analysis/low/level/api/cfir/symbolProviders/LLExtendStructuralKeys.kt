package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.normalizeCfirExtendTypeText
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty

/**
 * extend 的完整结构键：接收者 + 父类型 + 成员签名。
 *
 * 仅用「接收者 + 父类型」无法区分同一类型上的多个 extend（如 `extend Int64` 的
 * `operator *` 与 `operator -`、`extend Iterator<T>` 的 contains / max,min / zip 三组），
 * 会错误合并并丢成员。
 */
internal object LLExtendStructuralKeys {
    /** 二进制 extend 的完整键。 */
    fun of(extend: CfirExtend): String {
        val receiver = canonicalTypeName(renderTypeRef(extend.extendedTypeRef))
        val superTypes = extend.superTypeRefs
            .map { typeRef -> canonicalTypeName(renderTypeRef(typeRef)) }
            .filter(String::isNotEmpty)
            .sorted()
            .joinToString(",")
        val members = extend.declarations
            .filterIsInstance<CfirCallableDeclaration>()
            .map(::binaryMemberSignature)
            .sorted()
            .joinToString("&")
        return "$receiver<$superTypes>|$members"
    }

    /** PSI extend 的完整键（规范化规则与二进制侧一致）。 */
    fun of(extend: CjExtend): String {
        val receiver = canonicalTypeName(extend.receiverTypeReceiver?.getTypeText().orEmpty())
        val superTypes = extend.superTypeListEntries
            .mapNotNull { entry -> entry.typeReference?.getTypeText() }
            .map(::canonicalTypeName)
            .filter(String::isNotEmpty)
            .sorted()
            .joinToString(",")
        val members = extend.declarations
            .map(::psiMemberSignature)
            .sorted()
            .joinToString("&")
        return "$receiver<$superTypes>|$members"
    }

    /** 去掉 `R|` 标记、包前缀与声明种类前缀。 */
    fun canonicalTypeName(typeText: String): String =
        normalizeCfirExtendTypeText(typeText)
            .removePrefix("struct ")
            .removePrefix("class ")
            .removePrefix("interface ")
            .removePrefix("enum ")
            .trim()

    private fun renderTypeRef(typeRef: CfirTypeRef): String =
        CfirRenderer.withReadability().renderElementAsString(typeRef)

    private fun binaryMemberSignature(declaration: CfirCallableDeclaration): String {
        val name = declaration.symbol.callableId.callableName.asString()
        val kind = when (declaration) {
            is CfirProperty -> "prop"
            is CfirNamedFunction -> "func"
            is CfirFieldVariable -> "field"
            is CfirConstructor -> "init"
            else -> declaration::class.simpleName ?: "?"
        }
        val parameterCount = (declaration as? CfirNamedFunction)?.valueParameters?.size ?: 0
        return "$kind|$name|$parameterCount"
    }

    private fun psiMemberSignature(declaration: CjDeclaration): String = when (declaration) {
        is CjProperty -> "prop|${declaration.nameAsSafeName.asString()}|0"
        is CjNamedFunction -> "func|${declaration.nameAsSafeName.asString()}|${declaration.valueParameters.size}"
        is CjFieldVariable -> "field|${declaration.nameAsSafeName.asString()}|0"
        else -> "?|${(declaration as? org.cangnova.cangjie.psi.CjNamedDeclaration)?.name ?: "?"}|0"
    }
}
