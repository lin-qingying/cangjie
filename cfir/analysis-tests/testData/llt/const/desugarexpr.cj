// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f

class A {
    let a: Int64
    let b: Int64

    const init() {
        (a, b) = (1, -1)
    }
}

const func bar(a: () -> Unit) {}

const func dot() {
    bar {=>}
}

main() {}
