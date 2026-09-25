// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7b.txt）；替换原单文件占位 fixture
// common interface 与 specific class：同上
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common interface Printable {
    func print(): Unit
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific class Printable {}<!>
