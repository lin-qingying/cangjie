// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C<T, U> {
    public static func foo(a: T, b: U) {}
    public static func foo(a: U, b: U) {}
}

struct R<T, U> {
    public static func foo(a: T, b: U) {}
    public static func foo(a: U, b: U) {}
}

func bar<X>(a: X) {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<X, X><!>.<!AMBIGUOUS_FUNCTION_CALL!>foo<!>(a, a)
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>R<X, X><!>.<!AMBIGUOUS_FUNCTION_CALL!>foo<!>(a, a)
}

main() {
    return 0;
}
