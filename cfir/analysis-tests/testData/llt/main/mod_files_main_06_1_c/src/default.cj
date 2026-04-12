// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../mod1
// EXEC: %compiler %cmp_opt %compile_lib_opt ./mod1/src/p1/*.cj --module-name mod1 -o mod1/a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt ./mod1/src/*.cj mod1/a.%middle --module-name mod1 -o mod1/default.%middle
// EXEC: %compiler --verbose %cmp_opt mod1/a.%middle mod1/default.%middle %f -o %output

internal import mod1.p1.*
internal import mod1.default.*

main(): Int64 {
    var a = foo(Int8(1))
    if (a != 11) {
        return 1
    }
    return 0
}
