// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    return case01()
}

func case01() {
    let a = 0..10 : 1
    let b = 0..10 : 1
    var result = match (a) {
        case b => 0
        case _ => 1
    }
    return result
}
