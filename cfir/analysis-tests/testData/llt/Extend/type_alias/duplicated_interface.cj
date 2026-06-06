<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {}

class Foo <: I1 {}

extend Foo <: <!sema_extend_duplicate_interface!>I1<!> {}

interface I2<T> {}

type i2<T> = I2<T>

class Goo<T> <: I2<Int32> {}

extend<T> Goo<T> <: <!sema_extend_duplicate_interface!>i2<Int32><!> {}
