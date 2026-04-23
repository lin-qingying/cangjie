// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f() {
    1 < 2
}

main() {
    var a: Int64
    var b: Int64
    if (f()) {
    } else if ((a, b) = <!TYPE_MISMATCH!>(1, 2, 3)<!>) {
    }
    <!USED_BEFORE_INITIALIZATION!>b<!> - a - a
}
