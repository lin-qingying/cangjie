// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan Some(10)

interface I {
    func f() {
        println("In f of interface!");
        return 1
    }
}

extend Int64 <: I {
    public func f() {
        println("In f of extend Int64!");
        return 1
    }
}

main() {
    var x = 10

    let TimeUnit_2: I = match {
        case x > 10 => x
        case x <= 10 => 10
        case _ => 0
    }

    var a = TimeUnit_2 as Int64
    match (a) {
        case Some(10) => println("Some(10)")
        case Some(x) => println("Some(x)")
        case _ => println("WildCard")
    }

    return 0
}
