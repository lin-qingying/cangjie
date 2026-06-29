

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.containingDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.getNonLocalContainingOrThisDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.isAutonomousElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirFileBuilder
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirProvider
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 基于 [cfirFileBuilder] 和 [provider] 查找当前 PSI 声明对应的源码非局部 CFIR 声明。
 */
internal fun CjDeclaration.findSourceNonLocalCfirDeclaration(
    cfirFileBuilder: LLCfirFileBuilder,
    provider: CfirProvider,
): CfirDeclaration = findSourceNonLocalCfirDeclaration(
    cfirFileBuilder.buildRawCfirFileWithCaching(containingCjFile),
    provider,
)

/**
 * 查找当前非局部 PSI 声明对应的源码 CFIR 声明。
 *
 * “Non-local” 表示非局部 class、函数、属性等声明。
 */
internal fun CjDeclaration.findSourceNonLocalCfirDeclaration(cfirFile: CfirFile, provider: CfirProvider): CfirDeclaration {
    // TODO test what way faster
    if (isPhysical) {
        // do not request providers with non-physical psi in order not to leak them there and
        // to avoid inconsistency between physical psi and its copy during completion
        findSourceNonLocalCfirDeclarationByProvider(
            firDeclarationProvider = { declaration ->
                if (declaration is CjExtend) {
                    CfirElementFinder.findDeclaration(cfirFile, declaration)
                } else if (declaration is CjClassLikeDeclaration) {
                    declaration.findCfir(provider)
                } else {
                    val containingTypeStatement = declaration.containingTypeStatement
                    val declarations = when (containingTypeStatement) {
                        is CjExtend -> {
                            val containerExtendCfir = CfirElementFinder.findDeclaration(cfirFile, containingTypeStatement) as? CfirExtend
                            containerExtendCfir?.declarations
                        }
                        is CjClassLikeDeclaration -> {
                            val containerClassLikeCfir = containingTypeStatement.findCfir(provider) as? CfirClassLikeDeclaration
                            containerClassLikeCfir?.declarations
                        }
                        null -> cfirFile.declarations
                        else -> null
                    }

                    // It is possible that we will not be able to find the needed declaration here when the code is invalid
                    // e.g., we have two conflicting declarations with the same name,
                    // and we are searching for the wrong one
                    declarations?.find { it.psi == declaration }
                }
            },
        )?.let { return it }
    }

    findSourceNonLocalCfirDeclarationByProvider(
        firDeclarationProvider = { declaration ->
            CfirElementFinder.findDeclaration(cfirFile, declaration)
        },
    )?.let { return it }

    errorWithCfirSpecificEntries(
        "No cfir element was found for ${this::class.simpleName}",
        psi = this,
        cfir = cfirFile,
        additionalInfos = { withEntry("isPhysical", isPhysical.toString()) }
    )
}

/**
 * 收集 [element] 使用点所在的 CFIR 容器路径。
 */
@CaImplementationDetail
fun collectUseSiteContainers(element: PsiElement, resolutionFacade: LLResolutionFacade): List<CfirDeclaration>? {
    val containingDeclaration = element.getNonLocalContainingOrThisDeclaration { it.isAutonomousElement } ?: return null
    val containingFile = containingDeclaration.containingCjFile
    val cfirFile = resolutionFacade.getOrBuildCfirFile(containingFile)
    return CfirElementFinder.findPathToDeclarationWithTarget(cfirFile, containingDeclaration)
}

/**
 * 通过遍历整棵 CFIR 树查找当前 PSI 元素对应的源码 CFIR 声明。
 */
internal fun CjElement.findSourceByTraversingWholeTree(
    cfirFileBuilder: LLCfirFileBuilder,
    containerCfirFile: CfirFile?,
): CfirDeclaration? {
    val cfirFile = containerCfirFile ?: cfirFileBuilder.buildRawCfirFileWithCaching(containingCjFile)
    val originalDeclaration = (this as? CjDeclaration)?.originalDeclaration
    val isDeclaration = this is CjDeclaration
    return CfirElementFinder.findElementIn(
        cfirFile,
        canGoInside = { it is CfirClassLikeDeclaration || it is CfirFunction || it is CfirProperty },
        predicate = { cfirDeclaration ->
            cfirDeclaration.psi == this || isDeclaration && cfirDeclaration.psi == originalDeclaration
        }
    )
}

