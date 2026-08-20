package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.visibility.CfirVisibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.lookupOriginForAccessibility
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

/**
 * 判断成员声明对当前调用候选是否可见。
 */
fun isVisible(
    visibilityChecker: CfirVisibilityChecker,
    declaration: CfirMemberDeclaration,
    candidate: Candidate,
): Boolean = candidate.accessibilityResult(visibilityChecker, declaration) is CfirAccessibilityResult.Accessible

/**
 * 将 call resolver 的结构化信息适配为 providers 层唯一的可见性服务输入。
 * 这里不得复制 private/internal/protected 规则。
 */
fun Candidate.accessibilityResult(
    visibilityChecker: CfirVisibilityChecker,
    declaration: CfirMemberDeclaration,
): CfirAccessibilityResult {
    /*
     * synthetic typealias constructor 没有注册在 provider 文件索引中；其可见性 owner、
     * 声明文件和 nominal owner 都来自展开类型的原始构造器。若继续用 synthetic symbol，
     * internal/package 可见构造器会因 containing file 缺失被错误隐藏。
     */
    val visibilityOwner = (declaration as? CfirConstructor)
        ?.typeAliasConstructorInfo
        ?.originalConstructor
        ?: declaration
    val visibilityOwnerSymbol = checkNotNull(visibilityOwner.symbol as? CfirCallableSymbol<*>) {
        "Call candidate visibility owner must be callable: $visibilityOwner"
    }
    val accessKind = when (callInfo.callKind) {
        org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind.NamedValueAccess -> CfirAccessKind.NAMED_VALUE
        else -> CfirAccessKind.CALLABLE
    }
    val result = discoveryAccessibilityResult
        ?: callInfo.session.accessibilityChecker.checkCallable(
            symbol = visibilityOwnerSymbol,
            context = CfirAccessContext(
                useSiteFile = callInfo.containingFile,
                containingDeclarations = callInfo.containingDeclarations,
                receiverType = callInfo.explicitReceiver?.resolvedType,
                lookupOrigin = originScope?.lookupOriginForAccessibility() ?: CfirLookupOrigin.LEXICAL,
                kind = accessKind,
            ),
            provenance = lookupProvenance,
        )
    if (result !is CfirAccessibilityResult.Accessible) return result

    return if (visibilityChecker.platformVisibilityCheck(
            visibilityOwner.status.visibility,
            visibilityOwner,
            this,
        )
    ) {
        CfirAccessibilityResult.Accessible
    } else {
        CfirAccessibilityResult.Inaccessible(
            reportingOwner = visibilityOwner.symbol,
            disposition = org.cangnova.cangjie.cfir.resolve.providers.CfirLookupDisposition.REPORT_ACCESS_ERROR,
        )
    }
}
