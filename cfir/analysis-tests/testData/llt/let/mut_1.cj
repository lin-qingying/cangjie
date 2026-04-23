// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.x<!> = a.x + 1
    println(a.y)
}

func foo<T>(t: T): Unit where T <: IA {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>t.x<!> = t.x + 1
    println(t.y)
}
main() {}
