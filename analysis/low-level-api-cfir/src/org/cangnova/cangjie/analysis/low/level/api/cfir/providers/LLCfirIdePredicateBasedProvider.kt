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
import org.cangnova.cangjie.cfir.declarations.CfirExtend
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
    /**
     * 当前 provider 服务的低阶 CFIR session。
     */
    private val session: LLCfirSession,

    /**
     * IDE 层注解索引解析器，用于按注解 class id 找到 PSI 声明。
     */
    private val annotationsResolver: CangJieAnnotationsResolver,
) : CfirPredicateBasedProvider() {
    /**
     * 当前工程的项目结构提供器，用于把 PSI 文件映射到正确的 analysis module。
     */
    private val projectStructureProvider by lazy { CangJieProjectStructureProvider.getInstance(session.project) }

    /**
     * session 中注册的插件注解集合。
     */
    private val registeredPluginAnnotations: CfirRegisteredPluginAnnotations
        get() = session.registeredPluginAnnotations

    /**
     * 按 CFIR 文件缓存声明到其外层 owner symbol 列表的映射。
     */
    private val declarationOwnersCache: CfirCache<CfirFile, CfirOwnersStorage, Nothing?> =
        session.cfirCachesFactory.createCache { cfirFile -> CfirOwnersStorage.collectOwners(cfirFile) }

    /**
     * 根据 [predicate] 中的注解约束从 PSI 注解索引找到候选声明，再过滤为匹配的 CFIR symbol。
     */
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

    /**
     * 将 PSI 声明解析为可用于 lookup predicate 匹配的 CFIR 声明。
     *
     * 只接受非局部 class-like、函数、构造函数和属性声明；局部声明永远不应被 lookup predicate 命中。
     */
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

    /**
     * 返回 [declaration] 的外层声明 owner symbol 列表。
     */
    override fun getOwnersOfDeclaration(declaration: CfirDeclaration): List<CfirBasedSymbol<*>>? {
        val cfirFile = declaration.getContainingFile() ?: return null
        val declarationOwners = declarationOwnersCache.getValue(cfirFile)

        return declarationOwners.getOwners(declaration)
    }

    /**
     * 判断 [file] 对应的 PSI 文件中是否存在插件注册注解。
     */
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

    /**
     * 判断 [declaration] 是否满足给定 declaration 或 lookup predicate。
     */
    override fun matches(predicate: AbstractPredicate<*>, declaration: CfirDeclaration): Boolean {
        return when (predicate) {
            is DeclarationPredicate -> predicate.accept(declarationPredicateMatcher, declaration)
            is LookupPredicate -> predicate.accept(lookupPredicateMatcher, declaration)
        }
    }

    /**
     * declaration predicate 的匹配 visitor。
     */
    private val declarationPredicateMatcher = Matcher<DeclarationPredicate>()

    /**
     * lookup predicate 的匹配 visitor。
     */
    private val lookupPredicateMatcher = Matcher<LookupPredicate>()

    /**
     * 将 CFIR predicate 树解释为布尔匹配结果的 visitor。
     */
    private inner class Matcher<P : AbstractPredicate<P>> : PredicateVisitor<P, Boolean, CfirDeclaration>() {
        /**
         * 未覆盖的 predicate 类型会走到这里，表示 matcher 与 predicate 层级已经不一致。
         */
        override fun visitPredicate(predicate: AbstractPredicate<P>, data: CfirDeclaration): Boolean {
            error(
                "When overrides for all possible DeclarationPredicate subtypes are implemented, " +
                        "this method should never be called, but it was called with $predicate"
            )
        }

        /**
         * 匹配逻辑与：左右子谓词都必须满足。
         */
        override fun visitAnd(predicate: AbstractPredicate.And<P>, data: CfirDeclaration): Boolean {
            return predicate.a.accept(this, data) && predicate.b.accept(this, data)
        }

        /**
         * 匹配逻辑或：任一子谓词满足即可。
         */
        override fun visitOr(predicate: AbstractPredicate.Or<P>, data: CfirDeclaration): Boolean {
            return predicate.a.accept(this, data) || predicate.b.accept(this, data)
        }

        /**
         * 判断当前声明是否直接带有目标注解。
         */
        override fun visitAnnotatedWith(predicate: AbstractPredicate.AnnotatedWith<P>, data: CfirDeclaration): Boolean {
            return annotationsOnDeclaration(data).any { it in predicate.annotations }
        }

        /**
         * 判断当前声明的任意外层声明是否带有目标注解。
         */
        override fun visitAncestorAnnotatedWith(predicate: AbstractPredicate.AncestorAnnotatedWith<P>, data: CfirDeclaration): Boolean {
            return annotationsOnOuterDeclarations(data).any { it in predicate.annotations }
        }

        /**
         * 判断当前声明上的注解类型是否被目标 meta-annotation 标记。
         */
        override fun visitMetaAnnotatedWith(predicate: AbstractPredicate.MetaAnnotatedWith<P>, data: CfirDeclaration): Boolean {
            return data.annotations.any { annotation ->
                annotation.markedWithMetaAnnotation(session, data, predicate.metaAnnotations, predicate.includeItself)
            }
        }

        /**
         * 判断当前声明的直接父声明是否带有目标注解。
         */
        override fun visitParentAnnotatedWith(predicate: AbstractPredicate.ParentAnnotatedWith<P>, data: CfirDeclaration): Boolean {
            val parent = data.directParentDeclaration ?: return false
            val parentPredicate = DeclarationPredicate.AnnotatedWith(predicate.annotations)

            return parentPredicate.accept(declarationPredicateMatcher, parent)
        }

        /**
         * 判断当前声明的任意直接子声明是否带有目标注解。
         */
        override fun visitHasAnnotatedWith(predicate: AbstractPredicate.HasAnnotatedWith<P>, data: CfirDeclaration): Boolean {
            val childPredicate = DeclarationPredicate.AnnotatedWith(predicate.annotations)

            return data.anyDirectChildDeclarationMatches(childPredicate)
        }

        /**
         * 当前声明的直接父 CFIR 声明。
         */
        private val CfirDeclaration.directParentDeclaration: CfirDeclaration?
            get() = getOwnersOfDeclaration(this)?.lastOrNull()?.cfir
    }

    /**
     * 判断当前声明的直接子声明中是否存在满足 [childPredicate] 的声明。
     */
    private fun CfirDeclaration.anyDirectChildDeclarationMatches(childPredicate: DeclarationPredicate): Boolean {
        var result = false

        this.forEachDirectChildDeclaration {
            result = result || childPredicate.accept(declarationPredicateMatcher, it)
        }

        return result
    }

    /**
     * 读取 [declaration] 自身直接声明的注解全限定名集合。
     *
     * 优先使用已解析 CFIR 注解类型；当 CFIR 注解尚未解析出类型时，回退到 IDE 注解索引解析 PSI 注解。
     */
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

    /**
     * 收集 [declaration] 所有外层 owner 声明上的注解全限定名。
     */
    private fun annotationsOnOuterDeclarations(declaration: CfirDeclaration): Set<AnnotationFqn> {
        return getOwnersOfDeclaration(declaration)?.flatMap { annotationsOnDeclaration(it.cfir) }.orEmpty().toSet()
    }
}

/**
 * 判断 annotation call 对应的注解类是否带有任一 [metaAnnotations]。
 *
 * 如果 [includeItself] 为 `true`，注解类自身命中 meta-annotation 集合也算匹配。
 */
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

/**
 * 递归判断注解类 symbol 是否直接或间接带有目标 meta-annotation。
 *
 * [visited] 用于切断注解之间的循环引用。
 */
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

/**
 * 保存单个 CFIR 文件中声明到其外层 owner symbol 列表的映射。
 */
private class CfirOwnersStorage(
    /**
     * 每个声明对应的外层 owner symbol 列表，顺序由外到内。
     */
    private val declarationToOwner: Map<CfirDeclaration, List<CfirBasedSymbol<*>>>
) {
    /**
     * 返回 [declaration] 的 owner symbol 列表。
     */
    fun getOwners(declaration: CfirDeclaration): List<CfirBasedSymbol<*>>? = declarationToOwner[declaration]

    companion object {
        /**
         * 遍历 [file] 并收集所有声明的 owner 信息。
         */
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
 * declarations (like file, classes, functions/variables and so on).
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

        override fun visitExtend(extend: CfirExtend) {
            action(extend)
        }
    })
}
