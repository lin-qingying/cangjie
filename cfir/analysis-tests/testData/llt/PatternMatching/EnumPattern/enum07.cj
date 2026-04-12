// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args
// Assert scan 1,2,3

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d = match (time) {
        case Year(a) => a
        case Month(a, b) => a + b
        case Day(a, b, c) =>
            print(a.toString() + "," + b.toString() + "," + c.toString())
            a + b + c
        case Hour(1, 2, 3, 4) => Int32(-1)
        case Hour(a, b, c, d) => a + b + c + d
    }
    if (d == 6) {
        return 0
    }
    return 1
}
