// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

class A<T, K> {
    var x: T
    var y: K
    init(a: T, b: K) {
        x = a
        y = b
    }
    func test(): T {
        let m = A<String, T>("s", x)
        let n: A<String, T> = m
        return A<String, T>("s", x).get("s")
    }
    func get(a: T): K {
        y
    }
}

enum E<T, K> {
    EE(T, K)

    func test(): E<String, T> {
        let a = match (this) {
            case EE(t, k) => EE<String, T>("s", t)
        }
        return a
    }
}

main() {
    let a = A<Int32, Int64>(11, 22)
    let e = EE('a', 2)
    e.test()
    let v = a.test()
    if (v != 11) {
        return 1
    }
    return 0
}
