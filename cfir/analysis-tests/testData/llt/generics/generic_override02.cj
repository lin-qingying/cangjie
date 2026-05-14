// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {
    func foo(): Int64
}

interface I3<T> {
    func goo(): T
}

class CA<T> <: I3<I1> where T <: I1 {
    var x: T
    init(_x: T) {
        x = _x
    }
    public func <!OVERRIDING_RETURN_TYPE_MISMATCH!>goo<!>(): T {
        println("here")
        x
    }
}

struct TEST_1 <: I1 {
    public func foo(): Int64 {
        0
    }
}

main() {}
