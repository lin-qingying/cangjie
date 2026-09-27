// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `cjc a.cj --experimental --output-type=chir` 实测：含 common 声明的文件即 common package part
// 文件（官方 File::isCommon），其中的 main 报 `main function cannot be used in common package part`，
// 锚 main 声明，与 main 位于 common 声明之前或之后无关。
package cjmp_p

<!COMMON_PACKAGE_HAS_MAIN!>main(): Int64 {
    0
}<!>

public common func f(): Int64
