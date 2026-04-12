// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import b.*

main() {
    var a = A()
    var res = a.a // 1
    a.a = 2
    res += a.a
    if (res != 3) {
        return 1
    }
    return 0
}
