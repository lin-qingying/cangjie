// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs0018001a
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs0018001a/pkgs0018001a.cj -o pkgs0018001a/pkgs0018001a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs0018002a/pkgs0018002a.cj -o pkgs0018002a/pkgs0018002a.%middle --import-path pkgs0018001a
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs0018003a/pkgs0018003a.cj -o pkgs0018003a/pkgs0018003a.%middle --import-path pkgs0018001a
// DEPENDENCE: pkgs0018002a pkgs0018003a
// EXEC: %compiler %cmp_opt pkgs0018001a/pkgs0018001a.%middle pkgs0018002a/pkgs0018002a.%middle pkgs0018003a/pkgs0018003a.%middle %n.cj --import-path pkgs0018001a --import-path pkgs0018002a --import-path pkgs0018003a -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

package pkgs0018004a

internal import pkgs0018001a.*
internal import pkgs0018002a.*
internal import pkgs0018003a.*

main(): Int64 {
    let a = C1<Int32>()
    return 0
}
