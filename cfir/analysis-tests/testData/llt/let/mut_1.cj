// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRORCHECK: %compiler %cmp_opt %f
// ASSERT: scan-2 error: cannot assign to immutable value

interface IA {
    mut prop x: Int64
    prop y: Int64
}

struct A <: IA {
    public mut prop x: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    public prop y: Int64 {
        get() {
            1
        }
    }
}

func goo(a: A): Unit {
    a.x = a.x + 1
    println(a.y)
}

func foo<T>(t: T): Unit where T <: IA {
    t.x = t.x + 1
    println(t.y)
}

main() {}
