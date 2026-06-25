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
    /**
     * 插件谓词直接注册的注解集合。
     */
    abstract val annotations: Set<AnnotationFqn>

    /**
     * 插件谓词注册的元注解集合。
     */
    abstract val metaAnnotations: Set<AnnotationFqn>

    /**
     * 当前 session 是否存在插件注册注解或元注解。
     */
    val hasRegisteredAnnotations: Boolean
        get() = annotations.isNotEmpty() || metaAnnotations.isNotEmpty()

    /**
     * 查询带有指定元注解的用户注解集合。
     */
    abstract fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn>

    /**
     * 注册用户自定义注解与其元注解关系。
     */
    abstract fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<CfirClass>)

    /**
     * 计算指定声明谓词实际需要关注的注解集合。
     */
    abstract fun getAnnotationsForPredicate(predicate: DeclarationPredicate): Set<AnnotationFqn>

    /**
     * 初始化插件注解注册表。
     */
    @PluginServicesInitialization
    abstract fun initialize()

    /**
     * 空插件注解注册表。
     */
    object Empty : CfirRegisteredPluginAnnotations() {
        /**
         * 空实现没有普通注解。
         */
        override val annotations: Set<AnnotationFqn>
            get() = emptySet()

        /**
         * 空实现没有元注解。
         */
        override val metaAnnotations: Set<AnnotationFqn>
            get() = emptySet()

        /**
         * 空实现不返回元注解映射。
         */
        override fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn> = emptyList()

        /**
         * 空实现禁止注册用户自定义注解。
         */
        override fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<CfirClass>) {
            shouldNotBeCalled()
        }

        /**
         * 空实现对任意谓词都返回空注解集合。
         */
        override fun getAnnotationsForPredicate(predicate: DeclarationPredicate): Set<AnnotationFqn> = emptySet()

        /**
         * 空实现不允许初始化。
         */
        @PluginServicesInitialization
        override fun initialize() {
            shouldNotBeCalled()
        }
    }
}

/**
 * 插件注解注册表的可扩展基类。
 *
 * @property session 当前注册表所属 session。
 */
abstract class AbstractCfirRegisteredPluginAnnotations(
    /**
     * 当前注册表所属的 CFIR session。
     */
    protected val session: CfirSession,
) : CfirRegisteredPluginAnnotations() {
    /**
     * 从插件谓词中收集到的元注解集合。
     */
    final override val metaAnnotations: MutableSet<AnnotationFqn> = mutableSetOf()

    /**
     * 声明谓词到实际注解集合的缓存。
     */
    private val annotationsForPredicateCache: CfirCache<DeclarationPredicate, Set<AnnotationFqn>, Nothing?> =
        session.cfirCachesFactory.createCache { predicate ->
            collectAnnotations(predicate)
        }

    /**
     * 返回指定谓词需要关注的实际注解集合。
     */
    final override fun getAnnotationsForPredicate(predicate: DeclarationPredicate): Set<AnnotationFqn> {
        return annotationsForPredicateCache.getValue(predicate)
    }

    /**
     * 收集谓词直接注解与元注解展开后的注解集合。
     */
    private fun collectAnnotations(predicate: DeclarationPredicate): Set<AnnotationFqn> {
        val result = predicate.metaAnnotations.flatMapTo(mutableSetOf()) { getAnnotationsWithMetaAnnotation(it) }
        if (result.isEmpty()) return predicate.annotations
        result += predicate.annotations
        return result
    }

    /**
     * 从已注册扩展中收集所有谓词依赖的注解和元注解。
     */
    @PluginServicesInitialization
    final override fun initialize() {
        val registrar = object : CfirDeclarationPredicateRegistrar() {
            /**
             * 初始化期间收集到的谓词。
             */
            val predicates = mutableListOf<AbstractPredicate<*>>()

            /**
             * 注册 vararg 谓词。
             */
            override fun register(vararg predicates: AbstractPredicate<*>) {
                this.predicates += predicates
            }

            /**
             * 注册谓词集合。
             */
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

    /**
     * 保存插件直接注册的注解集合。
     */
    protected abstract fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>)
}

/**
 * 默认插件注解注册表实现。
 */
@NoMutableState
class CfirRegisteredPluginAnnotationsImpl(session: CfirSession) : AbstractCfirRegisteredPluginAnnotations(session) {
    /**
     * 插件直接注册和用户自定义注解展开后的注解集合。
     */
    override val annotations: MutableSet<AnnotationFqn> = mutableSetOf()

    /**
     * 元注解到用户自定义注解的映射。
     */
    private val userDefinedAnnotations: Multimap<AnnotationFqn, AnnotationFqn> = LinkedHashMultimap.create()

    /**
     * 查询带有指定元注解的用户自定义注解。
     */
    override fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn> {
        return userDefinedAnnotations[metaAnnotation]
    }

    /**
     * 保存插件直接声明需要关注的注解。
     */
    override fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>) {
        this.annotations += annotations
    }

    /**
     * 注册用户自定义注解与元注解关系。
     */
    override fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<CfirClass>) {
        val annotations = annotationClasses.map { it.symbol.classId.asSingleFqName() }
        this.annotations += annotations
        userDefinedAnnotations.putAll(metaAnnotation, annotations)
    }
}

/**
 * 当前 session 的插件注解注册表。
 */
val CfirSession.registeredPluginAnnotations: CfirRegisteredPluginAnnotations by CfirSession.sessionComponentAccessor()
