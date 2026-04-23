// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface Foo {}

class A {}

extend A <: Foo & <!EXTEND_DUPLICATE_INTERFACE!>Foo<!> {}

main(): Int64 {
    return 1
}
