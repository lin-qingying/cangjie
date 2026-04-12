// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRORCHECK: %compiler %cmp_opt %f
// ASSERT: scan-2 error: cannot use mutable function on immutable value

interface IA {
    mut func f1(): Unit
    func f2(): Unit
}

struct A <: IA {
    public mut func f1(): Unit {
        println("f1")
    }
    public func f2(): Unit {
        println("f2")
    }
}

func goo(a: A): Unit {
    a.f1()
    a.f2()
}

func foo<T>(t: T): Unit where T <: IA {
    t.f1()
    t.f2()
}
