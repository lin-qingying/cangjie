// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct Soo<T> {
    public static prop foo: Int64 {
        get() {
            1
        }
    }
    public static var a = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<!>
}

class Coo<T> {
    public static prop foo: Int64 {
        get() {
            1
        }
    }
    public static var a = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<!>
}

main() {
    0
}
