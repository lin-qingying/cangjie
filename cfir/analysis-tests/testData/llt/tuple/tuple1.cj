// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt %f -o %output
// EXEC:%run %run_opt %output %run_args

main(): Int64 {
    var a: (Int64, Int64, Int64, Int64) = (0, 1, 2, 3)
    var b = (5, 6, 7)
    var c: (Int64, Int64, Int64) = b
    var d = (8, a[0], 9)
    var e: (Int64, (Int64, Int64)) = (11, (22, 33))
    return a[0]
}
