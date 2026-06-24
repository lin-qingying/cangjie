package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataRegistry

/**
 * session 上的 CFIR annotation metadata registry。
 *
 * raw builder 在写入 owner.annotations 后登记 annotation slot snapshot；
 * macro construction、analysis API、LL resolve 与 checker 统一从这里读取
 * annotation macro 的 raw slot 信息。
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
fun CfirSession.ensureAnnotationMetadataRegistry(): CfirAnnotationMetadataRegistry {
    annotationMetadataRegistryOrNull?.let { return it }
    return CfirAnnotationMetadataRegistry().also {
        register(CfirAnnotationMetadataRegistry::class, it)
    }
}
