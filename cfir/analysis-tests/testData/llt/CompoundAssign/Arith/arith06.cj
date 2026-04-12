// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    var a: Float32 = 1.0
    a += 2.0
    a += 3.14
    var b = a + 1.0
    if (b - 7.14 < 0.0001) {
        return 0
    }
    return 1
}
