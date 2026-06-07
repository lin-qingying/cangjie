// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I0 {
    static func foo(): Int64 {
        return 1
    }

    func foo1(): Int64 {
        var res = <!sema_object_cannot_access_static_member!>t<!>his.foo()
        return res
    }
}

class C0 <: I0 {}

main(): Int64 {
    return 0
}
