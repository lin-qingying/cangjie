// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo(): Unit {
        i += 1
    }
}

func goo() {
    var obj = R1()
    var fn = <!USE_MUTABLE_FUNC_ALONE!>obj.foo<!>
    <!USE_MUTABLE_FUNC_ALONE!>obj.foo<!>
    return <!USE_MUTABLE_FUNC_ALONE!>obj.foo<!>
}

main() {
    return 1
}
