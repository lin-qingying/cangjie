// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A<K, V> <: Collection<(K, V)> {
    public func isEmpty(): Bool {
        true
    }
    public func iterator(): Iterator<(K, V)> {
        return AIter<(K, V)>()
    }
    public prop size: Int64 {
        get() {
            0
        }
    }
}

class AIter<T> <: Iterator<T> {
    public func next(): Option<T> {
        return None
    }
}

main() {
    let a: A<String, Int64> = A()
    let arr0 = a.toArray()
    let arr1: Array<(String, Int64)> = a.toArray()
    if (arr1.size != 0) {
        return 1
    }
    return 0
}
