// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -warnings

enum A {
    A1(Int32) | A2
}

enum AA {
    A1
}

func A1() {}

main(): Int64 {
    var a: AA = <!TYPE_MISMATCH!>A1<!>
    var A2 = A2 // left A2 is enum constructor.
    var b = match (A2) {
        case A2 => 0
        case _ => -1
    }

    var c = match (a) {
        case A1 => 0
        case _ => -1
    }

    return b
}
