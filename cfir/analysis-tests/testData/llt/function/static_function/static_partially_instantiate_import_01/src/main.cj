// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../src
// EXEC: %compiler %cmp_opt %compile_lib_opt src/pkga/a.cj -o a.%middle
// EXEC: %compiler %cmp_opt a.%middle src/%f -o %output
// EXEC: %run %run_opt %output %run_args

internal import pkga.Boo

main(): Int64 {
    if (Boo<Int32>.foo<Bool>(1, true)) {
        return 0
    }
    return 1
}
