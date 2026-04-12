// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

main() {
    var a = if (let a <- 1) {
        1
    }
    var res = match (a) {
        case x: Unit => 0
        case _ => 1
    }
    return res
}
