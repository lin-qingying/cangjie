// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE-11: %run %output | compare %f
// ASSERT: scan In extend B!
// ASSERT: scan func g in B

class B {
    func g(): Int64 {
        println("func g in B!")
        10
    }
}

interface I {
    func f(): Int64
}

extend B <: I {
    public func f() {
        println("In extend B!");
        1
    }
}

main(): Int64 {
    var m: E = C(B())

    let res: Int64 = match (m) {
        case C(x: B) => x.f()
        case _ => 0
    }

    var n: E = F(B(), B())

    let res2: Int64 = match (n) {
        case F(x: I, y: B) =>
            x.f()
            y.f()
            y.g()
        case _ => 0
    }

    res + res2
}

enum E {
    C(I) | F(B, I)
}
