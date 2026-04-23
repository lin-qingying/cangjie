// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class cls {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>a<!>: Int64
    static init() {
        func f() {
            a = 1
        }
        f()
    }
}

main(): Int64 {
    var a: Int64
    func f() {
        <!CAPTURE_BEFORE_INITIALIZATION!>a<!> = 1
    }
    f()
    println(<!USED_BEFORE_INITIALIZATION!>a<!>)

    let b: Int64
    {=> <!CAPTURE_BEFORE_INITIALIZATION!>b<!> = 2}
    println(<!USED_BEFORE_INITIALIZATION!>b<!>)
    return 1
}
