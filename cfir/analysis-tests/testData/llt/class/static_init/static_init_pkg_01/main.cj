// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %f -o %output
// EXEC: %run %run_opt %output
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle %enableO2
// EXEC: %compiler %cmp_opt pkg.%middle %f -o %output %enableO2
// EXEC: %run %run_opt %output

internal import pkg.*

let v = A.r

main() {
    if (A.a != 1) {
        return 1
    }
    if (v.start != A.a || v.end != 5) {
        return 2
    }
    return 0
}
