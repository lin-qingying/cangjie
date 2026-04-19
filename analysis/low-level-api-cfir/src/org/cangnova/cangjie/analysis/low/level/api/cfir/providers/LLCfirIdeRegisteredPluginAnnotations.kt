/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolver
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.caches.*
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.extensions.AbstractCfirRegisteredPluginAnnotations
import org.cangnova.cangjie.cfir.extensions.AnnotationFqn
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjClass

internal class LLCfirIdeRegisteredPluginAnnotations(
    session: CfirSession,
    private val annotationsResolver: CangJieAnnotationsResolver
) : AbstractCfirRegisteredPluginAnnotations(session) {

    private val annotationsFromPlugins: MutableSet<AnnotationFqn> = mutableSetOf()

    override val annotations: Set<AnnotationFqn>
        get() = allAnnotationsCache.getValue()

    private val allAnnotationsCache: CfirLazyValue<Set<AnnotationFqn>> = session.cfirCachesFactory.createLazyValue {
        // at this point, both metaAnnotations and annotationsFromPlugins should be collected
        val result = metaAnnotations.flatMapTo(mutableSetOf()) { getAnnotationsWithMetaAnnotation(it) }

        if (result.isEmpty()) {
            annotationsFromPlugins
        } else {
            result += annotationsFromPlugins
            result
        }
    }

    // MetaAnnotation -> Annotations
    private val annotationsWithMetaAnnotationCache: CfirCache<AnnotationFqn, Set<AnnotationFqn>, Nothing?> =
        session.cfirCachesFactory.createCache { metaAnnotation -> collectAnnotationsWithMetaAnnotation(metaAnnotation) }

    override fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn> {
        return annotationsWithMetaAnnotationCache.getValue(metaAnnotation)
    }

    private fun collectAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Set<FqName> {
        val annotatedDeclarations = annotationsResolver.declarationsByAnnotation(ClassId.topLevel(metaAnnotation))

        return annotatedDeclarations
            .asSequence()
            .filterIsInstance<CjClass>()
            .filter { it.isAnnotation() && it.isTopLevel() }
            .mapNotNull { it.fqName }
            .toSet()
    }

    override fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>) {
        annotationsFromPlugins += annotations
    }

    override fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<CfirClass>) {
        error("This method should never be called in IDE mode")
    }
}
