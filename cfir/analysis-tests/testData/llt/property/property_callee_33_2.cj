// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct Boo {
    public mut prop aaa: Int32 {
        get() {
            return aaa
        }
        set(input) {
            aaa = 250
        }
    }
    public prop bbb: Unit {
        get() {
            <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>aaa = 1<!>
        }
    }
}

main(): Int32 {
    var b = Boo()
    let t = b.bbb
    return b.aaa
}
