// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    func f(): Unit
}

extend Int64 <: I {
    public func f(): Unit {
        println("f in Extend Int64")
    }
}

main() {
    var i: I = 1

    match (i) {
        case x: Int64 =>
            for (y in 1..3) {
                y.f()
            }
            x.f()
        case _ => ()
    }
}
