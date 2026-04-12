// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args
class A {}

class B<T> where T <: A {
    B(var b: T) {
    }
}

type C<T> = B<T>

type I = II<C<A>>

interface II<T> {
    static func foos<T>(a: T) {
        return a
    }

    static prop pas: C<A> {
        get() {
            C<A>(A())
        }
    }
}

extend<T> C<T> <: I where T <: A {
    func foo<T>(a: T) {
        return a
    }

    prop pa: C<A> {
        get() {
            C<A>(A())
        }
    }
}

main() {
    B<A>.pas
    B<A>(A()).pa
    return B<A>.foos(1) + B<A>(A()).foo(-1)
}
