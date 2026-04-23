// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Basei {
    public func getNum1() {
        return 1
    }
    public var c = 5;
    private var t = 3
}

class Data <: Basei {
    public var a = 2;
    public var resc = temp;
    public var res = <!USED_BEFORE_INITIALIZATION!>getNum1<!>();
    public init() {}
}

func run(): Int64 {
    var value: Data = Data()
}
