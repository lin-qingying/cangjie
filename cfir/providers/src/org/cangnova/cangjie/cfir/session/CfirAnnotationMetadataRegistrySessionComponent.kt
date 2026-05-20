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

val CfirSession.annotationMetadataRegistryOrNull: CfirAnnotationMetadataRegistry? by CfirSession.nullableSessionComponentAccessor()

fun CfirSession.ensureAnnotationMetadataRegistry(): CfirAnnotationMetadataRegistry {
    annotationMetadataRegistryOrNull?.let { return it }
    return CfirAnnotationMetadataRegistry().also {
        register(CfirAnnotationMetadataRegistry::class, it)
    }
}
