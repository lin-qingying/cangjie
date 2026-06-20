// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return run()
}

struct Test<T> {
    public let width2: T
    public let length2: T

    Test(width: Int64, length: Int64) {
        width2 = <!ASSIGNMENT_TYPE_MISMATCH!>width<!>
        length2 = <!ASSIGNMENT_TYPE_MISMATCH!>length<!>
    }

    func area(): T {
        return width2
    }
}

func run(): Int64 {
    var a: Int64 = 3
    var b: Int64 = 4
    let data: Test<Int32> = Test<Int32>(a, b)
    var res: Int32
    res = data.area()
    if (res == 4) {
        return 0
    }
    return 1
}
