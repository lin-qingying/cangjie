package org.cangnova.cangjie.analysis.api.cfir.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/**
 * 作用域组件的 **context 桥接清单**校验（任务 7.5 的可机械化部分，缺陷 D-6 的守门用例）。
 *
 * 背景：`CaScopeProvider` 底部的 `context(session: CaSession)` 桥接标着「自动生成的 context 桥接。
 * 请勿手工修改」，但**没有任何生成器**（7.5 的取证结论：参考实现里不存在这类生成器，
 * 「哪些成员需要桥接」本身是人写的设计决定）。于是真实风险是**加了成员忘了写桥接**——
 * 静态可查、且一旦漏掉在调用侧只表现为「这个入口用不了」，不是错误。
 *
 * 本用例把清单**显式写下来**，并直接扫源码文件校验：
 * 1. 接口成员集合必须与清单完全一致——新增成员时本用例红，逼作者当场决定「要不要桥接」，
 *    而不是让一个没人决策的成员静默留在接口里；
 * 2. 标为 `bridged` 的成员必须在桥接区里有对应声明——桥接被删或写错名字时红。
 *
 * 这是**源码级**约定校验，不跑分析、不依赖 PSI：清单是人对格式的承诺，就该按格式检查。
 */
class ScopeProviderBridgeCoverageTest {

    /**
     * 桥接清单：`成员签名 -> bridged`（需要桥接）或 `unbridged`（刻意不桥接）。
     *
     * 不桥接的成员必须给理由——「选择性桥接」是设计决定，不是遗漏。
     */
    private val scopeProviderChecklist = linkedMapOf(
        "CjFile.getFileScope" to "bridged",
        "CaPackageSymbol.packageScope" to "bridged",
        "CaDeclarationContainerSymbol.combinedDeclaredMemberScope" to "bridged",
        "CaClassLikeSymbol.declaredMemberScope" to "bridged",
        "CaExtendSymbol.declaredMemberScope" to "bridged",
        "CaClassLikeSymbol.memberScope" to "bridged",
        "CaDeclarationContainerSymbol.memberScope" to "bridged",
        "CaType.scope" to "bridged",
        // 以下七个刻意不桥接：调用方都要传位置/访问形态（scopeContext、memberScopeQueries）、
        // 或返回的是另一类视图（instanceMemberScope/staticMemberScope、fragmentMemberScope、
        // compositeScope）。context(session) 桥接只服务「给一个会话就能取」的入口，硬加会诱导
        // 调用方在拿不到位置时误用，因此保持不桥接。
        "CaType.instanceMemberScope" to "unbridged",
        "CaType.staticMemberScope" to "unbridged",
        "CaType.memberScopeQueries" to "unbridged",
        "CjFile.scopeContext" to "unbridged",
        "CjFile.importingScopeContext" to "unbridged",
        "CjCodeFragment.fragmentMemberScope" to "unbridged",
        "CaScopeContext.compositeScope" to "unbridged",
    )

    /**
     * 类型作用域组件的清单（4.1 新增，同样走桥接约定）。
     */
    private val typeScopeProviderChecklist = linkedMapOf(
        "CaType.typeScope" to "bridged",
        "CaTypeScope.declarationScope" to "bridged",
    )

    /**
     * 接口成员集合必须与清单一致：新增成员而没更新清单时本用例红。
     */
    @Test
    fun scopeProviderMembersAreAllClassified() {
        val members = ScopeProviderBridgeScanner.scan(
            file = scopeProviderFile("CaScopeProvider.kt"),
            interfaceName = "CaScopeProvider",
        ).members
        assertEquals(
            scopeProviderChecklist.keys.sorted(),
            members.map { it }.sorted(),
            "CaScopeProvider 新增/删除了成员：请在 ScopeProviderBridgeCoverageTest 的清单里明确它是 bridged 还是 unbridged",
        )
    }

