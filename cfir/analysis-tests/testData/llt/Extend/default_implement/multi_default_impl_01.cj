// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1 {}

interface I2 <: I1 {}

interface I3 {}

interface I4 <: I3 {}

class A {}

extend <!EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE!>A<!> <: I1 & I4 {}

extend <!EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE!>A<!> <: I3 & I2 {}

main() {
}