/**
 * 使用 [firDeclarationProvider] 查找当前 PSI 声明对应的非局部 CFIR 声明。
 */
private fun CjDeclaration.findSourceNonLocalCfirDeclarationByProvider(
    firDeclarationProvider: (CjDeclaration) -> CfirDeclaration?,
): CfirDeclaration? {
    val candidate = when (this) {
        is CjTypeStatement,
        is CjPatternVariable,
        is CjFieldVariable,
        is CjProperty,
        is CjNamedFunction,
        is CjMainFunction,
        is CjMacroDeclaration,
        is CjFinalizer,
        is CjConstructor<*>,
        is CjTypeAlias,
            -> firDeclarationProvider(this)

        is CjPropertyAccessor -> {
            val cfirPropertyDeclaration = property.findSourceNonLocalCfirDeclarationByProvider(
                firDeclarationProvider,
            ) as? CfirProperty ?: return null

            if (isGetter) {
                cfirPropertyDeclaration.getter
            } else {
                cfirPropertyDeclaration.setter
            }
        }

        is CjParameter -> {
            val ownerDeclaration = ownerFunction ?: errorWithCfirSpecificEntries(
                "Containing declaration should be not null for ${CjParameter::class.simpleName}",
                psi = this,
            )

            val cfirDeclaration = ownerDeclaration.findSourceNonLocalCfirDeclarationByProvider(
                firDeclarationProvider,
            ) ?: return null

            val parameters = (cfirDeclaration as? CfirFunction)?.valueParameters

            parameters?.firstOrNull { it.psi == this }
        }

        is CjTypeParameter -> {
            val declaration = containingDeclaration ?: errorWithCfirSpecificEntries(
                "Containing declaration should be not null for CjTypeParameter",
                psi = this,
            )

            val cfirTypeParameterOwner = declaration.findSourceNonLocalCfirDeclarationByProvider(
                firDeclarationProvider,
            ) as? CfirTypeParameterRefsOwner ?: return null

            cfirTypeParameterOwner.typeParameters.firstOrNull { it.psi == this } as CfirDeclaration
        }

        else -> errorWithCfirSpecificEntries("Invalid container", psi = this)
    }

    return candidate?.takeIf { it.psi == this }
}

/**
 * 保存复制 PSI 对应原始声明的 user data key。
 */
val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<CjDeclaration>("ORIGINAL_DECLARATION_KEY")
/**
 * 复制 PSI 声明对应的原始声明。
 */
var CjDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)

/**
 * 保存复制仓颉文件对应原始文件的 user data key。
 */
private val ORIGINAL_CJ_FILE_KEY = com.intellij.openapi.util.Key<CjFile>("ORIGINAL_CJ_FILE_KEY")
/**
 * 复制仓颉文件对应的原始文件。
 */
var CjFile.originalCjFile by UserDataProperty(ORIGINAL_CJ_FILE_KEY)


/**
 * 通过 [provider] 查找 class-like PSI 对应的 CFIR class-like 声明。
 */
private fun CjClassLikeDeclaration.findCfir(provider: CfirProvider): CfirClassLikeDeclaration? {
    return if (provider is LLCfirProvider) {
        provider.getCfirClassifierByDeclaration(this)
    } else {
        val classId = getClassId() ?: return null
        provider.getCfirClassifierByFqName(classId)
    }
}

/**
 * 返回文件中唯一的 code fragment 声明。
 */
@LLCfirInternals
val CfirFile.codeFragment: CfirCodeFragment
    get() {
        return declarations.singleOrNull() as? CfirCodeFragment
            ?: errorWithCfirSpecificEntries("Code fragment not found in a CfirFile", cfir = this)
    }

