// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
    | Twounit(TimeUnit, TimeUnit)
}

main(): Int64 {
    var year = Year(1)
    var month = Month(2, 3)
    var day = Day(1, 2, 3)
    var time = Twounit(year, month)
    var d = match (time) {
        case Twounit(day, month) => 1
        case Twounit(year, month) => 0
        case _ => -1
    }
    if (d == 1) {
        return 0
    }
    return 1
}
