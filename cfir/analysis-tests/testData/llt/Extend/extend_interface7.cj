// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {}

extend Int64 <: <!EXTEND_NOT_INTERFACE!>Int64<!> {}

extend A <: <!EXTEND_NOT_INTERFACE!>Int64<!> {}

extend Int64 <: <!EXTEND_NOT_INTERFACE!>A<!> {}

extend A <: <!EXTEND_NOT_INTERFACE!>A<!> {}

extend A <: <!EXTEND_NOT_INTERFACE!>Int64<!> & <!EXTEND_NOT_INTERFACE!>A<!> {}
