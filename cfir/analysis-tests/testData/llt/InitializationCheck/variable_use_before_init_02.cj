// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    var a: Int64
    var b: Int64
    if ((1, <!USED_BEFORE_INITIALIZATION!>b<!>) == (1, 2)) {
        print(1)
    }
    if (<!USED_BEFORE_INITIALIZATION!>b<!> == 1) {
        a = 1
    }
    return <!USED_BEFORE_INITIALIZATION!>b<!> - a - a
}
