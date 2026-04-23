// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface A<T> {
    static func add(a: T): T {
        return a
    }
}

class C {}

extend C <: A<Int64> {}

main() {
    let b: Int64 = 1
    return C.add(b) - 1
}
