// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {
    public mut open prop v: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

class B <: A {
    public mut override prop v: Int64 {
        set(v) {
        }
    }
    public func f() {
        return v + 1
    }
}

main(): Int64 {
    if (A().v == 2 && B().v == 2 && B().f() == 3) {
        print("${A().v}\n")
        print("${B().v}\n")
        return 0
    }
    return 1
}
