// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    var b = R<!WRONG_NUMBER_OF_ARGUMENTS!>(a: 6)<!>
    b.c(7)
    return Int64(b.a - 8)
}

interface I {
    func c(b: Int32): Int32
    func cc(): Int32
}

struct R {
    public var a: Int32 = 8
    public mut func c(b: Int32): Int32 {
        let c = <!UNRESOLVED_REFERENCE!>c<!>c
        a = c() + b
        return a
    }
}

<!sema_incompatible_mut_modifier_between_struct_and_interface!>e<!>xtend R <: I {
    public func cc(): Int32 {
        return 1
    }
}
