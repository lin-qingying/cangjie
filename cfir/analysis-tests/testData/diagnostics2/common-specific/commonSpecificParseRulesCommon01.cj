// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// CJMP 解析族（common 侧，官方 ParseCJMPDecl.cpp / ParseDecl.cpp:1974-1980）：
// cjc 1.1.3 实测触发面——common 类/结构体必须有显式构造器、模式声明、static init、
// 成员与外层修饰符一致、函数缺返回类型（锚函数名）。
common class <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>NoCtor<!> {}

common struct <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>NoCtorS<!> {}

common let <!PARSE_CJMP_PATTERN_DECL!>(a, b)<!> = (1, 2)

class Plain2 {
    <!PARSE_CJMP_OUTDECL_MISS_MATCH!>common func f(): Unit {}<!>
}

class WithStatic {
    <!PARSE_CJMP_STATIC_INIT!>common static init() {}<!>
}

common func <!PARSE_COMMON_FUNCTION_MUST_HAVE_RETURN_TYPE!>NoRet<!>()
