/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.copyOrigin
import org.cangnova.cangjie.analysis.api.utils.errors.withPsiEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.containingDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.getNonLocalContainingOrThisDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.isAutonomousElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirFileBuilder
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirProvider
import org.cangnova.cangjie.analysis.utils.classId
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.toRegularClassSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingClassOrObject
import org.cangnova.cangjie.util.OperatorNameConventions
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

internal fun CjDeclaration.findSourceNonLocalCfirDeclaration(
    firFileBuilder: LLCfirFileBuilder,
    provider: CfirProvider,
): CfirDeclaration = findSourceNonLocalCfirDeclaration(
    firFileBuilder.buildRawCfirFileWithCaching(containingCjFile),
    provider,
)

/**
 * 'Non-local' stands for not local classes/functions/etc.
 */
internal fun CjDeclaration.findSourceNonLocalCfirDeclaration(firFile: CfirFile, provider: CfirProvider): CfirDeclaration {
    // TODO test what way faster
    if (isPhysical) {
        // do not request providers with non-physical psi in order not to leak them there and
        // to avoid inconsistency between physical psi and its copy during completion
        findSourceNonLocalCfirDeclarationByProvider(
            firDeclarationProvider = { declaration ->
                if (declaration is CjClassLikeDeclaration) {
                    declaration.findCfir(provider)
                } else {
                    val containingClassOrObject = declaration.containingClassOrObject
                    val declarations = if (containingClassOrObject != null) {
                        val containerClassCfir = containingClassOrObject.findCfir(provider) as? CfirRegularClass
                        containerClassCfir?.declarations
                    } else {
                        firFile.declarations
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
            CfirElementFinder.findDeclaration(firFile, declaration)
        },
    )?.let { return it }

    errorWithCfirSpecificEntries(
        "No fir element was found for ${this::class.simpleName}",
        psi = this,
        fir = firFile,
        additionalInfos = { withEntry("isPhysical", isPhysical.toString()) }
    )
}

@CaImplementationDetail
fun collectUseSiteContainers(element: PsiElement, resolutionFacade: LLResolutionFacade): List<CfirDeclaration>? {
    val containingDeclaration = element.getNonLocalContainingOrThisDeclaration { it.isAutonomousElement } ?: return null
    val containingFile = containingDeclaration.containingCjFile
    val firFile = resolutionFacade.getOrBuildCfirFile(containingFile)
    return CfirElementFinder.findPathToDeclarationWithTarget(firFile, containingDeclaration)
}

internal fun CjElement.findSourceByTraversingWholeTree(
    firFileBuilder: LLCfirFileBuilder,
    containerCfirFile: CfirFile?,
): CfirDeclaration? {
    val firFile = containerCfirFile ?: firFileBuilder.buildRawCfirFileWithCaching(containingCjFile)
    val originalDeclaration = (this as? CjDeclaration)?.originalDeclaration
    val isDeclaration = this is CjDeclaration
    return CfirElementFinder.findElementIn(
        firFile,
        canGoInside = { it is CfirRegularClass || it is CfirFunction || it is CfirProperty },
        predicate = { firDeclaration ->
            firDeclaration.psi == this || isDeclaration && firDeclaration.psi == originalDeclaration
        }
    )
}

private fun CjDeclaration.findSourceNonLocalCfirDeclarationByProvider(
    firDeclarationProvider: (CjDeclaration) -> CfirDeclaration?,
): CfirDeclaration? {
    val candidate = when (this) {
        is CjClassOrObject,
        is CjProperty,
        is CjNamedFunction,
        is CjConstructor<*>,
        is CjTypeAlias,
            -> firDeclarationProvider(this)

        is CjPropertyAccessor -> {
            val firPropertyDeclaration = property.findSourceNonLocalCfirDeclarationByProvider(
                firDeclarationProvider,
            ) as? CfirVariable ?: return null

            if (isGetter) {
                firPropertyDeclaration.getter
            } else {
                firPropertyDeclaration.setter
            }
        }

        is CjParameter -> {
            val ownerDeclaration = ownerDeclaration ?: errorWithCfirSpecificEntries(
                "Containing declaration should be not null for ${CjParameter::class.simpleName}",
                psi = this,
            )

            val firDeclaration = ownerDeclaration.findSourceNonLocalCfirDeclarationByProvider(
                firDeclarationProvider,
            ) ?: return null

            val parameters = (firDeclaration as? CfirFunction)?.valueParameters

            parameters?.firstOrNull { it.psi == this }
        }

        is CjTypeParameter -> {
            val declaration = containingDeclaration ?: errorWithCfirSpecificEntries(
                "Containing declaration should be not null for CjTypeParameter",
                psi = this,
            )

            val firTypeParameterOwner = declaration.findSourceNonLocalCfirDeclarationByProvider(
                firDeclarationProvider,
            ) as? CfirTypeParameterRefsOwner ?: return null

            firTypeParameterOwner.typeParameters.firstOrNull { it.psi == this } as CfirDeclaration
        }

        else -> errorWithCfirSpecificEntries("Invalid container", psi = this)
    }

    //property accessors for properties with delegation have CjFakeSourceElementKind.DelegatedPropertyAccessor kind
    return candidate?.takeIf { it.psi == this }
}

fun CfirAnonymousInitializer.containingClassIdOrNull(): ClassId? =
    (containingDeclarationSymbol as? CfirClassSymbol<*>)?.classId

val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<CjDeclaration>("ORIGINAL_DECLARATION_KEY")
var CjDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)

private val ORIGINAL_KT_FILE_KEY = com.intellij.openapi.util.Key<CjFile>("ORIGINAL_KT_FILE_KEY")
var CjFile.originalCjFile by UserDataProperty(ORIGINAL_KT_FILE_KEY)


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
            ?: errorWithCfirSpecificEntries("Code fragment not found in a CfirFile", fir = this)
    }

val CfirDeclaration.isGeneratedDeclaration
    get() = realPsi == null

internal inline fun CfirRegularClass.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    declarations.forEach(action)
}

