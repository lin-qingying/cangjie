// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum ET<T> {
    A | B | C

    public static func f(a: T) {
        return a
    }
}

main() {
    return ET<Int64>.f(0)
}
