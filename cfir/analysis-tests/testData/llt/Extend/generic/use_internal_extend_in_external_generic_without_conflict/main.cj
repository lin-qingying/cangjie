// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1 pkg2
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg1/pkg1.cj -o pkg1.o
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg2/pkg2.cj -o pkg2.o
// EXEC: %compiler %cmp_opt pkg1.o pkg2.o %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkg1.*
internal import pkg2.*

main() {
    return f<Int64>() - 1
}
