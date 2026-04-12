// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public open class Base {}

public class Sub <: Base {}

public interface I1 {
    func test(): Int64 {
        0
    }
}

public open class A {
    public func foo(a: Sub) {
        return 1
    }
}