internal inline fun CfirFile.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    declarations.forEach(action)
}

internal val CfirDeclaration.isDeclarationContainer: Boolean get() = this is CfirRegularClass || this is CfirFile

internal inline fun CfirDeclaration.forEachDeclaration(action: (CfirDeclaration) -> Unit) {
    when (this) {
        is CfirRegularClass -> forEachDeclaration(action)
        is CfirFile -> forEachDeclaration(action)
        else -> errorWithCfirSpecificEntries("Unsupported declarations container", fir = this)
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
        is CfirNamedFunction, is CfirAnonymousInitializer -> true
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
        is CfirAnonymousInitializer -> body
        else -> null
    }

/**
 * Some "local" declarations are not local from the lazy resolution perspective.
 */
internal val CfirCallableSymbol<*>.isLocalForLazyResolutionPurposes: Boolean
    get() = when (fir.origin) {
        else -> isLocal
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
    val originalFile = containingFile.copyOrigin
        ?: (containingFile as? CjFile)?.analysisContext?.containingFile
        ?: return null

    return try {
        PsiTreeUtil.findSameElementInCopy(this, originalFile)
    } catch (_: IllegalStateException) {
        // File copy has a different file structure
        null
    }
}

fun findStringPlusSymbol(session: CfirSession): CfirNamedFunctionSymbol? {
    val stringClassSymbol = session.builtinTypes.stringType.toRegularClassSymbol(session)
    return stringClassSymbol?.fir?.declarations?.singleOrNull {
        it is CfirNamedFunction && it.name == OperatorNameConventions.PLUS
    }?.symbol as? CfirNamedFunctionSymbol
}

internal fun PsiClass.classIdOrError(): ClassId =
    classId
        ?: errorWithAttachment("No classId for non-local class") {
            withPsiEntry(
                "psiClass",
                this@classIdOrError,
                KotlinProjectStructureProvider.getModule(project, this@classIdOrError, useSiteModule = null)
            )
            withEntry("qualifiedName", qualifiedName)
        }
