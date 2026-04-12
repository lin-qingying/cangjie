// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {
    public static var x: Int64 = 5
    public mut static prop v: Int64 {
        get() {
            return x
        }
        set(v) {
            x = v
        }
    }
}

class B <: A {
    public mut static redef prop v: Int64 {
        set(x) {
        }
    }
}

class C <: A {
    public mut static redef prop v: Int64 {
        get() {
            x + 1
        }
    }
}

main(): Int64 {
    C.v = 2
    if (A.v == 2 && B.v == 2 && C.v == 3) {
        print("${A.v}\n")
        print("${B.v}\n")
        print("${C.v}\n")
        return 0
    }
    return 1
}
