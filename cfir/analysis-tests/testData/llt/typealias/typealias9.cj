// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


type A<T> = Cl<T>

open class Base {}

class Cl<U> where U <: Base {}

class Sub<X> {
    public var <!CLASS_UNINITIALIZED_FIELD!>a<!>: <!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>A<!><X>
}

main() {
    return 0;
}
