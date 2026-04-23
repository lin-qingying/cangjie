// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

extend<T> Array<T> {
    static func foo(a: T): T {
        return a
    }
}

main() {
    return Array<Int64>.foo(1) - 1
}
