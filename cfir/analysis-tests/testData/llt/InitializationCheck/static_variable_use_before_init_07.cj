// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {
    static prop test: Int64 {
        get() {
            <!USED_BEFORE_INITIALIZATION!>c<!>
        }
    }
    mut static prop foo: Int64 {
        get() {
            1
        }
        set(v) {
            <!USED_BEFORE_INITIALIZATION!>b<!>
        }
    }
    static var b: Int64
    static var c: Int64 = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>test<!>
    static init() {
        b = if (true) {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<!> = 2
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<!>
        } else {
            2
        }
    }
}

main() {}
