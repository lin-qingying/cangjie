// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func rtTrue() {
    return true
}

main(): Int64 {
    var res = match {
        case false && false => 1
        case rtTrue() => 0
        case 1 > 2 => 3
        case 1 > 2 && false => 4
        case _ => 5
    }
    return res
}
