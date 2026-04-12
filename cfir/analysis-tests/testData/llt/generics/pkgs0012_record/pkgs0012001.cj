// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs0012001a
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0012001a/pkgs0012001a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs0012001a/pkgs0012001a.cj -o pkgs0012001a/pkgs0012001a.%middle
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0012001a/pkgs0012001a.%middle %n.cj --import-path pkgs0012001a
// EXEC: %compiler %cmp_opt pkgs0012001a/pkgs0012001a.%middle %n.cj --import-path pkgs0012001a -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkgs0012001a.Test

main(): Int64 {
    var value = Test<Int32>(1, 10)
    var b = value.member1 + value.member2
    var c = value.testNormal(false)
    var d = value.testGeneric1(123)
    var e = value.testGeneric2(123, true)
    if (value.member1 == 1 && value.member2 == 10 && b == 11 && c == false && d == 123 && e == true) {
        return 0
    } else {
        return 1
    }
}
