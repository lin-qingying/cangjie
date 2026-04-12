// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %n.%suffix 2>&1 

public interface PrintAble {
    static func test(): Int64 {
        1
    }
}

public open class Parent <: PrintAble {
    public static redef func test(): Int64 {
        2
    }
}

public class Child <: Parent {
    public static redef func test(): Int64 {
        3
    }
}

public func test<T>() where T <: PrintAble {
    T.test()
}

public func test2<T>() where T <: Parent {
    T.test()
}

main(): Int64 {
    let v1 = test<Child>()
    let v2 = test2<Child>()
    if (v1 != 3) {
        return 1
    }
    if (v2 != 3) {
        return 2
    }
    return 0
}
