// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

interface I {
    func test(): Int64
}

open class A {}

extend A <: I {
    public func test(): Int64 {
        return 123
    }
}

class B <: A {}

func test<T>(a: T): Int64 where T <: I {
    return a.test()
}

main() {
    let v = B()
    if (test(v) != 123) {
        return 1
    }
    return 0
}
