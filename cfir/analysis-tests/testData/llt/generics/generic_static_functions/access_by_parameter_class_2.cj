// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

open class C {
    public static func foo() {
        return 0
    }
}

func Foo<U>(a: U) where U <: C {
    var b = U.foo()
    return b
}

class C1 <: C {}

main() {
    return Foo<C1>(C1())
}
