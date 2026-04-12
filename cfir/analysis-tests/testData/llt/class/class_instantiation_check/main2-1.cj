// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I1<T> {
    func i1(a: Int32): Unit
    func i2(a: T): Unit
}

interface I2 <: I1<Int32> {}

interface I3 <: I1<Int64> {}

class C1 <: I1<Int32> {
    public func i1(a: Int32): Unit {}
    public func i2(a: Int32): Unit {}
}

class C2 <: I1<Int64> {
    public func i1(a: Int32): Unit {}
    public func i2(a: Int64): Unit {}
}

main() {
    return 0
}
