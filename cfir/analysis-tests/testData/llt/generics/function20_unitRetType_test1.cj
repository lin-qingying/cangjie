// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func test<T1, T2, T3>(f: (T1) -> T2, g: (T2) -> T3): (T1) -> T3 {
    return {x: T1 => g(f(x))}
}

public class A<T> {
    public var x: T
    public init(input: T) {
        x = input
    }
    public func a(input: T): T {
        x
    }

    public func b(input: T): T {
        x
    }
}

main() {
    test(A<Int64>(1).a, A<Int64>(1).b)
    A<Int64>(1).a ~> A<Int64>(1).b
    return 0
}
