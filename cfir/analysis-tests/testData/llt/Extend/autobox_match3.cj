// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan In f of extend TimeUnit!
// ASSERT: scan Some(1)

enum TimeUnit<T1, T2> {
    | Year(T1)
    | Month(T1, T2)
}

interface I {
    func f() {
        println("In f of interface!");
        return 1
    }
}

extend<T1, T2> TimeUnit<T1, T2> <: I {
    public func f() {
        println("In f of extend TimeUnit!");
        return 1
    }
}

main() {
    var time_Year: TimeUnit<Int64, Int32> = TimeUnit.Year(2021)

    let TimeUnit_2: Any = match (time_Year) {
        case x: I => x.f()
        case Year(y) => y
        case Month(y, m) => m
    }

    var a = TimeUnit_2 as Int64
    match (a) {
        case Some(2021) => println("Some(2021)")
        case Some(1) => println("Some(1)")
        case _ => println("WildCard")
    }
    return 0
}
