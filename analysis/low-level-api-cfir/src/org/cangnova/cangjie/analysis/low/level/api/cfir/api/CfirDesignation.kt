/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirDanglingFileSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirLibraryOrLibrarySourceResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.*
import org.cangnova.cangjie.analysis.utils.errors.unexpectedElementError
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.resolve.toClassSymbol
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.SmartList
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * This class describes where locates [target] element and its essential [path].
 *
 * Usually a resolver uses [path] to resolve [target] in the proper context.
 *
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
 */
class CfirDesignation(
    /**
     * The path to [target] element.
     *
     * ### Contracts:
     * * Can contain [CfirFile] only in the first position
     * @see file
     * @see fileOrNull
     */
    val path: List<CfirDeclaration>,
    val target: CfirElementWithResolveState,
) {
    constructor(target: CfirElementWithResolveState) : this(emptyList(), target)

    init {
        for ((index, declaration) in path.withIndex()) {
            when (declaration) {
                is CfirFile -> requireWithAttachment(
                    index == 0,
                    { "${CfirFile::class.simpleName} can be only in the first position of the path, but actual is '$index'" },
                ) {
                    withCfirDesignationEntry("designation", this@CfirDesignation)
                }

                is CfirClass -> {}
                else -> errorWithAttachment("Unexpected declaration type: ${declaration::class.simpleName}") {
                    withCfirDesignationEntry("designation", this@CfirDesignation)
                }
            }
        }
    }

    val file: CfirFile
        get() = fileOrNull ?: errorWithAttachment("File is not found") {
            withCfirDesignationEntry("designation", this@CfirDesignation)
        }

    val fileOrNull: CfirFile? get() = path.firstOrNull() as? CfirFile ?: target as? CfirFile

    override fun toString(): String = path.plus(target).joinToString(separator = " -> ") {
        it::class.simpleName ?: it.toString()
    }
}

fun ExceptionAttachmentBuilder.withCfirDesignationEntry(name: String, designation: CfirDesignation) {
    withEntryGroup(name) {
        for ((index, declaration) in designation.path.withIndex()) {
            withCfirEntry("path$index", declaration)
        }

        withCfirEntry("target", designation.target)
    }
}

fun CfirDesignation.toSequence(includeTarget: Boolean): Sequence<CfirElementWithResolveState> = sequence {
    yieldAll(path)
    if (includeTarget) yield(target)
}

private fun tryCollectDesignation(providedFile: CfirFile?, target: CfirElementWithResolveState): CfirDesignation? {
    if (target !is CfirDeclaration) {
        unexpectedElementError<CfirDeclaration>(target)
    }

    return when (target) {
        is CfirAnonymousFunction,
        is CfirErrorFunction,
        is CfirTypeParameter,
        is CfirValueParameter,
        is CfirPatternVariable,
        is CfirPatternBindingVariable,
            -> null

        is CfirNamedFunction,
        is CfirMainFunction,
        is CfirMacroDeclaration,
        is CfirFinalizer,
        is CfirProperty,
        is CfirFieldVariable,
        is CfirConstructor,
        is CfirEnumConstructor,
            -> {
            if (target.symbol.isLocalForLazyResolutionPurposes) {
                return null
            }

            val containingClassId = target.containingClassLookupTag()?.toClassSymbol(target.moduleData.session)?.classId
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is CfirClassLikeDeclaration -> {
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId = null)
        }

        is CfirFile -> CfirDesignation(target)
        else -> null
    }
}

private fun collectDesignationPathWithContainingClass(
    providedFile: CfirFile?,
    target: CfirDeclaration,
    containingClassId: ClassId?,
): CfirDesignation? {
    val file = providedFile ?: target.getContainingFile()
    if (file != null && (containingClassId == null || file.packageDirective.packageFqName == containingClassId.packageFqName)) {
        val designationPath = CfirElementFinder.collectDesignationPath(
            firFile = file,
            declarationContainerClassId = containingClassId,
            targetMemberDeclaration = target,
        )

        if (designationPath != null) {
            return designationPath
        }
    }

    val fallbackClassPath = containingClassId?.let { collectDesignationPathWithContainingClassFallback(target, it) }.orEmpty()
    val fallbackFile = providedFile ?: fallbackClassPath.lastOrNull()?.getContainingFile() ?: file
    val fallbackPath = listOfNotNull(fallbackFile) + fallbackClassPath
    val patchedPath = patchDesignationPathIfNeeded(target, fallbackPath)
    return CfirDesignation(patchedPath, target)
}

