// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan-3 1111
// ASSERT: scan-2 2222

interface I {
    func f0(): Unit
    func f() {
        f0()
    }
}

extend<T> CPointer<T> <: I where T <: CType {
    public func f0() {
        println("1111")
    }

    public func f2() {
        f()
    }
}

class A<T> where T <: I {
    func h(a: T) {
        a.f()
    }
}

extend Int64 <: I {
    public func f0() {
        println("2222")
    }

    public func f2() {
        f()
    }
}

main(): Int64 {
    // ----member access
    // Generic
    var a = A<CPointer<Unit>>()
    a.h(CPointer<Unit>()) // 1111

    // built-in
    var b = CPointer<Unit>()
    b.f() // 1111

    // primitive
    10.f() // 2222

    // ----ref
    b.f2() // 1111
    10.f2() // 2222
    0
}
