package org.cangnova.cangjie.analysis.api.types

/**
 * 未解析的 class-like 限定段。
 *
 * 在 [CaClassTypeQualifier] sealed 层级中,此分支用于表达 “这一段名字未能解析为 classifier 符号” 的情形。
 *
 * - 仅保留源码中仍可稳定恢复的限定信息([name]、[typeArguments]);
 * - 不暴露 symbol,避免把未解析状态伪装成已解析类型;
 * - 通常出现在 [CaClassErrorType] 的 qualifiers 列表中,与 [CaResolvedClassTypeQualifier] 混合存在。
 *
 * 对齐 Kotlin Analysis API 的 `KaUnresolvedClassTypeQualifier`。
 */
interface CaUnresolvedClassTypeQualifier : CaClassTypeQualifier
