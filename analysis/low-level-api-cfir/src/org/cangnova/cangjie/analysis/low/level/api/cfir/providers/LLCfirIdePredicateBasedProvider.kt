@file:OptIn(CaPlatformInterface::class)

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.getContainingFile
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolver
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.cfirCachesFactory
import org.cangnova.cangjie.cfir.caches.createCache
import org.cangnova.cangjie.cfir.caches.getValue
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.extensions.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.extensions.predicate.AbstractPredicate
import org.cangnova.cangjie.cfir.extensions.predicate.DeclarationPredicate
import org.cangnova.cangjie.cfir.extensions.predicate.LookupPredicate
import org.cangnova.cangjie.cfir.extensions.predicate.PredicateVisitor
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*

/**
 * PSI index based implementation of [CfirPredicateBasedProvider].
 */
internal class LLCfirIdePredicateBasedProvider(
    private val session: LLCfirSession,
    private val annotationsResolver: CangJieAnnotationsResolver,
) : CfirPredicateBasedProvider() {
    private val projectStructureProvider by lazy { CangJieProjectStructureProvider.getInstance(session.project) }

    private val registeredPluginAnnotations: CfirRegisteredPluginAnnotations
        get() = session.registeredPluginAnnotations

    private val declarationOwnersCache: CfirCache<CfirFile, CfirOwnersStorage, Nothing?> =
        session.cfirCachesFactory.createCache { cfirFile -> CfirOwnersStorage.collectOwners(cfirFile) }

    override fun getSymbolsByPredicate(predicate: LookupPredicate): List<CfirBasedSymbol<*>> {
        val annotations = predicate.annotations
        val annotatedDeclarations = annotations
            .asSequence()
            .flatMap { annotationsResolver.declarationsByAnnotation(ClassId.topLevel(it)) }
            .toSet()

        return annotatedDeclarations
            .asSequence()
            .mapNotNull { it.findCfirDeclarationForLookupPredicate() }
            .filter { matches(predicate, it) }
            .map { it.symbol }
            .toList()
    }

    private fun CjElement.findCfirDeclarationForLookupPredicate(): CfirDeclaration? {
        if (this !is CjDeclaration) return null

        if (this !is CjClassLikeDeclaration &&
            this !is CjNamedFunction &&
            this !is CjConstructor<*> &&
            this !is CjProperty
        ) return null

        // LookupPredicates should never match local declarations, so we filter them early
        if (CjPsiUtil.isLocal(this)) return null

        val moduleForFile = projectStructureProvider.getModule(this, session.caModule)
        val resolutionFacadeForFile = moduleForFile.getResolutionFacade(project)
        return this.resolveToCfirSymbol(resolutionFacadeForFile).cfir
    }

    override fun getOwnersOfDeclaration(declaration: CfirDeclaration): List<CfirBasedSymbol<*>>? {
        val cfirFile = declaration.getContainingFile() ?: return null
        val declarationOwners = declarationOwnersCache.getValue(cfirFile)

        return declarationOwners.getOwners(declaration)
    }

    override fun fileHasPluginAnnotations(file: CfirFile): Boolean {
        val targetCjFile = file.psi as? CjFile ?: return false
        val pluginAnnotations = registeredPluginAnnotations.annotations

        return pluginAnnotations.any {
            val annotationId = ClassId.topLevel(it)
            val markedDeclarations = annotationsResolver.declarationsByAnnotation(annotationId)

            markedDeclarations.any { declaration ->
                declaration == targetCjFile || declaration.containingFile == targetCjFile
            }
        }
    }

    override fun matches(predicate: AbstractPredicate<*>, declaration: CfirDeclaration): Boolean {
        return when (predicate) {
            is DeclarationPredicate -> predicate.accept(declarationPredicateMatcher, declaration)
            is LookupPredicate -> predicate.accept(lookupPredicateMatcher, declaration)
        }
    }

    private val declarationPredicateMatcher = Matcher<DeclarationPredicate>()
    private val lookupPredicateMatcher = Matcher<LookupPredicate>()

    private inner class Matcher<P : AbstractPredicate<P>> : PredicateVisitor<P, Boolean, CfirDeclaration>() {
        override fun visitPredicate(predicate: AbstractPredicate<P>, data: CfirDeclaration): Boolean {
            error(
                "When overrides for all possible DeclarationPredicate subtypes are implemented, " +
                        "this method should never be called, but it was called with $predicate"
            )
        }

        override fun visitAnd(predicate: AbstractPredicate.And<P>, data: CfirDeclaration): Boolean {
            return predicate.a.accept(this, data) && predicate.b.accept(this, data)
        }

        override fun visitOr(predicate: AbstractPredicate.Or<P>, data: CfirDeclaration): Boolean {
            return predicate.a.accept(this, data) || predicate.b.accept(this, data)
        }

        override fun visitAnnotatedWith(predicate: AbstractPredicate.AnnotatedWith<P>, data: CfirDeclaration): Boolean {
            return annotationsOnDeclaration(data).any { it in predicate.annotations }
        }

        override fun visitAncestorAnnotatedWith(predicate: AbstractPredicate.AncestorAnnotatedWith<P>, data: CfirDeclaration): Boolean {
            return annotationsOnOuterDeclarations(data).any { it in predicate.annotations }
        }

        override fun visitMetaAnnotatedWith(predicate: AbstractPredicate.MetaAnnotatedWith<P>, data: CfirDeclaration): Boolean {
            return data.annotations.any { annotation ->
                annotation.markedWithMetaAnnotation(session, data, predicate.metaAnnotations, predicate.includeItself)
            }
        }

        override fun visitParentAnnotatedWith(predicate: AbstractPredicate.ParentAnnotatedWith<P>, data: CfirDeclaration): Boolean {
            val parent = data.directParentDeclaration ?: return false
            val parentPredicate = DeclarationPredicate.AnnotatedWith(predicate.annotations)

            return parentPredicate.accept(declarationPredicateMatcher, parent)
        }

        override fun visitHasAnnotatedWith(predicate: AbstractPredicate.HasAnnotatedWith<P>, data: CfirDeclaration): Boolean {
            val childPredicate = DeclarationPredicate.AnnotatedWith(predicate.annotations)

            return data.anyDirectChildDeclarationMatches(childPredicate)
        }

        private val CfirDeclaration.directParentDeclaration: CfirDeclaration?
            get() = getOwnersOfDeclaration(this)?.lastOrNull()?.cfir
    }

    private fun CfirDeclaration.anyDirectChildDeclarationMatches(childPredicate: DeclarationPredicate): Boolean {
        var result = false

        this.forEachDirectChildDeclaration {
            result = result || childPredicate.accept(declarationPredicateMatcher, it)
        }

        return result
    }

    private fun annotationsOnDeclaration(declaration: CfirDeclaration): Set<AnnotationFqn> {
        if (declaration.annotations.isEmpty()) return emptySet()

        val cfirResolvedAnnotations = declaration.annotations
            .asSequence()
            .mapNotNull { it.typeRef as? CfirResolvedTypeRef }
            .mapNotNull { it.coneType.classId }
            .map { it.asSingleFqName() }
            .toSet()

        if (cfirResolvedAnnotations.isNotEmpty()) return cfirResolvedAnnotations

        val psiDeclaration = declaration.psi as? CjAnnotated ?: return emptySet()
        val psiAnnotations = annotationsResolver.annotationsOnDeclaration(psiDeclaration)

        return psiAnnotations.map { it.asSingleFqName() }.toSet()
    }

    private fun annotationsOnOuterDeclarations(declaration: CfirDeclaration): Set<AnnotationFqn> {
        return getOwnersOfDeclaration(declaration)?.flatMap { annotationsOnDeclaration(it.cfir) }.orEmpty().toSet()
    }
}

