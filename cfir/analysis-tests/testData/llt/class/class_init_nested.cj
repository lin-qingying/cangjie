// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f

class Test {
    public var a: Int32 = 1
    public var b: Int32 = 0
    public init() {
        a = a + 1
    }
    public init(b: Int32) {
        this()
        this.b = b
        this.plus(this, this)
        this + this
    }

    public operator func +(right: Test): Test {
        return Test(this.a + right.a)
    }

    public func plus(left: Test, right: Test): Test {
        return Test(left.a + right.a)
    }
}

func run(): Int64 {
    var test: Test = Test(2)
    var res1: Int32 = test.b

    if (res1 == 4) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}

// ASSERT: regex-not .
