// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum Time {
    Day(Int32)
}

main(): Int64 {
    var time = Day(3)
    var res: Int32 = match (time) {
        case Day(x) =>
            x
            return 0
    }
    return 1
}
