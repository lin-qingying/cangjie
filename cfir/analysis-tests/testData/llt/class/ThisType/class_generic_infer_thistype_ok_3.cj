// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// This for type check compilation
main() {
    let a = Sub()
    let b: Sub = g(a)
}

open class Base {
    protected open func f(): This {
        this
    }
}

class Sub <: Base {}

func g<T>(a: T) where T <: Base {
    a.f()
}
