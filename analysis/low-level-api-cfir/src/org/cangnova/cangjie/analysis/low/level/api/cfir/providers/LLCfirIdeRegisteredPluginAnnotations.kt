@file:OptIn(CaPlatformInterface::class)

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolver
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.caches.*
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.extensions.AbstractCfirRegisteredPluginAnnotations
import org.cangnova.cangjie.cfir.extensions.AnnotationFqn
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjBuiltInAnnotation
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile

/**
 * IDE low-level CFIR session 中的插件注解注册表实现。
 *
 * 它合并编译器插件直接声明的注解和通过 meta-annotation 在 PSI 索引中发现的用户注解。
 */
internal class LLCfirIdeRegisteredPluginAnnotations(
    session: CfirSession,

    /**
     * 用于按注解 class id 查找 PSI 声明的 IDE 注解解析器。
     */
    private val annotationsResolver: CangJieAnnotationsResolver
) : AbstractCfirRegisteredPluginAnnotations(session) {

    /**
     * 插件直接注册的注解全限定名集合。
     */
    private val annotationsFromPlugins: MutableSet<AnnotationFqn> = mutableSetOf()

    /**
     * 当前 session 可见的全部插件相关注解。
     */
    override val annotations: Set<AnnotationFqn>
        get() = allAnnotationsCache.getValue()

    /**
     * 合并 meta-annotation 派生注解和插件直接注册注解的惰性缓存。
     */
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
    /**
     * meta-annotation 到实际注解集合的缓存。
     */
    private val annotationsWithMetaAnnotationCache: CfirCache<AnnotationFqn, Set<AnnotationFqn>, Nothing?> =
        session.cfirCachesFactory.createCache { metaAnnotation -> collectAnnotationsWithMetaAnnotation(metaAnnotation) }

    /**
     * 返回被 [metaAnnotation] 标记的注解集合。
     */
    override fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn> {
        return annotationsWithMetaAnnotationCache.getValue(metaAnnotation)
    }

    /**
     * 通过 PSI 注解索引查找所有被 [metaAnnotation] 标记的顶层注解类。
     */
    private fun collectAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Set<FqName> {
        val annotatedDeclarations = annotationsResolver.declarationsByAnnotation(ClassId.topLevel(metaAnnotation))

        return annotatedDeclarations
            .asSequence()
            .filterIsInstance<CjClassLikeDeclaration>()
            .filter { declaration ->
                declaration.parent is CjFile &&
                    declaration.annotationEntries.any { annotation ->
                        annotation.builtInAnnotation == CjBuiltInAnnotation.ANNOTATION
                    }
            }
            .mapNotNull { it.getClassId()?.asSingleFqName() }
            .toSet()
    }

    /**
     * 保存插件显式声明的注解全限定名。
     */
    override fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>) {
        annotationsFromPlugins += annotations
    }

    /**
     * IDE 模式不通过 CFIR class 注册用户注解；用户注解由 PSI 索引和 meta-annotation 查询发现。
     */
    override fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<CfirClass>) {
        error("This method should never be called in IDE mode")
    }
}
