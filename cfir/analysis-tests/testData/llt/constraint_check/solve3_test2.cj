// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f

class Z {
    public func test() {}
}

struct R<X> where X <: Z {
    public var i: X
    public init(input: X) {
        input.test()
        i = input
    }
}
