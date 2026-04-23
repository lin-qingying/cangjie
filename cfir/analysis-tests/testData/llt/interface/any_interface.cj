// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct A {}

extend A <: <!EXTEND_INTERFACE_NOT_EXTENDABLE!>Any<!> {}

class B {}

extend B <: <!EXTEND_INTERFACE_NOT_EXTENDABLE!>Any<!> {}
