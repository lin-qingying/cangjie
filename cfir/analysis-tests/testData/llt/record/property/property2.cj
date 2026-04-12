// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct A {
    public var a: Int32 = 3
    public mut prop m: Int32 {
        get() {
            return a
        }
        set(v) {
            a = v
        }
    }
    public mut func f(v: Int32) {
        a = v
        0
    }
}

main(): Int64 {
    var a = A()
    a.m = 4
    if (a.m != 4) {
        return 1
    }
    return 0
}
