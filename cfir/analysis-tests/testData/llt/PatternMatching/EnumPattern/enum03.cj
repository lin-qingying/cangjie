// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args
// Assert scan 1

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
}

main(): Int64 {
    var time = Year(1)
    var b = match (time) {
        case Month(1, 2) => 1
        case Year(c) =>
            print(c.toString())
            0
        case _ => -1
    }
    return b
}
