// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

var r1 = match (((), ())) {
    case (b: Int64, a: Int64) => 1
    case (a: Unit, b: Int64) => 1
    case (a: Int64, b: Unit) => 1
    case (b: Unit, a: Unit) => 0
    case _ => 1
}

main(): Int64 {
    if (r1 != 0) {
        return 1
    }
    return 0
}
