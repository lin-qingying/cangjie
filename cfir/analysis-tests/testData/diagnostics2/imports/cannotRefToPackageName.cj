// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`import std.collection`（simple import）后裸用 `collection` →
// error: package name cannot be referred independently（锚包名 `collection`）。
// CFIR CfirCallResolver 只在 simple import 把裸名唯一绑定到包时构造 ConeCannotRefToPackageNameError。
// 官方 cjc 1.0.5 / 1.1.3 另给 sema_unused_import warning（锚整条 import 语句），本项目对应 UNUSED_IMPORT，未在此用例覆盖。
import std.collection

func f(): Unit {
    let x = <!CANNOT_REF_TO_PKG_NAME!>collection<!>
    let _ = x
}
