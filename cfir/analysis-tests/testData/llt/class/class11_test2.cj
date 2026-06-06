// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface A {
    func i() {}
    static func test(<!sema_cannot_have_default_param!>input!: Unit = <!sema_static_members_cannot_call_members!>t<!>his.i()<!>) {
        <!sema_static_members_cannot_call_members!>t<!>his.i()
    }
}

main(): Int64 {
    return 1
}
