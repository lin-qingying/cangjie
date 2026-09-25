// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7b.txt）；替换原单文件占位 fixture
// common interface 与 specific struct：只报种类诊断——interface 成员未标 common，接口整体视为自带默认实现（COMMON_WITH_DEFAULT），不要求配对
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common interface Config {
    func ready(): Bool
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific struct Config {}<!>
