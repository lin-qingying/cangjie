// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public abstract class A {
    public func a(): Int64
}

public abstract class B <: A {
    public func c(): Int64
    public func b(): Int64 {
        0
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>public class C<!> <: B {
    public func c(): Int64 {
        3
    }
}

main() {
    var c = C()
    if (c.c() == 3 && c.b() == 4) {
        print("Pass\n")
    }
    return 0
}
