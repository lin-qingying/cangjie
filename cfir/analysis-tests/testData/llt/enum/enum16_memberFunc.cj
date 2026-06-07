// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum ET {
    A
    | B(String)
    | C(String, String)

    public func foo1(): Int64 {
        print("function foo1 defined in enum\n")
        return 1
    }

    public func foo1(a: Int64): Int64 {
        print("function foo1(Int64) defined in enum\n")
        return 1
    }

    private func foo2(): Int64 {
        print("function foo2 defined in enum\n")
        return 1
    }

    public static func foo3(): Int64 {
        print("function foo3 defined in enum\n")
        return 1
    }
}

main() {
    let e1: ET = ET.A
    let e2: ET = ET.B("one")
    e1.<!sema_no_match_function_declaration_for_call!>f<!>oo2()
    <!sema_object_cannot_access_static_member!>e<!>2.foo3()
    <!sema_illegal_access_non_static_member!>E<!>T.foo1()
    <!sema_illegal_access_non_static_member!>E<!>T.foo1(1)

    return 0
}
