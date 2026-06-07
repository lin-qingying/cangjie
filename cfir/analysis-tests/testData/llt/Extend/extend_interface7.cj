<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {}

extend Int64 <: <!sema_extend_not_interface!>Int64<!> {}

extend A <: <!sema_extend_not_interface!>Int64<!> {}

extend Int64 <: <!sema_extend_not_interface!>A<!> {}

extend A <: <!sema_extend_not_interface!>A<!> {}

extend A <: <!sema_extend_not_interface!>Int64<!> & <!sema_extend_not_interface!>A<!> {}