private fun CfirAnnotation.markedWithMetaAnnotation(
    session: LLCfirSession,
    containingDeclaration: CfirDeclaration,
    metaAnnotations: Set<AnnotationFqn>,
    includeItself: Boolean,
): Boolean {
    containingDeclaration.symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
    val annotationType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return false
    val annotationSymbol = annotationType.toSymbol(session) as? CfirClassSymbol ?: return false
    return annotationSymbol.markedWithMetaAnnotation(session, metaAnnotations, includeItself, mutableSetOf())
}

private fun CfirClassSymbol.markedWithMetaAnnotation(
    session: LLCfirSession,
    metaAnnotations: Set<AnnotationFqn>,
    includeItself: Boolean,
    visited: MutableSet<CfirClassSymbol>,
): Boolean {
    if (!visited.add(this)) return false

    val annotationFqName = classId.asSingleFqName()
    if (annotationFqName in metaAnnotations) return includeItself

    lazyResolveToPhase(CfirResolvePhase.TYPES)
    return cfir.annotations.any { annotation ->
        val nestedAnnotationType = (annotation.typeRef as? CfirResolvedTypeRef)?.coneType ?: return@any false
        val nestedAnnotationSymbol = nestedAnnotationType.toSymbol(session) as? CfirClassSymbol ?: return@any false
        nestedAnnotationSymbol.markedWithMetaAnnotation(session, metaAnnotations, includeItself = true, visited)
    }
}

