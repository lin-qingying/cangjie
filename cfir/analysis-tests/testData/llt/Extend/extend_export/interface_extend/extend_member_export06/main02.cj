// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb.cj
import a
internal import b.*

interface I2 {
    func foo(): Int64
}

extend<T> a.A<T> <: I2 {
    public func foo() {
        2
    }
}

extend a.B <: I2 {
    public func foo() {
        2
    }
}

public func bar() {
    if (a.B().foo() == 2) {
        println("non-generic extending works well")
    }
    let res = funcB()
    if (res == 1) {
        println("non-generic extending works well")
    }

    // Test generic extend in different pkg, but instantiated in the same pkg.
    if (a.A<Int64>().foo() != 2) {
        return 1
    }
    return funcA<Int64>() - 1
}

main() {
    return bar()
}
