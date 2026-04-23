// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

var a: Int64 = 5
var b = a
var c: Int64 = <!UNRESOLVED_REFERENCE!>d<!>
var e = while (<!TYPE_MISMATCH!>b<!>) {
    var c: Int32 = 1
    var f: Int64 = <!TYPE_MISMATCH!>c<!>
}

func F(a: Int32) {
    var g: Int64 = <!TYPE_MISMATCH!>a<!>
}
