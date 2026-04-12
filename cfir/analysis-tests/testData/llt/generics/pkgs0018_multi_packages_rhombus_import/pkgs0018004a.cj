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
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0018001a/pkgs0018001a.%middle pkgs0018003a/pkgs0018003a.cj --import-path pkgs0018001a
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs0018001a/pkgs0018001a.%middle pkgs0018003a/pkgs0018003a.cj --import-path pkgs0018001a -o pkgs0018003a/pkgs0018003a.%middle
// DEPENDENCE: pkgs0018002a pkgs0018003a
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs0018001a/pkgs0018001a.%middle pkgs0018002a/pkgs0018002a.%middle pkgs0018003a/pkgs0018003a.%middle %n.cj --import-path pkgs0018001a --import-path pkgs0018002a --import-path pkgs0018003a
// (not not CJNATIVE) EXEC: %compiler %cmp_opt pkgs0018003a/pkgs0018003a.%middle pkgs0018002a/pkgs0018002a.%middle pkgs0018001a/pkgs0018001a.%middle %n.cj --import-path pkgs0018001a --import-path pkgs0018002a --import-path pkgs0018003a -o %n.%suffix
// (not CJNATIVE) EXEC: %compiler %cmp_opt  pkgs0018001a/pkgs0018001a.%middle pkgs0018002a/pkgs0018002a.%middle pkgs0018003a/pkgs0018003a.%middle %n.cj --import-path pkgs0018001a --import-path pkgs0018002a --import-path pkgs0018003a -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

package pkgs0018004a

internal import pkgs0018002a.run1
internal import pkgs0018003a.run2

main(): Int64 {
    let ans1 = run1()
    let ans2 = run1()
    let ans3 = run2()
    let ans4 = run2()

    if (ans1 == 1 && ans2 == 1 && ans3 == 2 && ans4 == 2) {
        return 0
    }
    return 1
}
