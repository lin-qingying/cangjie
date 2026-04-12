// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A <: ToString {
    public func toString() {
        "A"
    }
}

var ff: (Int64) -> Any = {v => ()}

func test<T>(f: (Int64) -> T) where T <: ToString & A {
    ff = f
}

main() {
    if (!(ff(1) is Unit)) {
        return 1
    }
    test<A>({v => A()})
    let v = ff(1)
    if (v is Unit) {
        return 2
    }
    if (v is ToString) {
        return 0
    }
    return 3
}
