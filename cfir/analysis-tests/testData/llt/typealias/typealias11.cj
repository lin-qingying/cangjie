// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum TimeUnit<T> {
    Year(T) | Month
}

type Time<T> = TimeUnit<T>

main(): Int64 {
    var time: Time<Int32> = TimeUnit<Int32>.Year(2020)
    var res: Int32 = match (time) {
        case Year(x) => x
        case Month => 11
    }
    if (res == 2020) {
        return 0
    }
    return 1
}
