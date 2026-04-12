// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface A<T> {
    func add(a: T) {
        return a
    }
}

extend Int32 <: A<Int32> {}

main() {
    let a: Int32 = 1
    a.add(1)
    0
}
