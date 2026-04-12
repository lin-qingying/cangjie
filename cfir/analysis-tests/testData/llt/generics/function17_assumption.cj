// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Base {
    public func hello() {
        print("hello")
    }
}

class A {
    public static func test<T>(a: T) where T <: Base {
        a.hello()
    }
}

main(): Int64 {
    A.test<Base>(Base());
    return 0;
}
