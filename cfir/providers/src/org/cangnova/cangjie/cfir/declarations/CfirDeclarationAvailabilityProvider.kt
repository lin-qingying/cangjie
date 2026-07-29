package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.nameConflictsTracker
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.unwrapForDeclarationMetadataLookup
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * OHOS 平台注解的稳定身份。
 *
 * 平台语义只能在完整 [ClassId] 相等时生效，不能因为其他包存在同名注解而误触发。
 */
object CfirPlatformAnnotationClassIds {
    private val labelsPackage = FqName("ohos.labels")

    /** `ohos.labels.Hide`。 */
    val HIDE: ClassId = ClassId(labelsPackage, Name.identifier("Hide"))

    /** `ohos.labels.APILevel`；Syscap 是该注解的参数，不是独立注解。 */
    val API_LEVEL: ClassId = ClassId(labelsPackage, Name.identifier("APILevel"))
}

/**
 * 声明自身的 Hide 状态。
 *
 * [Absent] 与 [Present] 且 `isChecked == false` 在 extend/override 规则中含义不同，
 * 因而不能压缩为单个 Boolean。
 */
sealed interface CfirHideAnnotationState {
    /** 声明没有显式 Hide。 */
    data object Absent : CfirHideAnnotationState

    /** 声明显式携带 Hide；无参数按官方语义等价于 `isChecked: false`。 */
    data class Present(
        val annotation: CfirAnnotationCall,
        val isChecked: Boolean,
    ) : CfirHideAnnotationState
}

/** `APILevel` 中影响引用可用性的结构化参数。 */
data class CfirApiLevelAnnotationInfo(
    val annotation: CfirAnnotationCall,
    val since: String?,
    val syscap: String?,
)

/** 平台 Hide 导致的引用不可用原因。 */
data class CfirHideUnavailability(
    /** 实际拥有生效 Hide 的 outer 或 target 声明。 */
    val owner: CfirDeclaration,
    /** 触发不可用性的 Hide 注解。 */
    val annotation: CfirAnnotationCall,
)

/**
 * Session 级声明可用性服务。
 *
 * 该服务统一负责平台注解身份、outer/target 顺序和包关系。名称解析与冲突检测
 * 必须保留完整候选；只有已解析目标的后置 checker 消费这里的可用性结果。
 */
