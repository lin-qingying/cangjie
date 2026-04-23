// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    func test(): Int64 {
        1
    }
}

interface J {}

open class A {}

extend A <: I & J {}

class B <: A {}

main() {
    let b = B()
    b.test() - 1
}
