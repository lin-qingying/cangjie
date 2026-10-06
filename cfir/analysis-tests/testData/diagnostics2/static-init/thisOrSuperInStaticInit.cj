// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   `static var s: Int64 = super.v`  → error: 'super' is not allowed to initialize static member（锚 `super` 首字母 1 个字符）
//   `static var b: Int64 = this.own`（own 为 prop）→ error: 'this' is not allowed to initialize static member（锚 `this` 首字母 1 个字符）
// CFIR CfirFieldVariableThisOrSuperInitializerChecker 把两条诊断都 reportOn 在 this/super 关键字本身，锚点一致。
open class Base {
    var v: Int64 = 0
}

class Child <: Base {
    prop own: Int64 {
        get() {
            return 1
        }
    }

    static var fromSuper: Int64 = <!THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_STATIC_MEMBER!>s<!>uper.v

    static var fromThis: Int64 = <!THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_STATIC_MEMBER!>t<!>his.own
}
