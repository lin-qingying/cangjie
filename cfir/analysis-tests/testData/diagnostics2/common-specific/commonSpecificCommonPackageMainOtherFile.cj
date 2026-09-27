// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `cjc d1.cj d2.cj --experimental --output-type=chir` 实测零诊断（d1 = 本例 part.cj，d2 = entry.cj）：
// common package part 是文件级属性（官方 File::isCommon），同包内不含 common 声明的文件可以声明 main。
// FILE: part.cj
package cjmp_p

public common func f(): Int64

// FILE: entry.cj
package cjmp_p

main(): Int64 {
    0
}
