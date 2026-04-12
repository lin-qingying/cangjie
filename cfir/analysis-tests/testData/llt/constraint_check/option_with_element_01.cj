// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func f<T>(_: T, _: T): Unit where T <: Equatable<T> {}

class A <: Equatable<A> {
    public operator func !=(_: A) {
        false
    }
    public operator func ==(_: A) {
        true
    }
}

main() {
    f(1, Some(1))
    f(Some(1), 1)
    f(A(), Some(A()))
    f(Some(A()), A())
}
