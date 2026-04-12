// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix %run_args

class NativePointerResource<T> <: Resource {
    public var v: Array<T>

    prop value: T {
        get() {
            v[0]
        }
    }

    init(x: T) {
        v = [x]
    }

    public override func isClosed() {
        true
    }
    public override func close() {}
}

main() {
    try (v = NativePointerResource<UIntNative>(0)) {
        if (v.value > 0) {}
    }

    let v = NativePointerResource<Int>(0)

    for (i in 1..10) {
        if (v.value > 0) {}
    }

    println("hello ${if (v.value > 0) {}} world!")
}