private class CfirOwnersStorage(private val declarationToOwner: Map<CfirDeclaration, List<CfirBasedSymbol<*>>>) {
    fun getOwners(declaration: CfirDeclaration): List<CfirBasedSymbol<*>>? = declarationToOwner[declaration]

    companion object {
        fun collectOwners(file: CfirFile): CfirOwnersStorage {
            val declarationToOwners = hashMapOf<CfirDeclaration, List<CfirBasedSymbol<*>>>()
            val psiToCfir = hashMapOf<CjElement, CfirDeclaration>()

            file.forEachElementWithContainers { element, owners ->
                if (element !is CfirDeclaration) return@forEachElementWithContainers

                declarationToOwners[element] = owners

                val psiDeclaration = element.psi
                if (psiDeclaration is CjElement) {
                    // FIXME we actually have a problem with CjFakeSourceElement sources
                    psiToCfir.putIfAbsent(psiDeclaration, element)
                }
            }

            return CfirOwnersStorage(declarationToOwners)
        }
    }
}

/**
 * Walks over every [CfirElement] in [this] file and invokes [saveDeclaration] on it, passing each element and the list of its containing
 * declarations (like file, classes, functions/properties and so on).
 */
private inline fun CfirFile.forEachElementWithContainers(
    crossinline saveDeclaration: (element: CfirElement, owners: List<CfirBasedSymbol<*>>) -> Unit
) {
    val declarationsCollector = object : CfirVisitor<Unit, PersistentList<CfirBasedSymbol<*>>>() {
        override fun visitElement(element: CfirElement, data: PersistentList<CfirBasedSymbol<*>>) {
            if (element is CfirDeclaration) {
                saveDeclaration(element, data)
            }

            element.acceptChildren(
                visitor = this,
                data = if (element is CfirDeclaration) data.add(element.symbol) else data
            )
        }
    }

    accept(declarationsCollector, persistentListOf())
}

/**
 * Calls [action] on every direct child declaration of [this] declaration.
 */
private inline fun CfirDeclaration.forEachDirectChildDeclaration(crossinline action: (child: CfirDeclaration) -> Unit) {
    this.acceptChildren(object : CfirDefaultVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            // we must visit only direct children
        }

        override fun visitFile(file: CfirFile) {
            action(file)
        }

        override fun visitCallableDeclaration(callableDeclaration: CfirCallableDeclaration) {
            action(callableDeclaration)
        }

        override fun visitClassLikeDeclaration(classLikeDeclaration: CfirClassLikeDeclaration) {
            action(classLikeDeclaration)
        }
    })
}
