// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class C<E> {
    public let myData: Array<E>
    public init(a: E) {
        myData = Array<E>(1, repeat: a)
    }
    public func get(): E {
        return myData[0];
    }
}

func f<F>(a: F): F {
    let myData = C<F>(a)
    return myData.get();
}

main() {
    f<Int64>(1)
    f<C<Int64>>(C(2))
    return 0
}
