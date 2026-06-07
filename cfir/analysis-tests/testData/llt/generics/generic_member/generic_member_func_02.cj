// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface Bar {
    func bar<T>(a: T): T
    func test<T>(v: T) {
        return v
    }
}

open class Cls<X> {
    public open func test<T>(v: T) {
        return v
    }
}

abstract class Cls2<X> {
    public func foo<T>(v: T): T
    public open func test<T>(v: T) {
        return v
    }
}

main(): Int64 {
    return 0
}
