package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 一个原类型节点的最终保存选择。语义类型及选择后的有序子槽分别拥有身份，不能把
 * 同一 Cone 的表达式用途与显式 Alias 用途合并。这里不构造 Cone，也不分配格式编号。
 */
sealed interface CfirExportTypeUse {
    /** 已完成的语义 Ty；包括符合资格的完整实例化 Alias Ty，均在此停止语法递归。 */
    data class Semantic(val type: CfirExportTypeElement) : CfirExportTypeUse
    data class Arguments(val type: CfirExportTypeElement, val arguments: List<CfirExportTypeUse>) : CfirExportTypeUse
    data class Function(
        val type: CfirExportTypeElement,
        val parameters: List<CfirExportTypeUse>,
        val returnType: CfirExportTypeUse,
        val isC: Boolean,
    ) : CfirExportTypeUse
    /** 固定 native ObtainsAliasType 对错误节点的明确结果；不是未支持类型的兜底。 */
    data object Invalid : CfirExportTypeUse
}

/**
 * AnalyzeFunctionLinkage 收敛后一次冻结的类型使用图。固定 v113 TypeManager.cpp
 * 2373–2487 的节点选择在 providers 完成，serialization 只做 TP 投影与 d/e/t 编号。
 * Alias 的 self/RHS/use、函数签名返回/正文返回和 inheritedTypes 都是独立入口。
 */
