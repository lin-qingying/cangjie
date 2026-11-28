package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataRegistry

/**
 * session 上的 CFIR annotation metadata registry。
 *
 * 只提供已发布 annotation slot 查询；raw 与 macro construction 使用显式私有 writer。
 */
val CfirSession.annotationMetadataRegistry: CfirAnnotationMetadataRegistry by CfirSession.sessionComponentAccessor()

/**
 * session 上的 CFIR annotation metadata registry，可空访问版本。
 */
val CfirSession.annotationMetadataRegistryOrNull: CfirAnnotationMetadataRegistry? by CfirSession.nullableSessionComponentAccessor()

/**
 * 确保当前 session 已注册 annotation metadata registry。
 *
 * 已存在时复用原实例；缺失时创建并注册新实例。
 */
fun CfirSession.ensureAnnotationMetadataRegistry(): CfirAnnotationMetadataRegistry = synchronized(this) {
    annotationMetadataRegistryOrNull?.let { return@synchronized it }
    CfirAnnotationMetadataRegistry(this).also {
        register(CfirAnnotationMetadataRegistry::class, it)
    }
}
