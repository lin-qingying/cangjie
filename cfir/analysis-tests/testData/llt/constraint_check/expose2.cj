// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func i(a: T): Int32
}

open class C<U> {
    public open func c(a: U): Bool {
        return false
    }
}

func foo<X>(a: X): Int64 where X <: C<Bool> & I<Rune> {
    a.i(r'a')
    a.c(true)
    return 10
}

class B <: C<Bool> & I<Rune> {
    public func i(a: Rune): Int32 {
        return 10;
    }
}

main(): Int64 {
    foo<B>(B())
    return 0
}
