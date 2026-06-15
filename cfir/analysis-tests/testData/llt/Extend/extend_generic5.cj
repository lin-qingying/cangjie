<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A<T> {}

interface EQ {}

extend Int32 <: EQ {}

extend<T> A<T> where T <: EQ {
    public func soo() {}
    func add(a: T) {}
}

class B<E> <: A<E> { // generic-E does not satisfy the constraint T <: EQ
    func test(a: E) {
        <!UNRESOLVED_REFERENCE!>s<!>oo()
        <!UNRESOLVED_REFERENCE!>a<!>dd(a)
    }
}
