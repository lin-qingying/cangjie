package org.cangnova.cangjie.analysis.api.types

/**
 * 未解析的 class-like qualifier 片段。
 *
 * 该叶子只保留源码中仍可稳定恢复的限定名信息，不暴露 symbol，
 * 以避免把未解析状态伪装成已解析类型。
 */
interface CaUnresolvedClassTypeQualifier : CaClassTypeQualifier