/**
 * Whether the search via [CfirSymbolProvider] is required to find a declaration in the context of [this] session.
 *
 * Not all sessions have required providers in the session itself (not its dependencies).
 * In such cases, the search might not be able to find even the containing declaration
 */
private val LLCfirSession.requiresDependenciesSearch: Boolean
    get() = when (this) {
        is LLCfirLibraryOrLibrarySourceResolvableModuleSession -> true
        is LLCfirDanglingFileSession -> {
            val module = ktModule as CaDanglingFileModule
            // Dangling files in the ignore self mode have the empty declaration provider,
            // so they cannot find any declarations inside themselves. Search in the context is required
            module.resolutionMode == CaDanglingFileResolutionMode.IGNORE_SELF
        }

        else -> false
    }

private fun collectDesignationPathWithContainingClassFallback(
    target: CfirDeclaration,
    containingClassId: ClassId,
): List<CfirDeclaration> {
    val useSiteSession by lazy(LazyThreadSafetyMode.NONE) { getTargetSession(target) }

    fun resolveChunk(classId: ClassId): CfirClass {
        val declaration = if (useSiteSession.requiresDependenciesSearch) {
            useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        } else {
            useSiteSession.cfirProvider.getCfirClassifierByFqName(classId)
                ?: findKotlinStdlibClass(classId, target)
        }

        checkWithAttachment(
            declaration is CfirClass,
            {
                "'${CfirClass::class.simpleName}' expected as a containing declaration, " +
                        "got '${declaration?.javaClass?.simpleName}'. " +
                        "Module: ${useSiteSession.ktModule::class.simpleName}"
            },
        ) {
                withEntry("chunk", "$classId in $containingClassId")
                withCfirEntry("target", target)
                if (declaration != null) {
                    withCfirEntry("foundDeclaration", declaration)
                }
        }

        return declaration
    }

    val containingClassIds = sequenceOf(containingClassId)
    val (_, containingClasses) = containingClassIds.fold(target to SmartList<CfirClass>()) { (declaration, result), classId ->
        // Psi-based calculator is called explicitly to avoid `LLCfirProvider#getContainingClassSymbol`
        // since we have a fallback logic with strict checking (no dependencies in the search scope)
        val psiBasedContainingClass = LLContainingClassCalculator.getContainingClassSymbol(declaration.symbol)?.cfir
        checkWithAttachment(
            psiBasedContainingClass == null || psiBasedContainingClass is CfirClass,
            {
                "${LLContainingClassCalculator::class.simpleName} is supposed to return '${CfirClass::class.simpleName}' " +
                        "as a containing declaration since the class is not local (classId exists), got '${psiBasedContainingClass?.let { it::class.java.simpleName }}'. " +
                        "Module: ${useSiteSession.ktModule::class.simpleName}"
            },
        ) {
            withEntry("classId", classId.toString())
            withEntry("containingClassId", containingClassId.toString())
            withCfirEntry("declaration", declaration)
        }

        if (psiBasedContainingClass == null && classId.shortClassName.isSpecial) {
            errorWithAttachment(
                "Special classes are supposed to be covered via ${LLContainingClassCalculator::class.simpleName}. " +
                        "Module: ${useSiteSession.ktModule::class.simpleName}"
            ) {
                withEntry("classId", classId.toString())
                withEntry("containingClassId", containingClassId.toString())
                withCfirEntry("declaration", declaration)
            }
        }

        val containingClass = psiBasedContainingClass ?: resolveChunk(classId)
        result += containingClass
        containingClass to result
    }

    return containingClasses.asReversed()
}

private fun getTargetSession(target: CfirDeclaration): LLCfirSession {
    if (target is CfirCallableDeclaration) {
        val containingSymbol = target.getContainingClassSymbol()
        if (containingSymbol != null) {
            // Synthetic declarations might have a call site session
            return containingSymbol.llCfirSession
        }
    }

    return target.llCfirSession
}

private fun findKotlinStdlibClass(classId: ClassId, target: CfirDeclaration): CfirClass? {
    if (!classId.packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) {
        return null
    }

    val firFile = target.getContainingFile() ?: return null
    return CfirElementFinder.findClassifierWithClassId(firFile, classId) as? CfirClass
}

