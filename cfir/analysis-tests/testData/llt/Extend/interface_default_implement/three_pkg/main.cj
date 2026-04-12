// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1 pkg2
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg1/pkg1.cj -o pkg1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg2/pkg2.cj -o pkg2.%middle
// (not not CJNATIVE) EXEC: %compiler %cmp_opt %n.cj pkg2.%middle pkg1.%middle -o %n.%suffix
// (not CJNATIVE) EXEC: %compiler %cmp_opt %n.cj pkg1.%middle pkg2.%middle -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkg2.*

main(): Int64 {
    return 0
}
