// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class MyClass {
    public let value: Int64
    public init(x: Int64) {
        value = x
    }
}

main(): Int64 {
    let fut: Future<MyClass> = spawn {
        =>
        var myClass: MyClass = MyClass(3)
        return myClass
    }
    return 0
}
