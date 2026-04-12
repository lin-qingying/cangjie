// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func test(): Int64 {
    var i = 0
    let a = 1
    let b = 2
    var res = match (i) {
        case 1 | -2 | 0 where false => 1
        case 2 => 0
        case _ => -1
    }
    if (res == -1) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
