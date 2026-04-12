// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs0010001a
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0010001a/pkgs0010001a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs0010001a/pkgs0010001a.cj -o pkgs0010001a/pkgs0010001a.%middle
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0010001a/pkgs0010001a.%middle %n.cj --import-path pkgs0010001a
// EXEC: %compiler %cmp_opt pkgs0010001a/pkgs0010001a.%middle %n.cj --import-path pkgs0010001a -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkgs0010001a.*

main(): Int64 {
    var value = Test<Int32>(1)
    var b = value.member1 + 10

    if (value.member1 == 1 && b == 11) {
        return 0
    } else {
        return 1
    }
}