open class CfirDeclarationAvailabilityProvider(
    protected val session: CfirSession,
) : CfirSessionComponent {
    /** 返回声明上完整身份匹配 [classId] 的全部注解调用。 */
    open fun findAnnotations(
        declaration: CfirDeclaration,
        classId: ClassId,
    ): List<CfirAnnotationCall> = declaration.annotations.mapNotNull { annotation ->
        val call = annotation as? CfirAnnotationCall ?: return@mapNotNull null
        call.takeIf { annotationClassId(it) == classId }
    }

    /** 返回声明自身的 Hide 三态。 */
    open fun ownHideState(declaration: CfirDeclaration): CfirHideAnnotationState {
        val annotation = findAnnotations(declaration, CfirPlatformAnnotationClassIds.HIDE).firstOrNull()
            ?: return CfirHideAnnotationState.Absent
        return CfirHideAnnotationState.Present(
            annotation = annotation,
            isChecked = annotation.booleanArgument("isChecked") ?: false,
        )
    }

    /** 返回声明自身第一个 APILevel 注解的引用相关参数。 */
    open fun ownApiLevelInfo(declaration: CfirDeclaration): CfirApiLevelAnnotationInfo? {
        val annotation = findAnnotations(declaration, CfirPlatformAnnotationClassIds.API_LEVEL).firstOrNull()
            ?: return null
        return CfirApiLevelAnnotationInfo(
            annotation = annotation,
            since = annotation.argumentLiteralText("since", positionalIndex = 0),
            syscap = annotation.argumentLiteralText("syscap"),
        )
    }

    /**
     * 返回官方引用检查顺序中的声明链：outer 在前，实际 target 在后。
     *
     * class-like 没有额外 outer；callable 的 outer 为真实 containing extend，若不是 extend
     * 成员则为 nominal containing class。属性访问器先归一化到属性本身。
     */
    open fun referenceAvailabilityChain(symbol: CfirBasedSymbol<*>): List<CfirDeclaration> {
        val normalized = normalizeSymbol(symbol)
        val target = normalized.cfir
        if (normalized !is CfirCallableSymbol<*>) return listOf(target)

        val outer = outerDeclaration(normalized)

        return if (outer == null || outer === target) listOf(target) else listOf(outer, target)
    }

    /**
     * 返回 override/extend 继承语义使用的 effective Hide。
     *
     * 与引用短路顺序不同：函数或成员自身 Hide 优先，只有自身没有显式 Hide 时才继承 outer。
     */
    open fun effectiveHideStateForOverride(symbol: CfirBasedSymbol<*>): CfirHideAnnotationState {
        val normalized = normalizeSymbol(symbol)
        ownHideState(normalized.cfir).let { own ->
            if (own is CfirHideAnnotationState.Present) return own
        }
        val callable = normalized as? CfirCallableSymbol<*> ?: return CfirHideAnnotationState.Absent
        val outer = outerDeclaration(callable) ?: return CfirHideAnnotationState.Absent
        return ownHideState(outer)
    }

    /** 返回 callable 的真实 outer：extend 优先，否则为 nominal class。 */
    open fun outerDeclaration(symbol: CfirCallableSymbol<*>): CfirDeclaration? {
        val normalized = normalizeSymbol(symbol) as? CfirCallableSymbol<*> ?: return null
        return containingExtend(normalized)
            ?: normalized.getContainingClass()?.cfir
            ?: normalized.callableId.classId
                ?.let(normalized.cfir.moduleData.session.symbolProvider::getClassLikeSymbolByClassId)
                ?.cfir
    }

    /**
     * 返回同一 [ClassId] 下的全部已知 class-like 候选。
     *
     * symbol provider 负责跨来源聚合，name-conflicts tracker 补齐单个 source provider 中被首项索引
     * 遮住的重声明；选择可用候选前不得退化为 `getClassLikeSymbolByClassId` 首命中。
     */
    open fun classLikeCandidates(classId: ClassId): List<CfirClassLikeSymbol<*>> = buildList {
        addAll(session.symbolProvider.getClassLikeSymbolsByClassId(classId))
        session.nameConflictsTracker
            ?.getClassifierRedeclarations(classId)
            .orEmpty()
            .mapTo(this) { it.classifierSymbol }
    }.distinct()

    /**
     * 判断 [symbol] 在 [useSitePackage] 是否因 `Hide(isChecked: true)` 不可引用。
     *
     * 官方规则按 outer→target 短路，并以包首段判定是否跨模块；无参和 false 均可引用。
     */
    open fun hideUnavailabilityAt(
        symbol: CfirBasedSymbol<*>,
        useSitePackage: FqName,
    ): CfirHideUnavailability? {
        for (declaration in referenceAvailabilityChain(symbol)) {
            hideUnavailabilityOf(declaration, useSitePackage)?.let { return it }
        }
        return null
    }

    /**
     * 只检查单个 outer 或 target 声明的 Hide 可用性。
     *
     * 引用 checker 用该入口按官方 `Hide → APILevel → Syscap` 顺序逐层短路，
     * 避免先扫完 target Hide 再回头检查 outer APILevel。
     */
    open fun hideUnavailabilityOf(
        declaration: CfirDeclaration,
        useSitePackage: FqName,
    ): CfirHideUnavailability? {
        val hide = ownHideState(declaration) as? CfirHideAnnotationState.Present ?: return null
        if (!hide.isChecked) return null
        val declarationPackage = checkNotNull(declarationPackage(declaration)) {
            "Hide availability owner `${declaration.symbol}` has no package identity"
        }
        if (isSameTopLevelPackageModule(useSitePackage, declarationPackage)) return null
        return CfirHideUnavailability(declaration, hide.annotation)
    }

    /** 判断声明在给定使用点是否可用。 */
    open fun isAvailableAt(symbol: CfirBasedSymbol<*>, useSitePackage: FqName): Boolean =
        hideUnavailabilityAt(symbol, useSitePackage) == null

    /** 返回声明所在的真实包。 */
    open fun declarationPackage(declaration: CfirDeclaration): FqName? {
        if (declaration is CfirExtend) return extendPackage(declaration)

        return when (val symbol = normalizeSymbol(declaration.symbol)) {
            is CfirClassLikeSymbol<*> -> symbol.classId.packageFqName
            is CfirCallableSymbol<*> -> {
                containingExtend(symbol)
                    ?.let(::extendPackage)
                    ?: symbol.callableId.packageName.takeUnless { symbol.callableId.isLocal }
                    ?: symbol.getContainingFile()?.packageDirective?.packageFqName
            }
            else -> symbol.getContainingFile()?.packageDirective?.packageFqName
        }
    }

    /** 从已解析 typeRef 或 resolved callee symbol 取得注解完整身份。 */
    protected open fun annotationClassId(annotation: CfirAnnotation): ClassId? {
        val typeClassId = (annotation.typeRef as? CfirResolvedTypeRef)
            ?.coneType
            ?.classIdOrPrimitiveClassId
        if (typeClassId != null) return typeClassId

        val resolvedSymbol = (annotation as? CfirAnnotationCall)
            ?.calleeReference
            ?.let { it as? CfirResolvedNamedReference }
            ?.resolvedSymbol
        return when (resolvedSymbol) {
            is CfirClassLikeSymbol<*> -> resolvedSymbol.classId
            is CfirCallableSymbol<*> -> resolvedSymbol.callableId.classId
            else -> null
        }
    }

    /** 复用 provider 公共入口，保证所有声明元数据查询采用同一个最终声明身份。 */
    private fun normalizeSymbol(symbol: CfirBasedSymbol<*>): CfirBasedSymbol<*> =
        symbol.unwrapForDeclarationMetadataLookup()

    /** 返回 callable 所属的真实 extend。 */
    private fun containingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? {
        val normalized = symbol.unwrapForDeclarationMetadataLookup() as CfirCallableSymbol<*>
        return normalized.getContainingExtend()
    }

    /** 从 extend 声明自身所属 session 查询真实包，避免使用点 session 丢失 owner 索引。 */
    private fun extendPackage(extend: CfirExtend): FqName? =
        extend.moduleData.session.extendProvider.getPackageFqName(extend)

    /** Hide 的跨包判定只比较包首段；两个 root 包视为同一模块。 */
    private fun isSameTopLevelPackageModule(first: FqName, second: FqName): Boolean {
        if (first.isRoot || second.isRoot) return first.isRoot && second.isRoot
        return first.pathSegments().firstOrNull() == second.pathSegments().firstOrNull()
    }
}