    /**
     * 标为 bridged 的成员必须有桥接：桥接被删时本用例红（D-6 的真实风险）。
     */
    @Test
    fun bridgedScopeProviderMembersHaveBridges() {
        val scan = ScopeProviderBridgeScanner.scan(
            file = scopeProviderFile("CaScopeProvider.kt"),
            interfaceName = "CaScopeProvider",
        )
        val expectedBridges = scopeProviderChecklist.filterValues { it == "bridged" }.keys
        assertEquals(
            expectedBridges.sorted(),
            scan.bridges.sorted(),
            "桥接与清单不一致：一条 `context(session) val` 被删/改名，或清单该更新了",
        )
    }

    /**
     * 类型作用域组件同样校验（成员 → 桥接）。
     */
    @Test
    fun typeScopeProviderBridgesMatchChecklist() {
        val scan = ScopeProviderBridgeScanner.scan(
            file = scopeProviderFile("CaTypeScopeProvider.kt"),
            interfaceName = "CaTypeScopeProvider",
        )
        assertEquals(
            typeScopeProviderChecklist.keys.sorted(),
            scan.members.sorted(),
            "CaTypeScopeProvider 新增/删除了成员：请在清单里明确分类",
        )
        assertEquals(
            typeScopeProviderChecklist.filterValues { it == "bridged" }.keys.sorted(),
            scan.bridges.sorted(),
            "CaTypeScopeProvider 的 context 桥接与清单不一致",
        )
    }

    /**
     * 定位 `analysis-api` 的组件源码文件。
     *
     * 测试任务的工作目录是仓库根（见 `analysis-api-cfir/build.gradle.kts` 的 `workingDir = rootDir`），
     * 与生成套件按 `File("analysis/...")` 读夹具的方式一致。
     */
    private fun scopeProviderFile(name: String): File {
        val file = File("analysis/analysis-api/src/org/cangnova/cangjie/analysis/api/components/$name")
        check(file.isFile) {
            "找不到组件源码文件（工作目录必须是仓库根）：${file.absolutePath}"
        }
        return file
    }
}

/**
 * 源码级扫描：从组件接口文件里取出**接口体内的成员扩展**与**桥接区的桥接**。
 *
 * 只用最小文本规则，不解析 Kotlin：
 * - 成员 = 接口块（`interface X ... {` 到配对大括号）内形如 `val Type.name` / `fun Type.name(` 的行；
 * - 桥接 = 接口块之后形如 `val Type.name` 的顶层声明（桥接区就挂在 `context(session: CaSession)` 后面）。
 */
private object ScopeProviderBridgeScanner {
    /** 扫描结果：接口成员与桥接，均为 `Type.name` 形式。 */
    data class ScanResult(val members: List<String>, val bridges: List<String>)

    fun scan(file: File, interfaceName: String): ScanResult {
        val source = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
        val headerIndex = source.indexOf("interface $interfaceName")
        check(headerIndex >= 0) { "找不到 interface $interfaceName（文件：${file.path}）" }
        val bodyStart = source.indexOf('{', headerIndex)
        check(bodyStart >= 0) { "interface $interfaceName 没有函数体" }
        val bodyEnd = matchBrace(source, bodyStart)
        val body = source.substring(bodyStart + 1, bodyEnd)
        val rest = source.substring(bodyEnd + 1)

        val memberPattern = Regex("""^\s*(?:val|fun)\s+([A-Za-z_][\w]*)\.([A-Za-z_][\w]*)""", RegexOption.MULTILINE)
        val bridgePattern = Regex("""^(?:val|fun)\s+([A-Za-z_][\w]*)\.([A-Za-z_][\w]*)""", RegexOption.MULTILINE)

        val members = memberPattern.findAll(body).map { "${it.groupValues[1]}.${it.groupValues[2]}" }.distinct().toList()
        val bridges = bridgePattern.findAll(rest).map { "${it.groupValues[1]}.${it.groupValues[2]}" }.distinct().toList()
        return ScanResult(members, bridges)
    }

    /** 从 [openIndex] 处的 `{` 找配对的 `}`。 */
    private fun matchBrace(source: String, openIndex: Int): Int {
        var depth = 0
        var index = openIndex
        while (index < source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
            index++
        }
        error("大括号不配对（从 $openIndex 起）")
    }
}
