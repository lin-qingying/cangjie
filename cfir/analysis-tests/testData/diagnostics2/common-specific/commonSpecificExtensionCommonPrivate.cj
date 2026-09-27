// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C05）：无接口 common extend 中的
// `common private` 成员只报 `'common' and 'private' modifier conflict on function 'g' declaration`（锚成员声明），
// 不报通用修饰符冲突。
package cjmp_p

public class C {
    public init() {}
}

common extend C {
    <!COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS!>common private func g(): Int64 { 1 }<!>
}
