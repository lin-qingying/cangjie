// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface A {
    func default_foo(): Int32 {
        1
    }
}

interface B<X> <: A {
    func default_foo(): Int32 {
        2
    }
}

class C <: B<Int32> {}

main() {
    var b: B<Int32> = C()
    print(b.default_foo().toString())
    if (b.default_foo() != Int32(2)) {
        print("default_foo failed!")
        return 1
    }
    return 0
}
