// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {
    public var a: Int64 = 1
    public mut open prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

class B <: A {
    public mut override prop i: Int64 {
        set(v) {}
    }
}

main(): Int64 {
    if (A().i != 1) {
        return 3
    }
    if (B().i != 1) {
        return 4
    }
    return 0
}
