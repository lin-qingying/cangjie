// FILE: file2.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Bar {
    static let x: Int64
    static init() {
        x = Foo.y
    }
}

// FILE: file1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class Foo <: Bar {
    static var y: Int64
    static init() {
        y = 1
    }
}

main() {
    if (Foo.y == Bar.x && Bar.x == 1) {
        return 0
    }
    return 1
}
