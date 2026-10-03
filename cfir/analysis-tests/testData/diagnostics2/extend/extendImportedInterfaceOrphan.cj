// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测（三包：pa 定义接口 I/J，pb 定义继承 pa.Base 的 Derived，pc extend 导入类型）：
//   pc 中 `extend Derived <: I { public func i(): Int64 { return 2 } }`
//   → error: imported type 'Derived' cannot extend imported interface（锚 extend 的目标类型名 `Derived`）
//   → error: implementation of function or property is needed in 'extend Derived'（kind sema_need_member_implementation，
//     锚 `extend` 首字母 1 个字符，附注未实现的接口函数 j）
//   pa.I / pa.J 的接口成员写了显式 `public`：官方 cjc 1.0.5 / 1.1.3 对此给 parse_redundant_modifier warning
//     （接口成员隐式 public），故两处 `public` 加 REDUNDANT_MODIFIER 标记。
//   同样条件下 `extend Derived { ... }`（不写继承接口）零诊断 —— 孤儿规则只看 extend 自己引入的接口。
// 官方实现 TypeChecker::CheckExtendOrphanRule：被扩展类型是 imported（或 builtin）且 extend 引入的接口
// 存在其他包声明的父类型时报 sema_type_cannot_extend_imported_interface。
// FILE: iface.cj
package pa

public interface J {
    <!REDUNDANT_MODIFIER!>public<!> func j(): Int64
}

public interface I <: J {
    <!REDUNDANT_MODIFIER!>public<!> func i(): Int64 {
        return 1
    }
}

public open class Base {
    public init() {}
}

// FILE: derived.cj
package pb

import pa.Base

public class Derived <: Base {
    public init() {
        super()
    }
}

// FILE: orphan.cj
package pc

import pb.Derived
import pa.I

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>Derived<!> <: I {
    public func i(): Int64 {
        return 2
    }
}