/** 当前 session 的统一声明可用性服务。 */
val CfirSession.declarationAvailabilityProvider: CfirDeclarationAvailabilityProvider by
    CfirSession.sessionComponentAccessor()

/** 读取注解调用中指定命名参数或位置参数的结构化表达式。 */
private fun CfirAnnotationCall.argumentExpression(
    name: String,
    positionalIndex: Int? = null,
): CfirExpression? {
    explicitArguments()
        .filterIsInstance<CfirNamedArgumentExpression>()
        .firstOrNull { it.argumentName.asString() == name }
        ?.let { return it.expression }

    for ((argument, parameter) in (argumentList as? CfirResolvedArgumentList)?.mapping.orEmpty()) {
        if (parameter.name.asString() == name) return argument.unwrapNamedArgument()
    }

    return positionalIndex
        ?.let(explicitArguments()::getOrNull)
        ?.unwrapNamedArgument()
}

/** 读取注解字面量参数文本；不从 source 文本猜测。 */
private fun CfirAnnotationCall.argumentLiteralText(
    name: String,
    positionalIndex: Int? = null,
): String? = (argumentExpression(name, positionalIndex) as? CfirLiteralExpression)
    ?.value
    ?.toString()

/** 读取布尔注解参数；非结构化布尔字面量返回 null。 */
private fun CfirAnnotationCall.booleanArgument(name: String): Boolean? =
    (argumentExpression(name, positionalIndex = 0) as? CfirLiteralExpression)?.value as? Boolean

/** 去掉命名参数包装。 */
private tailrec fun CfirExpression.unwrapNamedArgument(): CfirExpression = when (this) {
    is CfirNamedArgumentExpression -> expression.unwrapNamedArgument()
    else -> this
}

/**
 * 返回源码显式实参，而不是只返回已成功映射到形参的 resolved argument。
 *
 * 平台可用性语义消费 annotation 源码参数；当 annotation constructor resolve
 * 失败或部分失败时，参数仍然必须参与 APILevel/Hide 判断。
 */
private fun CfirAnnotationCall.explicitArguments(): List<CfirExpression> =
    (argumentList as? CfirResolvedArgumentList)
        ?.originalArgumentList
        ?.arguments
        ?: argumentList.arguments
