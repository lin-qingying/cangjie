// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A<T> {
    init() {}
    init(x: Bool, a: Array<T>) {}
    static func f(x: Int64, y: Int64, z: Int64) {
        x + y + z
    }
    static func f(x: Array<T>) {
        x.size
    }
    func g(a: Array<Array<T>>) {
        a.size
    }
    func g(x: Bool, a: Array<T>) {
        if (x) {
            a.size + 1
        } else {
            a.size
        }
    }
}

main() {
    func h(b: Bool, a: Array<Int8>) {
        if (b) {
            0
        } else {
            a.size
        }
    }
    func h(a: Array<Int8>) {
        a.size
    }
    if (A<Int8>(true, 1, 2, 3, 4).g() != 0) {
        return 1
    }
    if (A<Int64>.f(1, 2, 3) != 6) {
        return 1
    }
    if (A<Bool>.f(true, false) != 2) {
        return 1
    }
    if (A<Int8>.f(1, 2, 3, 4) != 4) {
        return 1
    }
    if (A<Bool>().g([true], [false]) != 2) {
        return 1
    }
    if (A<Int64>().g(true, 1, 2, 3) != 4) {
        return 1
    }
    if (h(1, 2, 3) != 3) {
        return 1
    }
    return 0
}
