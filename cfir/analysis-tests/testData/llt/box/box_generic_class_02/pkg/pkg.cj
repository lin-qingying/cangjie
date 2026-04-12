// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public open class A<T> {}

extend<T> A<T> <: Equatable<A<T>> {
    public operator func ==(ot: A<T>) {
        refEq(this, ot)
    }
    public operator func !=(ot: A<T>) {
        false
    }
}

public class B <: A<Int64> {}

extend B <: Equatable<B> {
    public operator func ==(ot: B) {
        refEq(this, ot)
    }
    public operator func !=(ot: B) {
        false
    }
}

public func bar(): Any {
    let a = B()
    return a
}
