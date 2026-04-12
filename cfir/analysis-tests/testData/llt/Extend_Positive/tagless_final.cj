// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: scan -3
// ASSERT: scan 9

interface ExprSYM<repr> {
    func add(t2: repr): repr
    func neg(): repr
    func lit(): repr
}

extend Int64 <: ExprSYM<Int64> {
    public func add(t2: Int64): Int64 {
        this + t2
    }
    public func neg(): Int64 {
        -this
    }
    public func lit(): Int64 {
        this
    }
}

interface MulSYM<repr> {
    func mul(t2: repr): repr
}

extend Int64 <: MulSYM<Int64> {
    public func mul(t2: Int64): Int64 {
        this * t2
    }
}

// term1 : ExprSym<Int64>
let term1 = 1.lit().neg() // -1
let term2 = 2.lit().neg() // -2
let term3 = term1.add(term2) // -3
let term4 = term3.mul(term3) // 9

main(): Int64 {
    println(term3.toString())
    println(term4.toString())
    0
}
