package org.cangnova.cangjie.cfir.extensions

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.cangnova.cangjie.cfir.NoMutableState
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.cfirCachesFactory
import org.cangnova.cangjie.cfir.caches.createCache
import org.cangnova.cangjie.cfir.caches.getValue
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.extensions.predicate.AbstractPredicate
import org.cangnova.cangjie.cfir.extensions.predicate.DeclarationPredicate
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.utils.shouldNotBeCalled

/**
 * `CfirRegisteredPluginAnnotations` 对位 Kotlin `FirRegisteredPluginAnnotations`。
 */
abstract class CfirRegisteredPluginAnnotations : CfirSessionComponent {
    abstract val annotations: Set<AnnotationFqn>

    abstract val metaAnnotations: Set<AnnotationFqn>

    val hasRegisteredAnnotations: Boolean
        get() = annotations.isNotEmpty() || metaAnnotations.isNotEmpty()

    abstract fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn>

    abstract fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<CfirClass>)

    abstract fun getAnnotationsForPredicate(predicate: DeclarationPredicate): Set<AnnotationFqn>

    @PluginServicesInitialization
    abstract fun initialize()

    object Empty : CfirRegisteredPluginAnnotations() {
        override val annotations: Set<AnnotationFqn>
            get() = emptySet()
        override val metaAnnotations: Set<AnnotationFqn>
            get() = emptySet()

        override fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn> = emptyList()

        override fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<CfirClass>) {
            shouldNotBeCalled()
        }

        override fun getAnnotationsForPredicate(predicate: DeclarationPredicate): Set<AnnotationFqn> = emptySet()

        @PluginServicesInitialization
        override fun initialize() {
            shouldNotBeCalled()
        }
    }
}

abstract class AbstractCfirRegisteredPluginAnnotations(
    protected val session: CfirSession,
) : CfirRegisteredPluginAnnotations() {
    final override val metaAnnotations: MutableSet<AnnotationFqn> = mutableSetOf()

    private val annotationsForPredicateCache: CfirCache<DeclarationPredicate, Set<AnnotationFqn>, Nothing?> =
        session.cfirCachesFactory.createCache { predicate ->
            collectAnnotations(predicate)
        }

    final override fun getAnnotationsForPredicate(predicate: DeclarationPredicate): Set<AnnotationFqn> {
        return annotationsForPredicateCache.getValue(predicate)
    }

    private fun collectAnnotations(predicate: DeclarationPredicate): Set<AnnotationFqn> {
        val result = predicate.metaAnnotations.flatMapTo(mutableSetOf()) { getAnnotationsWithMetaAnnotation(it) }
        if (result.isEmpty()) return predicate.annotations
        result += predicate.annotations
        return result
    }

    @PluginServicesInitialization
    final override fun initialize() {
        val registrar = object : CfirDeclarationPredicateRegistrar() {
            val predicates = mutableListOf<AbstractPredicate<*>>()

            override fun register(vararg predicates: AbstractPredicate<*>) {
                this.predicates += predicates
            }

            override fun register(predicates: Collection<AbstractPredicate<*>>) {
                this.predicates += predicates
            }
        }

        for (extension in session.extensionService.getAllExtensions()) {
            with(extension) {
                registrar.registerPredicates()
            }
        }

        for (predicate in registrar.predicates) {
            saveAnnotationsFromPlugin(predicate.annotations)
            metaAnnotations += predicate.metaAnnotations
        }
    }

    protected abstract fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>)
}

@NoMutableState
class CfirRegisteredPluginAnnotationsImpl(session: CfirSession) : AbstractCfirRegisteredPluginAnnotations(session) {
    override val annotations: MutableSet<AnnotationFqn> = mutableSetOf()

    private val userDefinedAnnotations: Multimap<AnnotationFqn, AnnotationFqn> = LinkedHashMultimap.create()

    override fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn> {
        return userDefinedAnnotations[metaAnnotation]
    }

    override fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>) {
        this.annotations += annotations
    }

    override fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<CfirClass>) {
        val annotations = annotationClasses.map { it.symbol.classId.asSingleFqName() }
        this.annotations += annotations
        userDefinedAnnotations.putAll(metaAnnotation, annotations)
    }
}

val CfirSession.registeredPluginAnnotations: CfirRegisteredPluginAnnotations by CfirSession.sessionComponentAccessor()
