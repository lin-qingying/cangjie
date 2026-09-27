// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C01）：common open 类无构造器且被
// 普通类继承时，只报解析族 `at least one constructor is required in common class 'A'`（锚类名）；解析错误先于 sema，
// `sema_common_open_class_no_init`（官方 PreCheckCJMPClass）在 1.1.3 无可达触发点。
package cjmp_p

public common open class <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>A<!> {}

public class B <: A {
    public init() {}
}
