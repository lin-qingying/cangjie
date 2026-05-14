/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.utils.StableHash

/**
 * Macro construction 单文件 cache key（baseline PLAN.md §11）。
 *
 * 共 **13 维**，与 baseline 第 11 节 "cache key 包含" 列表一一对应：
 *
 *   1. [sourceContentHash]            ← source content
 *   2. [fileIdentity]                 ← file identity
 *   3. [macroSurfaceRangesHash]       ← macro surface ranges
 *   4. [importsHash]                  ← imports / default imports / resolved macro bindings
 *   5. [modulePackageIdentity]        ← module / package identity
 *   6. [sdkSignature]                 ← SDK / language version
 *   7. [macroDependencySignature]     ← stdlib / macro artifacts
 *   8. [compilerOptionsHash]          ← compiler / language options
 *   9. [executorAbi]                  ← executor ABI / protocol / version
 *  10. [constructionAlgorithmVersion] ← macro construction algorithm version
 *  11. [tokenScannerVersion]          ← token scanner version
 *  12. [fragmentParserVersion]        ← fragment parser mode / version
 *  13. [runtimeFingerprint]           ← builtin macro/non-macro registry version
 *                                      + macro expand iteration limit
 *                                      + macro result token hash
 *
 * 任何单一维度变化即使 [stableHash] 翻动，外部 cache 层据此失效该文件项；
 * [MacroExpansionRegistry.moduleSignature] 把所有文件 hash 合并后参与
 * 模块级失效（baseline 第 11 节 "macro artifact / import / builtin registry
 * 改变时模块级失效"）。
 *
 * **不在 cache key 里**：源码 PSI 节点 identity、line/column 等会因 IDE
 * 重排而漂移的字段；只把"足以让宏展开产物变化"的稳定维度纳入。
 */
data class MacroExpansionCacheKey(
    val sourceContentHash: String,
    val fileIdentity: String,
    val macroSurfaceRangesHash: String,
    val importsHash: String,
    val modulePackageIdentity: String,
    val sdkSignature: String,
    val macroDependencySignature: String,
    val compilerOptionsHash: String,
    val executorAbi: String,
    val constructionAlgorithmVersion: Int,
    val tokenScannerVersion: Int,
    val fragmentParserVersion: Int,
    val runtimeFingerprint: String,
) {
    /** 13 维聚合成一个稳定 hash，用作上游 cache 的查找键。 */
    fun stableHash(): String = StableHash.sha256Of(
        sourceContentHash,
        fileIdentity,
        macroSurfaceRangesHash,
        importsHash,
        modulePackageIdentity,
        sdkSignature,
        macroDependencySignature,
        compilerOptionsHash,
        executorAbi,
        constructionAlgorithmVersion.toString(),
        tokenScannerVersion.toString(),
        fragmentParserVersion.toString(),
        runtimeFingerprint,
    )

    companion object {
        /** Cache key 维度数。本常量供 architecture guard / 上游 tooling 校验。 */
        const val DIMENSION_COUNT: Int = 13
    }
}
