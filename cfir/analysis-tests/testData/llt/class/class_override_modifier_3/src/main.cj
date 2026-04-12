// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../src
// EXEC: %compiler %cmp_opt %compile_lib_opt src/pkgs/pkgs.cj -o pkgs.%middle
// EXEC: %compiler %cmp_opt pkgs.%middle %f -o %output
// EXEC: %run %run_opt %output %run_args 

internal import pkgs.*

main(): Int64 {
    var res = C3().foo()
    if (res == 3) {
        return 0
    }
    return 1
}
