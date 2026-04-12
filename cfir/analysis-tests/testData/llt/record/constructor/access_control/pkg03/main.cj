// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %pkg_opt %compile_lib_opt pkg -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %n.cj -o %output
// EUN-EXEC: %run %run_opt %output %run_args

internal import pkg.*

main(): Int64 {
    if (run() != 10) {
        return 1
    }
    return 0
}
