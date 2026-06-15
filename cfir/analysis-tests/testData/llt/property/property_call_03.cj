// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public var x: Int64 = 0
    public mut prop X: Int64 {
        get() {
            return x + y
        }
        set(v) {
            x = v + y
        }
    }
    public static var y: Int64 = 0
    public mut static prop Y: Int64 {
        get() {
            return y
        }
        set(v) {
            <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>X<!> = v
        }
    }
}

main() {
    return 0
}
