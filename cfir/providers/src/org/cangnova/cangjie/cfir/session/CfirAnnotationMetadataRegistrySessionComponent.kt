package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataRegistry

/**
 * session 上的 CFIR annotation metadata registry。
 *
 * 只提供已发布 annotation slot 查询；raw 与 macro construction 使用显式私有 writer。
 */
val CfirSession.annotationMetadataRegistry: CfirAnnotationMetadataRegistry
    get() = checkNotNull(annotationMetadataRegistryOrNull) { "Annotation metadata registry has not been registered." }

/**
 * session 上的 CFIR annotation metadata registry，可空访问版本。
 */
val CfirSession.annotationMetadataRegistryOrNull: CfirAnnotationMetadataRegistry?
    get() = synchronized(this) { registeredAnnotationMetadataRegistry }

private val CfirSession.registeredAnnotationMetadataRegistry: CfirAnnotationMetadataRegistry? by
    CfirSession.nullableSessionComponentAccessor()

/**
 * 确保当前 session 已注册 annotation metadata registry。
 *
 * 已存在时复用原实例；缺失时创建并注册新实例。
 */
fun CfirSession.ensureAnnotationMetadataRegistry(): CfirAnnotationMetadataRegistry = synchronized(this) {
    annotationMetadataRegistryOrNull ?: CfirAnnotationMetadataRegistry(this).also {
        register(CfirAnnotationMetadataRegistry::class, it)
    }
}

/**
 * 为一轮新的源码快照构造创建独立 metadata registry。
 *
 * IDE 可能在宏展开期间观察到 PSI revision 变化，并在同一 session 中重新采集 raw CFIR。
 * 上一轮 construction 已冻结的 registry 属于旧输入，不能被下一轮 raw builder 复用。
 */
fun CfirSession.beginAnnotationMetadataRegistryGeneration(): CfirAnnotationMetadataRegistry = synchronized(this) {
    CfirAnnotationMetadataRegistry(this).also {
        register(CfirAnnotationMetadataRegistry::class, it)
    }
}
