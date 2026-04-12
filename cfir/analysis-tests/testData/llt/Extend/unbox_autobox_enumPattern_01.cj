// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan In extend B!
// ASSERT: scan func g in B

class B {
    func g(): Int64 {
        println("func g in B");
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
    var m: E = F(B(), B())

    let res: Int64 = match (m) {
        case F(x: I, y: B) =>
            x.f();
            y.f();
            y.g()
        case _ => 0
    }

    res
}

enum E {
    C(B) | F(B, I)
}
