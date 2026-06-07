// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main():Int64{
    return run()
}
open class Base{
    public var base_a:Int32 = 2
}
class COO <: Base{
    public var a:Int32 = 1
    public var b:Int32 = 0
    public operator func <!parse_invalid_overloaded_operator!>++<!>():COO{
        this.a = this.a + 1
        return this
    }
}
func run():Int64 {
    var coo:COO = COO()
    coo++
    if (coo.a != 2) {return 1}
    return 0
}
