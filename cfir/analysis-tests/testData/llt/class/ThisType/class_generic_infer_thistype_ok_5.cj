// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main() {
    let a = Sub()
    // Checking infered function ret type.
    let b: Sub = g(a)

    // Checking infered variable type.
    let ff = a.f
    let c: Sub = ff()
}

open class Base {
    protected open func f(): This {
        this
    }
}

class Sub <: Base {}

func g<T>(a: T) where T <: Base {
    let ff = a.f
    ff()
}
