// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

type Opt<T> = Option<T>

main(): Int64 {
    var time: Opt<Int32> = Option<Int32>.Some(2020)
    var res: Int32 = match (time) {
        case Some(x) => x
        case None => 11
    }
    if (res == 2020) {
        return 0
    }
    return 1
}
