<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {}

class Foo <: I1 {}

extend Foo <: <!EXTEND_DUPLICATE_INTERFACE!>I1<!> {}

interface I2<T> {}

type i2<T> = I2<T>

class Goo<T> <: I2<Int32> {}

extend<T> Goo<T> <: <!EXTEND_DUPLICATE_INTERFACE!>i2<Int32><!> {}
