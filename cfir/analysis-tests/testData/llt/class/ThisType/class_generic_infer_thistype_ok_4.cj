// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main() {
    let a = Sub<Rune>()
    let b = g(a, r'a')
    if (b is Sub<Rune>) {
        return 0
    }
    return 1
}

open class Base<T> {
    protected open func f(): This {
        this
    }
}

class Sub<T> <: Base<T> {}

func g<T, K>(a: T, b: K) where T <: Base<K> {
    a.f()
}
