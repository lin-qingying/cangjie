// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg1/a.cj -o pkg1.%middle %enableO2
// EXEC: %compiler %cmp_opt %f pkg1.%middle -o %output %enableO2
// RUN-EXEC: %run %run_opt %output %run_args

internal import pkg1.*

main() {
    var en1: enum1 = enum1.A(enum1.foo2<UInt32>(enum1.foo1() + enum1.foo1()))
    var en2: enum1 = enum1.A(enum1.foo3())
    var en3: enum2 = enum2.A(enum2.foo2<UInt32>(enum2.foo1()))
    var en4: enum2 = enum2.A(enum2.foo3())
    var en5: enum3 = enum3.A(enum3.foo2<UInt32>(enum3.foo1()))
    var en6: enum3 = enum3.A(enum3.foo3())

    var e1 = match (en1) {
        case A(x) => x
    }
    var e2 = match (en2) {
        case A(x) => x
    }
    var e3 = match (en3) {
        case A(x) => x
    }
    var e4 = match (en4) {
        case A(x) => x
    }
    var e5 = match (en5) {
        case A(x) => x
    }
    var e6 = match (en6) {
        case A(x) => x
    }

    if (e1 == 1 && e2 == 2 && e3 == 2 && e4 == 4 && e5 == 3 && e6 == 6) {
        return 0
    } else {
        return 1
    }
}
