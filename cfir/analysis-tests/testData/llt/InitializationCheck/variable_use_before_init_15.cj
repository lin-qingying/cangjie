// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f() {
    var b: Int32
    do {
        if (true) {
            continue
        }
        b = 1
    } while (<!USED_BEFORE_INITIALIZATION!>b<!> == 0)
    <!USED_BEFORE_INITIALIZATION!>b<!>
}

main() {
    println(f())
}
