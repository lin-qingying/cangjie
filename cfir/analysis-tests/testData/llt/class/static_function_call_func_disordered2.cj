// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %f 2>&1 | compare %f

open class A {
    public func xxx(a: Int32) {
        0
    }
}

func xxx() {
    1
}

class Test <: A {
    public static func test() {
        return xxx(10)
    }
}

main() {
    var a = Test.test()
    if (a != 0) {
        return 1
    }
    return 0
}

// ASSERT: scan 'xxx' is non-static member, cannot be accessed by static function 'test'
