// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg pkg0
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg0/pkg0.cj -o pkg0/pkg0.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg0/pkg0.%middle pkg/pkg.cj --import-path pkg0 -o pkg/pkg.%middle
// (not not CJNATIVE) EXEC: %compiler %cmp_opt pkg/pkg.%middle pkg0/pkg0.%middle %n.cj --import-path pkg0 --import-path pkg -o %n.%suffix
// (not CJNATIVE) EXEC: %compiler %cmp_opt pkg0/pkg0.%middle pkg/pkg.%middle %n.cj --import-path pkg0 --import-path pkg -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkg.*

main(): Int64 {
    M()
    return 0
}
