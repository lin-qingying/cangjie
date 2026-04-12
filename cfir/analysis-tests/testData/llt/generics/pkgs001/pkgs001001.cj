// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs001001a
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs001001a/pkgs001001a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs001001a/pkgs001001a.cj -o pkgs001001a/pkgs001001a.%middle
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs001001a/pkgs001001a.%middle %n.cj --import-path pkgs001001a
// EXEC: %compiler %cmp_opt pkgs001001a/pkgs001001a.%middle %n.cj --import-path pkgs001001a -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// Test condition: package with a single generic function is imported

internal import pkgs001001a.foo

main(): Int64 {
    let res = foo<Int32>(3)
    if (res == 3) {
        return 0
    } else {
        return 1
    }
}
