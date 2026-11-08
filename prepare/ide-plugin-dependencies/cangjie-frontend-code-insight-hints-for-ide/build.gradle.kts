plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :code-insight:hints。"

/*
 * 只打 hints 自身 jar：它依赖的 `:analysis:analysis-api`、`:psi`、`:common` 在 IDE 插件
 * 类路径上已由 `cangjie-frontend-analysis-api-for-ide` / `cangjie-frontend-psi-for-ide` 等
 * 工件提供，重复打包只会让同一批类出现两份。
 */
publishCangjieJarsForIde(
    listOf(
        ":code-insight:hints",
    ),
)
