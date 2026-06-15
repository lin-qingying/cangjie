// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class Z<T> {
    public func foo() {}
}

<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>c<!>lass C<X> where X <: Z<C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>I<!>nt32>> {
    init(input: X) {
        let a: Unit = input.foo()
    }
}

main(): Int64 {
    return 0
}
