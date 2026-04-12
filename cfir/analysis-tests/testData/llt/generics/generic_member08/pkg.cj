// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public enum A<T> {
    A1
}

extend<T> A<T> {
    public func foo<X>(b!: A<T> = A1) {
        return b
    }
}

public func test() {
    A1<Int64>.foo<Int64>()
    return 0
}
