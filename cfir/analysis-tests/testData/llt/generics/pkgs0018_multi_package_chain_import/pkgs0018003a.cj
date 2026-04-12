// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs0018001a
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0018001a/pkgs0018001a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs0018001a/pkgs0018001a.cj -o pkgs0018001a/pkgs0018001a.%middle
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0018001a/pkgs0018001a.%middle pkgs0018002a/pkgs0018002a.cj --import-path pkgs0018001a
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs0018001a/pkgs0018001a.%middle pkgs0018002a/pkgs0018002a.cj --import-path pkgs0018001a -o pkgs0018002a/pkgs0018002a.%middle
// DEPENDENCE: pkgs0018002a
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0018001a/pkgs0018001a.%middle pkgs0018002a/pkgs0018002a.%middle %n.cj --import-path pkgs0018001a --import-path pkgs0018002a
// (not not CJNATIVE) EXEC: %compiler %cmp_opt pkgs0018002a/pkgs0018002a.%middle pkgs0018001a/pkgs0018001a.%middle %n.cj --import-path pkgs0018001a --import-path pkgs0018002a -o %n.%suffix
// (not CJNATIVE) EXEC: %compiler %cmp_opt pkgs0018001a/pkgs0018001a.%middle pkgs0018002a/pkgs0018002a.%middle %n.cj --import-path pkgs0018001a --import-path pkgs0018002a -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

package pkgs0018003a

internal import pkgs0018002a.*

main(): Int64 {
    let ans = Bar<Int32>(1)
    if (ans == 1) {
        return 0
    }
    return 1
}
