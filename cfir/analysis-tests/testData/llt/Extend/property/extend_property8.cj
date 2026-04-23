// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A<T> {
    public var z: String = "C"
}

extend<T> A<T> {
    public mut prop f: String {
        get() {
            return z
        }
        set(v) {
            z = v
        }
    }

    public func g() {
        f = "X"
        return f
    }
}

class B<T> <: A<T> {}

main() {
    let x = B<String>()
    if (x.f == "C" && x.g() == "X") {
        print("${x.g()}")
        return 0
    }
    return 1
}
