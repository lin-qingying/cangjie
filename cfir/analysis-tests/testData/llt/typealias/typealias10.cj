// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


type A<T, U> = C<U, Base<T>>

class C2 {}

class Base<Z> where Z <: C2 {}

class C<X, Y> where X <: Base<Y> {}

main() {
    return 0;
}
