// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    return run()
}

class Data {
    var width: Int32
    var length: Int32

    init(x: Int32, y: Int32) {
        width = x
        length = y
    }

    func getWidth() {
        return width
    }

    func getLength() {
        return length
    }

    operator func -(operand: Data): Data {
        var left: Int32 = getWidth() - 1
        <!sema_mismatched_types!>var right: Int32 = getLength() - 1<!>
    }

    static func g() {
        var x = <!sema_static_function_cannot_access_non_static_member!>w<!>idth
        return x
    }
}

func run(): Int64 {
    var data: Data = Data(1, 2)
    var dd = <!sema_invalid_unary_expr!>-data<!>
    return 0
}
