// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Base12<T> {
    public var d: T
    public mut prop a: T {
        get() {
            return d
        }
        set(v) {
            d = v
            return ()
        }
    }
    public init(d: T, a: T) {
        this.d = d
        this.a = a
    }
}

main() {
    var x = Base12<Bool>(true, false)
    if (x.a == false) {
        print("${x.a}")
        return 0
    }
    return 1
}
