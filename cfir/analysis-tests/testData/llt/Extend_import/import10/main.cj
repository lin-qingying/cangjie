// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt %enableO2 pkg.%middle %f -o %output
// EXEC: %run %run_opt %output

internal import pkg.*

main(): Int64 {
    let a = fff([1])
    if (a != 111) {
        return 1
    }
    return 0
}
