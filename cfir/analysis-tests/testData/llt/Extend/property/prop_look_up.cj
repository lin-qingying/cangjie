// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

public interface fatherInterface {
    func foo1(): Int64
    prop b: Int64 {
        get() {
            foo1()
        }
    }
}

public interface MyInterface <: fatherInterface {
    prop b: Int64 {
        get() {
            3
        }
    }
}

public enum myEnum {
    A
}

extend myEnum <: MyInterface {
    public func foo1(): Int64 {
        1
    }
}

main() {
    var enum3: myEnum = myEnum.A
    return enum3.b - 3
}
