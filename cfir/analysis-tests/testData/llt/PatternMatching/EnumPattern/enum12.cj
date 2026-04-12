// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum TimeUnit1 {
    One(TimeUnit2)
}

enum TimeUnit2 {
    Two(TimeUnit3)
}

enum TimeUnit3 {
    Three | Four
}

main(): Int64 {
    var time = TimeUnit1.One(TimeUnit2.Two(TimeUnit3.Three))
    var d = match (time) {
        case One(Two(Four)) => 1
        case One(Two(Three)) => 0
        case _ => -1
    }
    return d
}
