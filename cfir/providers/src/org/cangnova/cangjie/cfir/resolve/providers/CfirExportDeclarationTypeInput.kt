package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.explicitUpperBounds
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId

/**
 * 最后一次 header/scope 生产结束后的声明类型构造输入。
 * 保留真实 ref、参数和 Cone 身份及其顺序；校验只读取字段，不重新构造类型或请求解析。
 */
internal class CfirExportDeclarationTypeInput private constructor(
    private val symbol: CfirBasedSymbol<*>,
    private val parameters: List<CfirValueParameter>,
    private val typeParameters: List<CfirTypeParameterRef>,
    private val references: List<CfirTypeRef>,
    private val types: List<ConeCangJieType?>,
    private val isCFunction: Boolean,
    private val hasVariableLenArg: Boolean,
    private val hasUnexportedSignature: Boolean,
    private val classId: ClassId?,
    private val builtinKind: CfirBuiltInTypeKind?,
    private val parameterOwner: CfirBasedSymbol<*>?,
) {
    fun validate(declaration: CfirDeclaration) {
        check(isCurrent(declaration)) {
            "Export declaration type input changed after final preparation: ${declaration.symbol}"
        }
    }

    fun isCurrent(declaration: CfirDeclaration): Boolean {
        val current = capture(declaration)
        return symbol === current.symbol && sameIdentities(parameters, current.parameters) &&
            sameIdentities(typeParameters, current.typeParameters) &&
            sameIdentities(references, current.references) &&
            types.size == current.types.size && types.indices.all { types[it] === current.types[it] } &&
            isCFunction == current.isCFunction && hasVariableLenArg == current.hasVariableLenArg &&
            hasUnexportedSignature == current.hasUnexportedSignature && classId == current.classId &&
            builtinKind == current.builtinKind && parameterOwner === current.parameterOwner
    }

    companion object {
        fun capture(declaration: CfirDeclaration): CfirExportDeclarationTypeInput {
            val parameters = when (declaration) {
                is CfirFunction -> declaration.valueParameters
                is CfirEnumConstructor -> declaration.valueParameters
                else -> emptyList()
            }.toList()
            val typeParameters = (declaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty().toList()
            val references = buildList {
                when (declaration) {
                    is CfirCallableDeclaration -> add(declaration.returnTypeRef)
                    is CfirTypeAlias -> add(declaration.expandedTypeRef)
                    is CfirExtend -> add(declaration.extendedTypeRef)
                    else -> Unit
                }
                parameters.forEach { add(it.returnTypeRef) }
                when (declaration) {
                    is CfirClassLikeDeclaration -> addAll(declaration.superTypeRefs)
                    is CfirExtend -> addAll(declaration.superTypeRefs)
                    is CfirTypeParameter -> {
                        addAll(declaration.bounds)
                        addAll(declaration.explicitUpperBounds)
                    }
                    else -> Unit
                }
            }
            return CfirExportDeclarationTypeInput(declaration.symbol, parameters, typeParameters, references,
                references.map { it.coneTypeOrNull }, declaration.interopInfo?.resolvedAbi?.isCFunction == true,
                (declaration as? CfirFunction)?.hasVariableLenArg == true, declaration.exportFacts.hasUnexportedSignature,
                (declaration as? CfirClassLikeDeclaration)?.symbol?.classId,
                (declaration as? CfirBuiltInDeclaration)?.kind,
                (declaration as? CfirTypeParameter)?.containingDeclarationSymbol)
        }

        private fun <T : Any> sameIdentities(first: List<T>, second: List<T>): Boolean =
            first.size == second.size && first.indices.all { first[it] === second[it] }
    }
}
