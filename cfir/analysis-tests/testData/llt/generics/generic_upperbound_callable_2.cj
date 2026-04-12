// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class C {
    operator func ()(x: Int) {
        x
    }
}

class C2 <: C {}

func foo<T>(f: T) where T <: C {
    f(0)
}

main() {
    foo(C2())
}
