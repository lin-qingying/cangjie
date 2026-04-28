/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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

internal fun CjDeclaration.findSourceNonLocalCfirDeclaration(
    cfirFileBuilder: LLCfirFileBuilder,
    provider: CfirProvider,
): CfirDeclaration = findSourceNonLocalCfirDeclaration(
    cfirFileBuilder.buildRawCfirFileWithCaching(containingCjFile),
    provider,
)

/**
 * 'Non-local' stands for not local classes/functions/etc.
 */
internal fun CjDeclaration.findSourceNonLocalCfirDeclaration(cfirFile: CfirFile, provider: CfirProvider): CfirDeclaration {
    // TODO test what way faster
    if (isPhysical) {
        // do not request providers with non-physical psi in order not to leak them there and
        // to avoid inconsistency between physical psi and its copy during completion
        findSourceNonLocalCfirDeclarationByProvider(
            firDeclarationProvider = { declaration ->
                if (declaration is CjClassLikeDeclaration) {
                    declaration.findCfir(provider)
                } else {
                    val containingTypeStatement = declaration.containingTypeStatement
                    val declarations = if (containingTypeStatement != null) {
                        val containerClassLikeCfir = containingTypeStatement.findCfir(provider) as? CfirClassLikeDeclaration
                        containerClassLikeCfir?.declarations
                    } else {
                        cfirFile.declarations
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

@CaImplementationDetail
fun collectUseSiteContainers(element: PsiElement, resolutionFacade: LLResolutionFacade): List<CfirDeclaration>? {
    val containingDeclaration = element.getNonLocalContainingOrThisDeclaration { it.isAutonomousElement } ?: return null
    val containingFile = containingDeclaration.containingCjFile
    val cfirFile = resolutionFacade.getOrBuildCfirFile(containingFile)
    return CfirElementFinder.findPathToDeclarationWithTarget(cfirFile, containingDeclaration)
}

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

private fun CjDeclaration.findSourceNonLocalCfirDeclarationByProvider(
    firDeclarationProvider: (CjDeclaration) -> CfirDeclaration?,
): CfirDeclaration? {
    val candidate = when (this) {
        is CjTypeStatement,
        is CjProperty,
        is CjNamedFunction,
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

val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<CjDeclaration>("ORIGINAL_DECLARATION_KEY")
var CjDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)

private val ORIGINAL_CJ_FILE_KEY = com.intellij.openapi.util.Key<CjFile>("ORIGINAL_CJ_FILE_KEY")
var CjFile.originalCjFile by UserDataProperty(ORIGINAL_CJ_FILE_KEY)


private fun CjClassLikeDeclaration.findCfir(provider: CfirProvider): CfirClassLikeDeclaration? {
    return if (provider is LLCfirProvider) {
        provider.getCfirClassifierByDeclaration(this)
    } else {
        val classId = getClassId() ?: return null
        provider.getCfirClassifierByFqName(classId)
    }
}

@LLCfirInternals
val CfirFile.codeFragment: CfirCodeFragment
    get() {
        return declarations.singleOrNull() as? CfirCodeFragment
            ?: errorWithCfirSpecificEntries("Code fragment not found in a CfirFile", cfir = this)
    }

val CfirDeclaration.isGeneratedDeclaration
    get() = realPsi == null

internal inline fun CfirClassLikeDeclaration.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    declarations.forEach(action)
}

internal inline fun CfirExtend.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    declarations.forEach(action)
}

internal inline fun CfirFile.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    declarations.forEach(action)
}

internal val CfirDeclaration.isDeclarationContainer: Boolean
    get() = this is CfirClassLikeDeclaration || this is CfirExtend || this is CfirFile

internal inline fun CfirDeclaration.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    when (this) {
        is CfirClassLikeDeclaration -> forEachDeclaration(action)
        is CfirExtend -> forEachDeclaration(action)
        is CfirFile -> forEachDeclaration(action)
        else -> errorWithCfirSpecificEntries("Unsupported declarations container", cfir = this)
    }
}

/**
 * Whether a non-local declaration of the given type supports partial body analysis.
 *
 * The function only checks the declaration type.
 * It does not perform other important checks such as a number of body statements, or even whether the body is present at all.
 */
internal val CfirElementWithResolveState.isPartialBodyResolvable: Boolean
    get() = when (this) {
        is CfirConstructor -> !isPrimary
        is CfirNamedFunction -> true
        else -> false
    }

/**
 * Whether a declaration body block supports partial body analysis.
 * For empty blocks and blocks with a single statement, partial analysis is unavailable.
 */
internal val CfirBlock.isPartialAnalyzable: Boolean
    get() = statements.size > 1

/**
 * A declaration body (a block with statements).
 */
internal val CfirElementWithResolveState.body: CfirBlock?
    get() = when (this) {
        is CfirFunction -> body
        else -> null
    }

/**
 * Some "local" declarations are not local from the lazy resolution perspective.
 */
internal val CfirCallableSymbol<*>.isLocalForLazyResolutionPurposes: Boolean
    get() = when (cfir.origin) {
        else -> cfir.isLocal
    }

val PsiElement.parentsWithSelfCodeFragmentAware: Sequence<PsiElement>
    get() = generateSequence(this) { element ->
        when (element) {
            is CjCodeFragment -> element.context
            is PsiFile -> null
            else -> element.parent
        }
    }

val PsiElement.parentsCodeFragmentAware: Sequence<PsiElement>
    get() = parentsWithSelfCodeFragmentAware.drop(1)

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

fun findStringPlusSymbol(session: CfirSession): CfirNamedFunctionSymbol? {
    val stringClassId = ClassId.topLevel(StandardNames.FqNames.stringFqName)
    return session.symbolProvider.getClassLikeSymbolByClassId(stringClassId)?.cfir?.declarations?.singleOrNull {
        it is CfirNamedFunction && it.name == OperatorNameConventions.PLUS
    }?.symbol as? CfirNamedFunctionSymbol
}
