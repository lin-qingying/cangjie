// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class Data {
    public Data(let a!: Bool = <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER, CAPTURE_HAS_SHADOW_VARIABLE!>x<!>, var x!: Bool = a) {}
}

func run(): Int32 {
    var data: Data = Data()
    print(data.a.toString())
    print(data.x.toString())

    if (data.a && data.x) {
        return 0
    }
    return 1
}

main(): Int64 {
    return Int64(run())
}
