// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Class {
    static var b: Int64 = 1
}

struct A<T> where T <: Class {
    static prop c: Int64 { // error
        get() {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
    }
    mut static prop d: Int64 { // error
        get() {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
        set(_) {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
    }
}

main() {}
