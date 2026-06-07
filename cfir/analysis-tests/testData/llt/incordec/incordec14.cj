// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct R{
    public let r : Int32
    public init(x:Int32) {
        r = x
    }
    public operator func <!parse_invalid_overloaded_operator!>++<!>():R{
        return R(this.r + 1)
    }
}

main():Int64 {
    var r:R = R(-1)
    var r1: R = r++
    return Int64(r1.r)
}
