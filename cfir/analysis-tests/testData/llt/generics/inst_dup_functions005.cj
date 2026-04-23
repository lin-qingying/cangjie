// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {}

class E<X, Y> {
    public static func f(a: X, b: Y) {
        print("f(a: X, b: Y)")
    }
    public static func f(a: A, b: Int64) {
        print("f(a: X, b: Int64)")
    }
}

main() {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>E<A, Int64><!>.<!AMBIGUOUS_FUNCTION_CALL!>f<!>(A(), 1)
    return 0;
}
