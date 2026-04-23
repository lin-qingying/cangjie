// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f1() {
    var a: Int64
    var b: Int64
    if ((a, b) = <!TYPE_MISMATCH!>(1, 2, 3)<!>) {
    }
    b - a - a
}

func f2() {
    var a: Int64
    var b: Int64
    if ((a, b) = <!TYPE_MISMATCH!>(1, 2, 3)<!>) {
        a = 1
    }
    b - a - a
}

func f3() {
    var a: Int64
    var b: Int64
    if ((a, b) = <!TYPE_MISMATCH!>(1, 2, 3)<!>) {
        b = 1
    }
    b - a - a
}

main() {
    f1()
    f2()
    f3()
}
