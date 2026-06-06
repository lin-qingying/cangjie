<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class A<T> {}

interface EQ {}

extend Int32 <: EQ {}

extend<T> A<T> where T <: EQ {
    public func soo() {}
    func add(a: T) {}
}

class B<E> <: A<E> { // generic-E does not satisfy the constraint T <: EQ
    func test(a: E) {
        <!sema_undeclared_identifier!>s<!>oo()
        <!sema_undeclared_identifier!>a<!>dd(a)
    }
}
