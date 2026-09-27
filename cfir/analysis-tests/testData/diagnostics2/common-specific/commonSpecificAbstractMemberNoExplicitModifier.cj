// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C08）：common abstract 类中无体且
// 未写 common/abstract 的成员报 `'common' abstract class function must have explicit 'common' or 'abstract' modifier`，
// 锚成员名（官方 DeclAttributeChecker::CheckCJMPAttributesForPropAndFuncDeclInClass）。
package cjmp_p

public common abstract class A {
    public common init()
    public func <!CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER!>f<!>(): Int64
}