class CfirExportTypeUseGraph private constructor(
    private val references: Map<CfirTypeRef, CfirExportTypeUse>,
    private val declarations: Map<CfirDeclaration, CfirExportTypeUse?>,
    private val aliasRightHandSides: Map<CfirTypeAlias, CfirExportTypeUse>,
    private val functionReturns: Map<CfirFunction, CfirExportTypeUse?>,
    private val enumConstructorReturns: Map<CfirEnumConstructor, CfirExportTypeUse?>,
    private val inheritedTypes: Map<CfirDeclaration, List<CfirExportTypeUse>>,
) {
    fun reference(ref: CfirTypeRef): CfirExportTypeUse = references.getValue(ref)
    fun declarationType(declaration: CfirDeclaration): CfirExportTypeUse? = declarations.getValue(declaration)
    fun aliasRightHandSide(alias: CfirTypeAlias): CfirExportTypeUse = aliasRightHandSides.getValue(alias)
    /** 没有原 Type 节点时返回 null，不能以推断后的签名返回补造 FuncBody.retType。 */
    fun functionReturn(function: CfirFunction): CfirExportTypeUse? = functionReturns.getValue(function)
    fun enumConstructorReturn(constructor: CfirEnumConstructor): CfirExportTypeUse? = enumConstructorReturns.getValue(constructor)
    fun superTypes(declaration: CfirDeclaration): List<CfirExportTypeUse> = inheritedTypes.getValue(declaration)

    internal companion object {
        fun freeze(input: CfirExportAnalysisInputSnapshot, analysis: CfirDeclarationExportAnalysis): CfirExportTypeUseGraph =
            Selection(input, analysis).run()
    }

    private class Selection(
        private val input: CfirExportAnalysisInputSnapshot,
        private val analysis: CfirDeclarationExportAnalysis,
    ) {
        private val references = IdentityHashMap<CfirTypeRef, CfirExportTypeUse>()
        private val declarations = IdentityHashMap<CfirDeclaration, CfirExportTypeUse?>()
        private val aliasRightHandSides = IdentityHashMap<CfirTypeAlias, CfirExportTypeUse>()
        private val functionReturns = IdentityHashMap<CfirFunction, CfirExportTypeUse?>()
        private val enumConstructorReturns = IdentityHashMap<CfirEnumConstructor, CfirExportTypeUse?>()
        private val inheritedTypes = IdentityHashMap<CfirDeclaration, List<CfirExportTypeUse>>()
        private val semanticUses = IdentityHashMap<CfirExportTypeElement, CfirExportTypeUse.Semantic>()
        private val selecting = Collections.newSetFromMap(IdentityHashMap<CfirTypeRef, Boolean>())

        fun run(): CfirExportTypeUseGraph {
            input.elements.forEach { (element, snapshot) ->
                if (element is CfirTypeRef && snapshot.resolvedType != null && !snapshot.isSyntax) selectReference(element)
            }
            input.declarations.keys.forEach { declaration ->
                selectDeclaration(declaration)
                val data = input.declaration(declaration)
                if (declaration is CfirTypeAlias && data.aliasUnderlyingType != null) {
                    aliasRightHandSides[declaration] = selectReference(checkNotNull(data.headerTypeRef))
                }
                if (declaration is CfirFunction) {
                    functionReturns[declaration] = if (!input.isSourceDeclaration(declaration) && input.hasCanonicalSourceOf(declaration)) {
                        input.canonicalSourceGraphOf(declaration).functionHeaders[declaration]?.returnType?.let {
                            semantic(input.type(it, declaration.moduleData.session))
                        }
                    } else actualTypeNode(data.headerTypeRef)?.let(::selectReference)
                }
                if (declaration is CfirEnumConstructor) {
                    enumConstructorReturns[declaration] = if (!input.isSourceDeclaration(declaration) && input.hasCanonicalSourceOf(declaration)) {
                        input.canonicalSourceGraphOf(declaration).functionHeaders[declaration]?.returnType?.let {
                            semantic(input.type(it, declaration.moduleData.session))
                        }
                    } else if (data.valueParameters.isEmpty()) null else actualTypeNode(data.headerTypeRef)?.let(::selectReference)
                }
                val refs = if (declaration is CfirClassLikeDeclaration && input.isSourceDeclaration(declaration) && input.hasCanonicalSourceOf(declaration))
                    input.canonicalSourceGraphOf(declaration).inheritedTypeReferences.getValue(declaration) else data.superTypeReferences
                inheritedTypes[declaration] = immutable(refs.map(::selectReference))
            }
            return CfirExportTypeUseGraph(immutable(references), immutable(declarations), immutable(aliasRightHandSides),
                immutable(functionReturns), immutable(enumConstructorReturns), immutable(inheritedTypes))
        }

        private fun semantic(type: CfirExportTypeElement): CfirExportTypeUse.Semantic =
            semanticUses.getOrPut(type) { CfirExportTypeUse.Semantic(type) }

        /** 解析包裹不是新的原 AST Type；原型只提供同一节点的语法角色。 */
        private fun syntaxPayload(ref: CfirTypeRef): CfirExportSourcePayload.TypeReference {
            var current = ref
            val seen = Collections.newSetFromMap(IdentityHashMap<CfirTypeRef, Boolean>())
            while (true) {
                check(seen.add(current)) { "A captured type prototype has a cycle" }
                val payload = input.element(current).payload as CfirExportSourcePayload.TypeReference
                if (payload !is CfirExportSourcePayload.ResolvedType || payload.prototype == null) return payload
                current = payload.prototype
            }
        }

        private fun actualTypeNode(ref: CfirTypeRef?): CfirTypeRef? = ref?.takeIf {
            (input.element(it).payload as CfirExportSourcePayload.TypeReference).origin != CfirTypeRefOrigin.SEMANTIC
        }

        private fun selectReference(ref: CfirTypeRef): CfirExportTypeUse {
            references[ref]?.let { return it }
            check(selecting.add(ref)) { "A source type syntax graph has a cycle" }
            val element = input.element(ref)
            val type = input.type(checkNotNull(element.resolvedType) { "A type use requires its completed resolved type" },
                element.context.moduleData.session)
            val origin = (element.payload as CfirExportSourcePayload.TypeReference).origin
            val result = when {
                type.containsError -> CfirExportTypeUse.Invalid
                // COMPILER_ADD 当前节点停止递归；SEMANTIC 没有原 Type 节点可供裁决。
                origin != CfirTypeRefOrigin.SOURCE -> semantic(type)
                else -> selectSourceType(ref, type)
            }
            selecting.remove(ref)
            references[ref] = result
            return result
        }

        private fun selectSourceType(ref: CfirTypeRef, type: CfirExportTypeElement): CfirExportTypeUse {
            val alias = when {
                type.abbreviatedType != null -> input.type(type.abbreviatedType, type.domain)
                type.payload is CfirExportTypePayload.Alias -> type
                else -> null
            }
            if (alias != null) {
                val declaration = checkNotNull(alias.declaration) { "An alias type requires its actual declaration binding" }
                // EXTERNAL linkage 不构成资格。public 与本代 IMPLICIT_USED 是唯一 native 判据。
                return if (analysis.isExported(declaration) || analysis.precedingFactsOf(declaration).isImplicitlyUsed) semantic(alias)
                else semantic(input.type(type.expandedType, type.domain))
            }
            return when (val syntax = syntaxPayload(ref)) {
                is CfirExportSourcePayload.ParenthesizedType -> selectReference(syntax.inner)
                is CfirExportSourcePayload.UserType -> {
                    // 多限定段对应 native QualifiedType；native switch 不递归其 arguments。
                    if (syntax.qualifiers.size != 1) return semantic(type)
                    if ((type.payload as? CfirExportTypePayload.Nominal)?.isThisType == true) return semantic(type)
                    val qualifier = input.element(syntax.qualifiers.single()).payload as CfirExportSourcePayload.TypeQualifier
                    if (qualifier.origin != CfirTypeRefOrigin.SOURCE) return semantic(type)
                    if ((type.payload as? CfirExportTypePayload.Function)?.isC == true) {
                        check(qualifier.arguments.size == 1) { "CFunc requires one function type argument" }
                        val argument = qualifier.arguments.single()
                        val selected = selectReference(argument)
                        when (selected) {
                            is CfirExportTypeUse.Function -> selected.copy(type = type, isC = true)
                            // 当前节点停止的产品原样保留，不在Semantic之后再追原型/重建子用途。
                            is CfirExportTypeUse.Semantic -> selected
                            CfirExportTypeUse.Invalid -> selected
                            is CfirExportTypeUse.Arguments -> error("CFunc's source argument is not a selected FuncType")
                        }
                    } else CfirExportTypeUse.Arguments(type, immutable(qualifier.arguments.map(::selectReference)))
                }
                is CfirExportSourcePayload.FunctionType -> CfirExportTypeUse.Function(type,
                    immutable(syntax.parameters.map(::selectReference)), selectReference(syntax.returnType),
                    (type.payload as CfirExportTypePayload.Function).isC)
                is CfirExportSourcePayload.TupleType -> CfirExportTypeUse.Arguments(type, immutable(syntax.elements.map(::selectReference)))
                is CfirExportSourcePayload.VArrayType -> CfirExportTypeUse.Arguments(type, listOf(selectReference(syntax.element)))
                // Option、Basic 和没有原型的已知类型不在 native 的递归选择分支中。
                is CfirExportSourcePayload.OptionType, is CfirExportSourcePayload.BasicType,
                is CfirExportSourcePayload.ResolvedType, is CfirExportSourcePayload.ImplicitType -> semantic(type)
            }
        }

        private fun selectDeclaration(declaration: CfirDeclaration): CfirExportTypeUse? {
            if (declarations.containsKey(declaration)) return declarations[declaration]
            val data = input.declaration(declaration)
            val type = data.type?.let { input.type(it, declaration.moduleData.session) }
            val result = if (type == null) null else if (type.containsError) CfirExportTypeUse.Invalid
            else if (!input.isSourceDeclaration(declaration) || analysis.precedingFactsOf(declaration).isCompilerAdded) semantic(type) else when (declaration) {
                is CfirFunction, is CfirEnumConstructor -> {
                    val function = type.payload as? CfirExportTypePayload.Function
                    if (function == null) semantic(type) else CfirExportTypeUse.Function(type,
                        immutable(data.valueParameters.map { checkNotNull(selectDeclaration(it)) { "A function parameter has no completed type" } }),
                        actualTypeNode(data.headerTypeRef)?.let(::selectReference) ?: semantic(input.type(function.returnType, type.domain)), function.isC)
                }
                is CfirVariable, is CfirValueParameter, is CfirPatternVariable, is CfirPatternBindingVariable,
                is CfirExtend -> actualTypeNode(data.headerTypeRef)?.let(::selectReference) ?: semantic(type)
                // TypeAliasDecl.self 使用自己的 Type Ty；Alias RHS 在独立表中选择。
                else -> semantic(type)
            }
            declarations[declaration] = result
            return result
        }

        private fun <T> immutable(values: Collection<T>): List<T> = Collections.unmodifiableList(values.toList())
        private fun <K : Any, V> immutable(values: IdentityHashMap<K, V>): Map<K, V> =
            Collections.unmodifiableMap(IdentityHashMap(values))
    }
}
