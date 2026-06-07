// FILE: file2.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Bar {
    static let x: Int64
    static init() {
        x = <!USED_BEFORE_INITIALIZATION!>Foo.y<!>
    }
}

// FILE: file1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// Test for sub class using parent class's static member in different file
// Circular initialization

class Foo <: Bar {
    static let y: Int64
    static init() {
        y = x
    }
}

main() {
    if (Foo.y == Bar.x && Bar.x == 2) {
        return 0
    }
    return 1
}
