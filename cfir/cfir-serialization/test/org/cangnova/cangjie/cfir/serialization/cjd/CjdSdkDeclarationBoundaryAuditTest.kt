package org.cangnova.cangjie.cfir.serialization.cjd

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 声明种类边界复核（P4 收尾）：对真实 SDK 语料做**无静默丢失**审计。
 *
 * 审计的不是覆盖率数字（那随 SDK 版本变化），而是**每一条带注解的 sidecar 条目都必须有归宿**：
 * 要么被合并，要么有一条指向它自己的匹配诊断，要么其祖先已经解释了原因。
 * 任何"够不到且无人解释"的条目都是静默丢失 ⇒ 用断言钉死。
 *
 * 与官方 `MergeAnnoFromCjd.cpp:486` 一致的门控（顶层声明自身无注解时不进入合并）单独计桶，
 * 因为它既是官方行为，也是本 SDK 语料中差额的主要来源。
 */
class CjdSdkDeclarationBoundaryAuditTest :
    CjParsingTestCase("", "cj.d", CangJieDeclarationFileType, CangJieParserDefinition()) {

    @BeforeEach fun setupFixture() { setUp() }
    @AfterEach fun teardownFixture() { tearDown() }

    @Test fun testConfiguredSdkDeclarationBoundaryAudit() {
        val directory = System.getenv("CANGJIE_CJD_SDK_DIR")
        assumeTrue(!directory.isNullOrBlank(), "Set CANGJIE_CJD_SDK_DIR to enable real SDK validation")
        val paths = Files.list(Path.of(directory!!)).use { stream ->
            stream.filter { it.toString().endsWith(".cj.d") }.sorted().toList()
        }
        assertEquals(38, paths.size, "Expected the 38-file SDK evidence corpus")

        val entriesByKind = sortedMapOf<String, Int>()
        val annotationsByKind = sortedMapOf<String, Int>()
        val mergedByKind = sortedMapOf<String, Int>()
        val buckets = sortedMapOf<String, Int>()
        val bucketSamples = linkedMapOf<String, MutableList<String>>()
        val unattributed = mutableListOf<String>()
        val ambiguous = mutableListOf<String>()
        val attributedDetail = mutableListOf<String>()
        val attributedByFile = sortedMapOf<String, Int>()
        // 二进制侧问题（sourceRange == null）：用来区分 MISSING 的两种成因——
        // 导出门控（NON_EXPORTED）与类型元数据不可解析（UNSUPPORTED / 参数键不相等）。
        val binarySideCounts = sortedMapOf<String, Int>()
        val nonExportedHit = sortedMapOf<String, Int>()
        var annotationTotal = 0
        var annotationMerged = 0
        var apiLevelTotal = 0
        var apiLevelMerged = 0

        fun bucket(name: String, annotations: Int, apiLevel: Int, sample: String) {
            buckets[name] = (buckets[name] ?: 0) + annotations
            buckets["$name#APILevel"] = (buckets["$name#APILevel"] ?: 0) + apiLevel
            val samples = bucketSamples.getOrPut(name) { mutableListOf() }
            if (samples.size < 6) samples += "$sample all=$annotations apiLevel=$apiLevel"
        }

        for (path in paths) {
            val sidecar = CjdSidecarParser.create().parse(path)
            assertTrue(sidecar.isUsable, "$path: ${sidecar.diagnostics}")
            val manager = CjoManager(CjoSearchPath { if (it == "CANGJIE_STDLIB_MODULE") directory else null })
            val loaded = assertNotNull(
                manager.loadPackageSnapshot(sidecar.annotationContext.packageFqName),
                "$path: no CJO for ${sidecar.annotationContext.packageFqName}",
            )
            val context = CfirDeserializationContext(loaded.pkg, loaded.header, Module(), manager, loaded.sourcePath)
            val matcher = CjdBinaryDeclarationMatcher.create(context, sidecar)
            ambiguous += matcher.diagnostics
                .filter { it.kind == CjdBinaryMatchDiagnosticKind.AMBIGUOUS }
                .map { "${path.fileName}: $it" }
            matcher.diagnostics.filter { it.sourceRange == null }.forEach { diagnostic ->
                val key = "${diagnostic.kind.name}@${path.fileName}"
                binarySideCounts[key] = (binarySideCounts[key] ?: 0) + 1
                if (diagnostic.kind == CjdBinaryMatchDiagnosticKind.NON_EXPORTED) {
                    val identifier = diagnostic.declarationIndex?.let { loaded.pkg.allDecls(it)?.identifier } ?: "<unknown>"
                    nonExportedHit["$identifier@${path.fileName}"] = (nonExportedHit["$identifier@${path.fileName}"] ?: 0) + 1
                }
            }

            // 实际被授予合并的条目（身份相等，不能用结构 equals）。
            val matchedEntries = Collections.newSetFromMap(IdentityHashMap<CjdDeclarationEntry, Boolean>())
            val mergedByEntry = IdentityHashMap<CjdDeclarationEntry, Int>()
            val mergedApiLevelByEntry = IdentityHashMap<CjdDeclarationEntry, Int>()
            for (index in 0 until loaded.pkg.allDeclsLength) {
                val selection = matcher.selection(index) ?: continue
                matchedEntries += selection.entry
                mergedByEntry[selection.entry] = (mergedByEntry[selection.entry] ?: 0) + selection.annotations.size
                mergedApiLevelByEntry[selection.entry] =
                    (mergedApiLevelByEntry[selection.entry] ?: 0) + selection.annotations.count { it.name == API_LEVEL }
            }
            // 诊断按"它指向的 sidecar 条目"归组；sourceRange 为空的诊断不指向任何条目。
            val diagnosticsByRange = matcher.diagnostics
                .mapNotNull { diagnostic -> diagnostic.sourceRange?.let { it to diagnostic } }
                .groupBy({ it.first }, { it.second })

            fun walk(entries: List<CjdDeclarationEntry>, reachable: Boolean, blockedBy: String?, topLevel: Boolean) {
                for (entry in entries) {
                    val kind = entry.key.kind
                    val apiLevel = entry.annotations.count { it.name == API_LEVEL } +
                        entry.parameterAnnotations.sumOf { list -> list.count { it.name == API_LEVEL } }
                    val annotations = entry.annotations.size + entry.parameterAnnotations.sumOf { it.size }
                    entriesByKind[kind.name] = (entriesByKind[kind.name] ?: 0) + 1
                    annotationsByKind[kind.name] = (annotationsByKind[kind.name] ?: 0) + annotations
                    annotationTotal += annotations
                    apiLevelTotal += apiLevel

                    val isMerged = entry in matchedEntries
                    val ownDiagnostics = diagnosticsByRange[entry.range].orEmpty()
                    // 官方同款门控：顶层声明自身无注解（或 main）时不进入合并，其成员随之不可达。
                    val gatedAtTopLevel = topLevel &&
                        (entry.annotations.isEmpty() || kind == CjdDeclarationKind.MAIN)

                    when {
                        isMerged -> {
                            val actual = mergedByEntry[entry] ?: 0
                            annotationMerged += actual
                            mergedByKind[kind.name] = (mergedByKind[kind.name] ?: 0) + actual
                            apiLevelMerged += mergedApiLevelByEntry[entry] ?: 0
                            val dropped = annotations - actual
                            if (dropped > 0) bucket("MERGED_BUT_WITHHELD", dropped, 0, "$path $kind:${entry.key.identifier}")
                        }

                        ownDiagnostics.isNotEmpty() -> {
                            bucket(
                                "ATTRIBUTED_SELF_" + ownDiagnostics.map { it.kind.name }.distinct().sorted().joinToString("+"),
                                annotations, apiLevel, "$path $kind:${entry.key.identifier}",
                            )
                            attributedDetail += "${path.fileName} $kind:${entry.key.identifier} " +
                                "apiLevel=$apiLevel diagnostics=${ownDiagnostics.map { it.kind.name + ":" + it.message }}"
                            val key = path.fileName.toString()
                            attributedByFile[key] = (attributedByFile[key] ?: 0) + apiLevel
                        }

                        gatedAtTopLevel -> bucket(
                            if (kind == CjdDeclarationKind.MAIN) "EXCLUDED_R13_MAIN"
                            else "GATED_TOP_LEVEL_WITHOUT_OWN_ANNOTATIONS",
                            annotations, apiLevel, "$path $kind:${entry.key.identifier}",
                        )

                        !reachable -> bucket("UNREACHABLE_BY_ANCESTOR_$blockedBy", annotations, apiLevel,
                            "$path $kind:${entry.key.identifier}")

                        else -> {
                            bucket("UNATTRIBUTED", annotations, apiLevel, "$path $kind:${entry.key.identifier}")
                            unattributed += "$path $kind:${entry.key.identifier} range=${entry.range} " +
                                "annotations=${entry.annotations.map { it.name }} " +
                                "diagnostics=${matcher.diagnostics.filter { it.sourceRange == entry.range }}"
                        }
                    }

                    // 只有被合并的条目才会继续向下遍历；否则子孙全部不可达。
                    val childBlockedBy = when {
                        isMerged -> blockedBy
                        ownDiagnostics.isNotEmpty() ->
                            ownDiagnostics.map { it.kind.name }.distinct().sorted().joinToString("+")
                        gatedAtTopLevel -> if (kind == CjdDeclarationKind.MAIN) "R13_MAIN" else "TOP_LEVEL_GATE"
                        else -> "NONE"
                    }
                    walk(entry.members, isMerged, childBlockedBy, false)
                }
            }

            walk(sidecar.declarations, true, null, true)
        }

        println("[cjd-audit] files=${paths.size} annotations=$annotationTotal merged=$annotationMerged " +
            "apiLevel=$apiLevelTotal apiLevelMerged=$apiLevelMerged")
        println("[cjd-audit] entries by kind: ${entriesByKind.toSortedMap()}")
        println("[cjd-audit] annotations by kind (all / merged):")
        annotationsByKind.toSortedMap().forEach { (kind, count) ->
            println("  $kind all=$count merged=${mergedByKind[kind] ?: 0}")
        }
        println("[cjd-audit] disposition buckets (allAnnotations / apiLevel):")
        buckets.filterKeys { !it.endsWith("#APILevel") }.forEach { (name, count) ->
            println("  $name all=$count apiLevel=${buckets[name + "#APILevel"] ?: 0}")
            bucketSamples[name]?.forEach { println("    - $it") }
        }
        println("[cjd-audit] ATTRIBUTED_SELF apiLevel by file: ${attributedByFile.toSortedMap()}")
        println("[cjd-audit] ATTRIBUTED_SELF detail (${attributedDetail.size}):")
        attributedDetail.sorted().forEach { println("    $it") }
        val binarySideByKind = sortedMapOf<String, Int>()
        binarySideCounts.forEach { (key, count) ->
            val kind = key.substringBefore('@')
            binarySideByKind[kind] = (binarySideByKind[kind] ?: 0) + count
        }
        println("[cjd-audit] binary-side diagnostics grouped: $binarySideByKind")
        println("[cjd-audit] NON_EXPORTED total=${nonExportedHit.values.sum()} by name: ${nonExportedHit.toSortedMap()}")

        assertTrue(ambiguous.isEmpty(), "Ambiguous matching must not appear:\n${ambiguous.take(20).joinToString("\n")}")
        assertTrue(unattributed.isEmpty(),
            "Sidecar entries with annotations must never be dropped without attribution:\n" +
                unattributed.take(20).joinToString("\n"))
        assertEquals(6497, apiLevelTotal, "APILevel corpus size changed; re-derive the boundary accounting")
        assertTrue(apiLevelMerged > 0, "Sidecar annotations must reach at least one declaration")
        assertEquals(annotationTotal - annotationMerged,
            buckets.filterKeys { !it.endsWith("#APILevel") && it != "UNATTRIBUTED" }.values.sum(),
            "Every unmerged annotation must fall into exactly one disposition bucket")
    }

    private class Module : CfirModuleData() {
        override val name = Name.identifier("cjd-audit")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjd-audit"
        override val session = object : CfirSession(CfirSession.Kind.Library) {}.also {
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }

    private companion object {
        const val API_LEVEL = "APILevel"
    }
}
