// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix | compare %f


interface xxx {
    func f(): Unit
}

extend Int64 <: xxx {
    public func f(): Unit {
        println("extend in Int64")
    }
}

extend Int8 <: xxx {
    public func f(): Unit {
        println("extend in Int8")
    }
}

main() {
    var a = 10
    var v: Array<xxx> = [a, a * a]

    match (v[1]) {
        case z: Int64 => z.f()
        case _ => println("wildcard.")
    }
}

/* SCAN
extend in Int64
*/
