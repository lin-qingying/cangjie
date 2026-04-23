// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(f: T) where T <: (Int) -> Int {
    func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>>(g: U) where U <: T {
        func baz<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>R<!>>(h: R) where R <: U {
            h(0)
        }
        g(1)
        baz(g)
    }
    f(2)
    bar(f)
}

main() {
    foo({x: Int => x})
}