/**
 * Consider using this function only if [collectDesignation] is not applicable.
 *
 * This extension function can be used in the case there your [CfirElementWithResolveState] probably
 * doesn't have [getContainingFile] and it doesn't matter for your purposes.
 * Potentially, this function can become obsolete if we support all possible cases in [getContainingFile]
 *
 * @return [CfirDesignation] where [CfirDesignation.fileOrNull] can be null or throws an exception.
 *
 * @see collectDesignation
 * @see tryCollectDesignation
 * @see tryCollectDesignationWithOptionalFile
 */
fun CfirElementWithResolveState.collectDesignationWithOptionalFile(providedFile: CfirFile? = null): CfirDesignation =
    tryCollectDesignationWithOptionalFile(providedFile) ?: errorWithAttachment("No designation of local declaration") {
        providedFile?.let { withCfirEntry("firFile", it) }
    }

/**
 * @return [CfirDesignation] where [CfirDesignation.fileOrNull] not null or throws an exception.
 *
 * @see collectDesignationWithOptionalFile
 * @see tryCollectDesignation
 * @see tryCollectDesignationWithOptionalFile
 */
fun CfirElementWithResolveState.collectDesignation(providedFile: CfirFile? = null): CfirDesignation =
    tryCollectDesignation(providedFile) ?: errorWithAttachment("No designation of local declaration") {
        withCfirEntry("CfirDeclaration", this@collectDesignation)
    }

/**
 * Consider using this function only if [tryCollectDesignation] is not applicable.
 *
 * This extension function can be used in the case there your [CfirElementWithResolveState] probably
 * doesn't have [getContainingFile] and it doesn't matter for your purposes.
 * Potentially, this function can become obsolete if we support all possible cases in [getContainingFile]
 *
 * @return [CfirDesignation] where [CfirDesignation.fileOrNull] can be null or null.
 *
 * @see collectDesignationWithOptionalFile
 * @see collectDesignation
 * @see tryCollectDesignation
 */
fun CfirElementWithResolveState.tryCollectDesignationWithOptionalFile(providedFile: CfirFile? = null): CfirDesignation? =
    tryCollectDesignation(providedFile = providedFile, target = this)

/**
 * @return [CfirDesignation] with not-null [CfirDesignation.file] or null.
 *
 * @see collectDesignation
 * @see tryCollectDesignationWithOptionalFile
 * @see collectDesignationWithOptionalFile
 */
fun CfirElementWithResolveState.tryCollectDesignation(providedFile: CfirFile? = null): CfirDesignation? {
    val designation = tryCollectDesignation(providedFile = providedFile, target = this)
    return designation?.takeIf { it.fileOrNull != null }
}

internal fun patchDesignationPathIfNeeded(target: CfirElementWithResolveState, targetPath: List<CfirDeclaration>): List<CfirDeclaration> {
    return patchDesignationPathForCopy(target, targetPath) ?: targetPath
}

private fun patchDesignationPathForCopy(target: CfirElementWithResolveState, targetPath: List<CfirDeclaration>): List<CfirDeclaration>? {
    val targetModule = target.llCfirModuleData.ktModule

    if (targetModule is CaDanglingFileModule && targetModule.resolutionMode == CaDanglingFileResolutionMode.IGNORE_SELF) {
        val targetPsiFile = targetModule.files.singleOrNull() ?: return targetPath
        val contextModule = targetModule.contextModule
        val contextResolutionFacade = contextModule.getResolutionFacade(contextModule.project)

        return buildList {
            for (targetPathDeclaration in targetPath) {
                val targetPathPsi = targetPathDeclaration.psi ?: return null
                if (targetPathPsi !is CjTypeStatement && targetPathPsi !is CjFile) return null

                val originalPathPsi = targetPathPsi.unwrapCopy(targetPsiFile) ?: return null
                val originalPathDeclaration = when (originalPathPsi) {
                    is CjTypeStatement -> originalPathPsi.resolveToCfirSymbolOfTypeSafe<CfirClassSymbol>(contextResolutionFacade)?.cfir
                    is CjFile -> originalPathPsi.getOrBuildCfirFile(contextResolutionFacade)
                    else -> null
                } ?: return null

                add(originalPathDeclaration)
            }
        }
    }

    return targetPath
}
