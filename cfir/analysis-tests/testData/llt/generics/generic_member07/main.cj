// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkg.*

main() {
    if (A1<Int64>.foo<Int64>(12) != 12) {
        return 1
    }
    test()
    if (A1<Int16>.foo<Rune>(r'a') != r'a') {
        return 2
    }
    return 0
}
