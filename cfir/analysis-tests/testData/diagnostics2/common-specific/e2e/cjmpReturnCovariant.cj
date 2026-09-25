// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7c.txt）
// specific 返回类型为 common 返回类型的子型：配对成功（官方 IsFuncDeclSubType 返回协变）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public open class Base {}
public class Derived <: Base {}
public common func r(): Base

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific func r(): Derived { Derived() }
