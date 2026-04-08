package org.cangnova.cangjie.analysis.api.renderer.base

/**
 * 关键字渲染器。
 */
fun interface CaKeywordsRenderer {
    fun renderKeyword(keyword: String, printer: CaPrettyPrinter)

    companion object {
        val AS_WORD: CaKeywordsRenderer = CaKeywordsRenderer { keyword, printer ->
            printer.append(keyword)
        }
    }
}
