// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    public func xxx() {
        0
    }
}

func xxx(a: Int32) {
    1
}

class Test <: A {
    public static func test() {
        return xxx(10)
    }
}

main() {
    var a = Test.test()
    if (a != 1) {
        return 1
    }
    return 0
}
