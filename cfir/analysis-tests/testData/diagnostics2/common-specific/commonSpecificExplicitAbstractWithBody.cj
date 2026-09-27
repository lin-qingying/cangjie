// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C09）：CJMP 类中显式 abstract 的
// 成员带体报 `abstract function can not have body`（锚成员声明）。
package cjmp_p

public common abstract class A {
    public common init()
    <!EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY!>public abstract func f(): Int64 {
        1
    }<!>
}
