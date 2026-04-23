// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum Unit2 {
    u2_10(Int64)
    | u2_110(Int64, Int64)
    | u2_1110(Int64, Int64, Int64)
    | u2_1110(Int64, Int32, Int64, Int32)
}

main(): Int64 {
    var u2 = Unit2.u2_1110(10, 100, 1000)
    match (u2) {
        case <!VAR_IN_OR_PATTERN!>u2_10(x) | u2_110(x, z)<!> => <!UNRESOLVED_REFERENCE!>x<!>
        case <!VAR_IN_OR_PATTERN!>u2_10(z) | u2_110(y, _)<!> => 0
        case <!VAR_IN_OR_PATTERN!>u2_10(a) | u2_1110(_, a, _, _)<!> => 0
        case _ => 0
    }
    return 0
}
