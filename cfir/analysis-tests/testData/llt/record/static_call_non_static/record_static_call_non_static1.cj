// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    return run()
}

struct Data {
    public var width: Int32
    public var length: Int32

    public init(x: Int32, y: Int32) {
        width = x
        length = y
    }

    public operator func -(): Data {
        var left: Int32 = width
        <!sema_mismatched_types!>var right: Int32 = length<!>
    }

    public static func g() {
        var x = <!sema_static_function_cannot_access_non_static_member!>w<!>idth
        return x
    }
}

func run(): Int64 {
    var data: Data = Data(1, 2)
    var dd = -data
    return 0
}
