// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class C {
    func coo() {
        return 0
    }
}

interface Tr {
    func bar(): Int64
}

class D <: C {}

extend D <: Tr {
    public func bar(): Int64 {
        return 0
    }
}

func foo<T>(a: T) where T <: C & Tr {
    return a.coo() + a.bar()
}

main(): Int64 {
    foo<D>(D())
    return 0
}
