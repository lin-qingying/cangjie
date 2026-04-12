// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan WildCard

enum TimeUnit<T1, T2> {
    | Year(T1)
    | Month(T1, T2)
    | Day(T1, T2, T2)
    | Hour(T1, T2, T2, T2)
}

main() {
    var time_Year: TimeUnit<Int64, Int32> = TimeUnit.Year(2021)

    let TimeUnit_2: Any = match (time_Year) {
        case Year(y) => y
        case Month(y, m) => m
        case Day(y, m, d) => d
        case Hour(y, m, d, h) => h
    }

    var a = TimeUnit_2 as TimeUnit<Int64, Int32>
    var b = match (a) {
        case Some(v) =>
            match (v) {
                case Month(y, m) =>
                    println(y);
                    println(m);
                    ()
                case _ => ()
            }
            println("Some")
        case _ => println("WildCard")
    }
    return 0
}
