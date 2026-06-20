// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Data {
    var b: Int32
    public let p0: Int32
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic Data(x: Int32, var <!REDECLARATION!>p0<!>: Int32) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.p0 = 3<!>
    }
}

func run(): Int32 {
    let a: Int32 = 5
    var data: Data = Data(a, a)
    return data.p0 - 5
}

main(): Int64 {
    return Int64(run())
}
