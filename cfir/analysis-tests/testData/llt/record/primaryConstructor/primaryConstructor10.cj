// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct Data<T1> where T1 <: ToString {
    public let a: T1
    public Data(x: T1) {
        a = x
    }
    public func show() {
        print("a = ${a}\n")
    }
}

main() {
    let data: Data<Int32> = Data(3)
    data.show()
    return 0
}
