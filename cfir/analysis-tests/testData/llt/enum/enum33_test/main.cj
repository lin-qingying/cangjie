// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

import pkg.*

main() {
    // First part test for using instantiated decls that already instantiated in imported pkg.
    if (A1<Int64>.test(123) != 123) {
        return 1
    }
    if (test() != 0) {
        return 4
    }
    // Second part test for current package instantiation.
    if (A1<Int16>.test(123) != 123) {
        return 1
    }
    return 0
}
