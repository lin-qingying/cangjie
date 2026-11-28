package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpContainerKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 物理 owner 只由原声明的 provider 取得；同名类型或当前会话中的替代容器不是原始归属。 */
fun CfirCallableDeclaration.getContainingCjmpDeclaration(): CfirDeclaration? {
    val declarationSession = moduleData.session
    return declarationSession.extendProvider.getContainingExtend(symbol) ?: symbol.getContainingClassSymbol()?.cfir
}

/** 原声明位置，source 与 binary 使用各自已有的位置元数据；无源码的合成节点明确没有 begin。 */
fun CfirDeclaration.cjmpSourcePosition(): CfirCjmpSourcePosition? {
    cjoDeclarationPosition?.let { position ->
        val begin = position.declarationLocation ?: return null
        return CfirCjmpSourcePosition(begin.filePath.substringAfterLast('/').substringAfterLast('\\'), begin.line, begin.column)
    }
    val source = source ?: return null
    val file = moduleData.session.cfirProvider.getContainingFile(symbol) ?: return null
    val mapping = file.sourceFileLinesMapping ?: return null
    val (line, column) = mapping.getLineAndColumnByOffset(source.startOffset)
    return CfirCjmpSourcePosition(requireNotNull(file.sourceFile).name, line + 1, column + 1)
}

/** 冻结容器原有的物理成员；调用方应先准备此容器的 TYPES，不能从已有配对结果反推输入。 */
fun CfirDeclaration.cjmpOriginalContainerInput(): CfirCjmpContainerInput? {
    val kind = when (this) {
        is CfirClass -> CjmpContainerKind.CLASS
        is CfirStruct -> CjmpContainerKind.STRUCT
        is CfirInterface -> CjmpContainerKind.INTERFACE
        is CfirEnum -> CjmpContainerKind.ENUM
        is CfirExtend -> CjmpContainerKind.EXTEND
        else -> return null
    }
    val declaration = this as CfirMemberDeclaration
    val members = when (this) {
        is CfirClassLikeDeclaration -> declarations
        is CfirExtend -> declarations
        else -> error("A CJMP container must have a declaration body")
    }
    val typeParameters = when (this) {
        is CfirClassLikeDeclaration -> typeParameters.map { it.symbol }
        is CfirExtend -> typeParameters.map { it.symbol }
        else -> error("A CJMP container must have type parameters")
    }
    return CfirCjmpContainerInput(
        symbol = symbol,
        name = if (this is CfirClassLikeDeclaration) name.asString() else "",
        kind = kind,
        isCommon = isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON),
        isSpecific = isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC),
        hasGenericNode = hasGenericNode,
        typeParameters = typeParameters,
        isNonExhaustive = this is CfirEnum && isNonExhaustive,
        originalDoNotExport = originalDoNotExport,
        members = members.map { member ->
            val callable = member as? CfirCallableDeclaration
            CfirCjmpMemberInput(
                symbol = member.symbol,
                name = when (member) {
                    is CfirCallableDeclaration -> member.symbol.name.asString()
                    is CfirClassLikeDeclaration -> member.name.asString()
                    else -> ""
                },
                isInstanceVariable = member is CfirFieldVariable && !member.status.isStatic,
                isMemberParameter = member is CfirVariable && member.isMemberParameter,
                isCommon = member.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON),
                isSpecific = member.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC),
                hasCommonDefault = member.cjmpHasCommonDefault(),
                originalDoNotExport = member.originalDoNotExport,
                isFromCommonPart = member.isFromCommonPart,
                sourcePosition = member.cjmpSourcePosition(),
            )
        },
        sourcePosition = cjmpSourcePosition(),
        extendKey = (this as? CfirExtend)?.cjmpOriginalExtendKey(),
    )
}

