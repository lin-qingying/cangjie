// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func getHeight(height: Int64): Int64 {
    return height * 2
}

func foo<T>(a: T): Int32 {
    return 10
}

class Data<T> {
    public let width: T
    public let f: Int32
    public init(x: T) {
        width = <!REF_NOT_BE_TYPE!>T<!>
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>width = ((<!REF_NOT_BE_TYPE!>T<!>))<!>
        f = foo<T>(<!REF_NOT_BE_TYPE!>T<!>)
    }
}

func run(): Int32 {
    let data: Data<(Int64) -> Int64> = Data<(Int64) -> Int64>(getHeight)
    var res = data.width(5)
    if (res == 10) {
        return 0
    }
    return 1
}

main(): Int32 {
    return run()
}
