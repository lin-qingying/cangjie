// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {
    public var a: Int64 = 1
    public static var c: Int64 = 2

    public var b: () -> Int64 = {=> <!sema_illegal_usage_of_member!>a<!>}
    public func f() {
        var d: Int64 = 1
        a
        b()
        var c: () -> Int64 = <!sema_use_func_capture_var_alone!>{=> d}<!>
        func ff() {
            var b: Int64 = a
        }
        return a
    }
    public static func f1() {
        c
    }
}

main(): Int64 {
    return 0
}
