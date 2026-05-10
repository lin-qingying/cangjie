// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I<T> {
    func foo1(a:T):Int64
    func foo2(a:T):Int64
    func foo3():Int64
}

class C<T, V> {} 

class A{}
class B{}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend C<A, B> <: I<A> & I<B> {}<!>

main() {
    return 0
}
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
