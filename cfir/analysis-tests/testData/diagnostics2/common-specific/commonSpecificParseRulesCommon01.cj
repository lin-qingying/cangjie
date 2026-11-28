// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// CJMP 解析族（common 侧，官方 ParseCJMPDecl.cpp / ParseDecl.cpp:1974-1980）：
// cjc 1.1.3 实测触发面——common 类/结构体必须有显式构造器、模式声明、static init、
// 成员与外层修饰符一致、函数缺返回类型（锚函数名）。
common class NoCtor {}

common struct NoCtorS {}

// common 模式声明由共享 parser 报告，不再定义 CFIR 语法诊断。
common let (a, b) = (1, 2)

class Plain2 {
    common func f(): Unit {}
}

class WithStatic {
    common static init() {}
}

// 缺返回类型由共享 parser 报告；本文件只记录 CFIR 语义诊断。
common func NoRet()
