// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    var tup: (Int64, Int64) = (444, 888)
    var removedOpt: Option<Int64> = tup[1]
    if ((removedOpt ?? -1) != 888) {
        return 1
    }
    return 0
}
