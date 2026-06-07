// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


public open class Class257 {
    public var a = 6
    public func test() {
        this |> func937
    }
}

public func func937(ins: Class257) {
    return ins.a
}

main(): Int64 {
    var res = Class257().test()
    if (res == 6) {
        return 0
    }
    return 1
}
