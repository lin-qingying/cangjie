// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan func f in Extend B
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
        println("func f in Extend B");
        1
    }
}

main(): Int64 {
    var m: I = B()

    let res: Int64 = match (m) {
        case x: B =>
            var y: Option<B> = x
            match (y) {
                case Some(z) =>
                    z.f()
                    z.g()
                case _ => 0
            }
        case _ => 0
    }

    res
}
