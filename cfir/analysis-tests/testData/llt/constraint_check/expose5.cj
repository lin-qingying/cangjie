// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class C {
    public func foo(a: Int32) {
        return a
    }
}

interface I<T> {
    func foo(a: T) {
        return a
    }
}

interface I2<T> {
    func foo(a: T) {
        return a
    }
}

func Foo<U>(a: U) where U <: C & I<U> & I2<U> {
    var b: U = a.foo(a)
    return b
}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>c<!>lass C1 <: C & I<C1> & I2<C1> {}

main() {
    var a: C1 = Foo<C1>(C1())
    return 0
}
