// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct Soo<T> {
    public static func foo<V>(): Option<V> {
        return Option<V>.None
    }
    public static var a: Option<String> = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<String>()<!>
}

class Coo<T> {
    public static func foo<V>(): Option<V> {
        return Option<V>.None
    }
    public static var a: Option<String> = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<String>()<!>
}

main() {
    0
}
