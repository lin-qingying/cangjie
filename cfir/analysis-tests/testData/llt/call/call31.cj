// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public class Base {
    public open func test(): Int64 {
        return 1
    }
}

class Test {
    public var i: Int64 = 0
    public init() {
        this.i = Base().test()
    }
}

main() {
    Test().i - 1
}