/**
 * 判断 CFIR 声明是否是生成声明。
 */
val CfirDeclaration.isGeneratedDeclaration
    get() = realPsi == null

/**
 * 遍历 class-like 声明的直接子声明。
 */
internal inline fun CfirClassLikeDeclaration.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    declarations.forEach(action)
}

/**
 * 遍历 extend 声明的直接子声明。
 */
internal inline fun CfirExtend.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    declarations.forEach(action)
}

/**
 * 遍历文件的直接子声明。
 */
internal inline fun CfirFile.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    declarations.forEach(action)
}

/**
 * 判断声明是否能作为直接声明容器。
 */
internal val CfirDeclaration.isDeclarationContainer: Boolean
    get() = this is CfirClassLikeDeclaration || this is CfirExtend || this is CfirFile

/**
 * 根据声明容器类型遍历其直接子声明。
 */
internal inline fun CfirDeclaration.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    when (this) {
        is CfirClassLikeDeclaration -> forEachDeclaration(action)
        is CfirExtend -> forEachDeclaration(action)
        is CfirFile -> forEachDeclaration(action)
        else -> errorWithCfirSpecificEntries("Unsupported declarations container", cfir = this)
    }
}

/**
 * 判断非局部声明类型是否支持局部 body 分析。
 *
 * 该属性只检查声明类型，不检查 body 是否存在或语句数量是否足以局部分析。
 */
internal val CfirElementWithResolveState.isPartialBodyResolvable: Boolean
    get() = when (this) {
        is CfirConstructor -> !isPrimary
        is CfirNamedFunction -> true
        else -> false
    }

/**
 * 判断声明 body block 是否支持局部分析。
 *
 * 空 block 和单语句 block 不支持局部分析。
 */
internal val CfirBlock.isPartialAnalyzable: Boolean
    get() = statements.size > 1

/**
 * 返回声明 body block。
 */
internal val CfirElementWithResolveState.body: CfirBlock?
    get() = when (this) {
        is CfirFunction -> body
        else -> null
    }

/**
 * 判断 callable 从懒解析视角是否属于局部声明。
 */
internal val CfirCallableSymbol<*>.isLocalForLazyResolutionPurposes: Boolean
    get() = when (cfir.origin) {
        else -> cfir.isLocal
    }

/**
 * 返回 code fragment 感知的 PSI 父链，包含当前元素。
 */
val PsiElement.parentsWithSelfCodeFragmentAware: Sequence<PsiElement>
    get() = generateSequence(this) { element ->
        when (element) {
            is CjCodeFragment -> element.context
            is PsiFile -> null
            else -> element.parent
        }
    }

/**
 * 返回 code fragment 感知的 PSI 父链，不包含当前元素。
 */
val PsiElement.parentsCodeFragmentAware: Sequence<PsiElement>
    get() = parentsWithSelfCodeFragmentAware.drop(1)

/**
 * 将复制 PSI 元素映射回原始文件中的对应元素。
 */
internal fun <T : PsiElement> T.unwrapCopy(containingFile: PsiFile = this.containingFile): T? {
    val originalFile = (containingFile as? CjFile)?.originalCjFile
        ?: containingFile.originalFile.takeUnless { it == containingFile }
        ?: (containingFile as? CjFile)?.elementContext?.containingFile
        ?: return null

    return try {
        PsiTreeUtil.findSameElementInCopy(this, originalFile)
    } catch (_: IllegalStateException) {
        // File copy has a different file structure
        null
    }
}

/**
 * 在 [session] 中查找 `String.plus` 函数符号。
 */
fun findStringPlusSymbol(session: CfirSession): CfirNamedFunctionSymbol? {
    val stringClassId = ClassId.topLevel(StandardNames.FqNames.stringFqName)
    return session.symbolProvider.getClassLikeSymbolByClassId(stringClassId)?.cfir?.declarations?.singleOrNull {
        it is CfirNamedFunction && it.name == OperatorNameConventions.PLUS
    }?.symbol as? CfirNamedFunctionSymbol
}
