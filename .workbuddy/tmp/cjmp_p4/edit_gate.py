import sys

s = ""


def rep(old, new):
    global s
    assert s.count(old) == 1, old
    s = s.replace(old, new)


def load(p):
    global s
    s = open(p, encoding='utf-8', newline='').read()
    # 统一按 LF 匹配，写回时恢复原行尾
    crlf = '\r\n' in s
    s = s.replace('\r\n', '\n')
    return crlf


def save(p, crlf):
    out = s.replace('\n', '\r\n') if crlf else s
    open(p, 'w', encoding='utf-8', newline='').write(out)


root = 'D:/code/intellij/cangjie/'

p = root + 'cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjmpCommonPartLoadGate.kt'
c = load(p)
rep('''    fun optLevelOrdinal(level: String): UByte = OPT_LEVEL_ORDINALS[level] ?: 0u
''', '''    fun optLevelOrdinal(level: String): UByte = OPT_LEVEL_ORDINALS[level] ?: 0u

    /** 官���序号 → 优化级别字符串（`OptimizationLevelToString` 对位）。 */
    private fun optLevelName(ordinal: UByte): String =
        OPT_LEVEL_ORDINALS.entries.firstOrNull { it.value == ordinal }?.key ?: "(unsupported)"
''')
rep('''                message = "common cjo '$actual' does not match the expected package '$expected'",''',
    '''                message = "common part is for another package '$actual', " +
                    "expected the same as for current package '$expected'.",''')
rep('''    /**
     * ③ features 子集门：common part 文件的每个 feature 必须出现在 specific 侧 features 中
     *（官方 `feature_is_not_subset_of_child_set`）。
     */
    fun checkFeaturesSubset(
        packageName: String,
        commonFeatures: Collection<List<String>>,
        specificFeatures: Set<String>,
    ): CfirCjmpLoadDiagnostic? {
        val missing = commonFeatures
            .map { identifiers -> identifiers.joinToString(".") }
            .filter { feature -> feature.isNotEmpty() && feature !in specificFeatures }
        if (missing.isEmpty()) return null
        return CfirCjmpLoadDiagnostic(
            kind = CfirCjmpLoadDiagnosticKind.FEATURE_NOT_SUBSET,
            packageName = packageName,
            message = "feature from common part is not a subset of the child feature set: " +
                missing.joinToString(", ") { "'$it'" },
        )
    }''', '''    /**
     * ③ features 子集门：common part **每个文件**的 features 必须是 specific 侧 features 的子集
     *（官方 `ValidateCommonSpecificFeatureSetsRelations` 逐文件判定，诊断
     * `feature_is_not_subset_of_child_set`，附注列出多出的 feature）。
     *
     * @param commonFileFeatures common part 文件名 → 该文件 features（每个 feature 为分段标识列表）。
     */
    fun checkFeaturesSubset(
        packageName: String,
        commonFileFeatures: Map<String, List<List<String>>>,
        specificFeatures: Set<String>,
    ): List<CfirCjmpLoadDiagnostic> = commonFileFeatures.mapNotNull { (fileName, features) ->
        val extra = features
            .map { identifiers -> identifiers.joinToString(".") }
            .filter { feature -> feature.isNotEmpty() && feature !in specificFeatures }
        if (extra.isEmpty()) return@mapNotNull null
        CfirCjmpLoadDiagnostic(
            kind = CfirCjmpLoadDiagnosticKind.FEATURE_NOT_SUBSET,
            packageName = packageName,
            message = "parent feature set must be subset of child feature set, package: '$packageName', " +
                "file with conflicted features: '$fileName'; extra feature from common part file: " +
                extra.joinToString(" ") { "'$it'" },
        )
    }''')
rep('''     * 选项缺失只诊断不拒绝（官方 `ValidateOptions` 在 `options == nullptr` 时记诊断后继续）；''',
    '''     * 选项缺失只告警不拒绝（官方 `ValidateOptions` 在 `options == nullptr` 时报 WARNING 后继续）；''')
rep('''                    message = "common cjo '$packageName' has no embedded compile options",
                ),''', '''                    message = "common part cjo is missing serialized options, possibly compiled by an old cjc",
                    isError = false,
                ),''')
rep('''                    message = "common cjo debug option is ${if (options.debug) "enabled" else "disabled"} " +
                        "but the current compile is ${if (debug) "enabled" else "disabled"}",''',
    '''                    message = "common part is compiled with different debug mode: " +
                        "${if (options.debug) "enabled" else "disabled"} in common and " +
                        "${if (debug) "enabled" else "disabled"} in current",''')
rep('''                    message = "common cjo optimization level is ${options.optLevel} " +
                        "but the current compile is $optLevel",''',
    '''                    message = "common part is compiled with different optimization level: " +
                        "${optLevelName(options.optLevel)} in common and ${optLevelName(optLevel)} " +
                        "in the current compilation",''')
save(p, c)

p = root + 'cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/CfirDeserializedSymbolProvider.kt'
c = load(p)
rep('''        CjmpCommonPartLoadGate.checkFeaturesSubset(
            packageName = header.fullPkgName,
            commonFeatures = header.fileFeatures.values.flatten(),
            specificFeatures = settings.packageFeatures,
        )?.let(collector::record)''', '''        CjmpCommonPartLoadGate.checkFeaturesSubset(
            packageName = header.fullPkgName,
            commonFileFeatures = header.fileFeatures,
            specificFeatures = settings.packageFeatures,
        ).forEach(collector::record)''')
save(p, c)

p = root + 'cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjmpCommonPartCjoTest.kt'
c = load(p)
rep('''        assertNull(
            CjmpCommonPartLoadGate.checkFeaturesSubset(
                packageName = "sample.cjmp",
                commonFeatures = listOf(listOf("Foo"), listOf("Bar", "Baz")),
                specificFeatures = setOf("Foo", "Bar.Baz", "Extra"),
            ),
        )
        val notSubset = CjmpCommonPartLoadGate.checkFeaturesSubset(
            packageName = "sample.cjmp",
            commonFeatures = listOf(listOf("Foo"), listOf("Bar", "Baz")),
            specificFeatures = setOf("Foo"),
        )
        assertNotNull(notSubset)''', '''        val commonFileFeatures = mapOf("common.cj" to listOf(listOf("Foo"), listOf("Bar", "Baz")))
        assertTrue(
            CjmpCommonPartLoadGate.checkFeaturesSubset(
                packageName = "sample.cjmp",
                commonFileFeatures = commonFileFeatures,
                specificFeatures = setOf("Foo", "Bar.Baz", "Extra"),
            ).isEmpty(),
        )
        val notSubset = CjmpCommonPartLoadGate.checkFeaturesSubset(
            packageName = "sample.cjmp",
            commonFileFeatures = commonFileFeatures,
            specificFeatures = setOf("Foo"),
        ).single()''')
rep('''        assertEquals(1, missing.size)
''', '''        assertEquals(1, missing.size)
        assertTrue(!missing.single().isError, "missing options is a warning (module_common_cjo_no_options)")
''')
save(p, c)
print("ok")
