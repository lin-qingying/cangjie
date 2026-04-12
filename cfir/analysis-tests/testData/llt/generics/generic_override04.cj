// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix 2>&1 | compare %f
// ASSERT: scan here

interface I1 {
    func foo(): Int64
}

interface I3 {
    public func goo(): I1
}

class CA<T> <: I3 where T <: I1 {
    var x: T
    init(_x: T) {
        x = _x
    }
    public func goo(): I1 {
        println("here")
        x
    }
}

struct TEST_1 <: I1 {
    public func foo(): Int64 {
        0
    }
}

main() {
    CA<TEST_1>(TEST_1()).goo()
    0
}
