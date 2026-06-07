// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A2 {}

enum A {
    A1(Int32) | A2
}

enum AA {
    A1 | A3 | A4 | A5
}

func A1() {}

let A3 = 1

main(): Int64 {
    var a = A1
    var A2 = <!sema_ref_not_be_type!>A<!>2
    var b = match (A2) {
        case <!sema_pattern_not_match!>A<!>1(x) => 0
        case _ => -1
    }
    let A4 = 1
    var a1 = AA.A1
    var c = match (a1) {
        case A3 => 0
        case _ => -1
    }
    var aa = AA.A1
    var d = match (aa) {
        case A4 => 0
        case _ => -1
    }
    let A5 = AA.A1
    var e = match (aa) {
        case A5 => 0
        case _ => -1
    }

    return 0
}
