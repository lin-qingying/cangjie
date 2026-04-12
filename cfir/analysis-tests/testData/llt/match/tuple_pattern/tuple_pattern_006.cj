// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum TimeUnit1 {
    Year(String)
}

main(): Int64 {
    let yearTuple = ((Year("Allen"), "Allen"), (Year("def"), 80))
    let allen = "Allen"
    let num80 = 80
    return match (yearTuple) {
        case ((Year("Allen"), "Allen"), (Year("def"), 80)) => 0
        case (_, _) => 1
    }
}
