// RUN_PIPELINE_TILL: FRONTEND
// NO_PRELUDE
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（`--no-prelude` 编译在类成员类型位置引用 `Object`，并在构造器里给它赋值）→ 三条诊断：
//   sema_undeclared_type_name "undeclared type name 'Object'"（锚 `Object` 首字母 1 个字符）—— 本用例唯一能以内联标记表达的
//   sema_no_core_object "`core` package should be imported"（锚首条声明的 `class` 关键字 1 个字符；`--no-prelude` 下每个文件都有这条，
//     与是否引用 `Object` 无关）；本项目 CfirDiagnosticsList 里没有对应诊断名
//   sema_core_object_not_found_when_no_prelude "class 'Object' of package 'std/core' is not found, cannot use '--no-prelude' option"
//     （**官方这条诊断没有位置**：JSON 里 Line = -1、File 为空），无法与内联标记的源码范围对应，本用例不覆盖。
// 形态说明（都是 cjc 1.0.5 / 1.1.3 实测）：postfix 形式 `Object?` 是 parse 错误（expected ';' found '?'）；前置 `?Object` 还会多报
//   `Option` 未声明；直接用 `= 0` 作初值时官方只报 sema_cannot_convert_literal（把未解析类型当已解析类型），拿不到 undeclared type name；
//   形参 / 返回类型位置的 `Object` 要真正被使用（如 `p.hashCode()`）才报；顶层 `class Object {}` 反而把 `Object` 变成新声明、不报。
//   故这里用"类成员类型 + init 里赋值"的形态。NO_PRELUDE 必须是模块级指令，故放在第一个 FILE: 段之前。

// FILE: memberTypeRef.cj
class C {
    var o: <!UNDECLARED_TYPE_NAME!>O<!>bject

    init() {
        o = 0
    }
}
