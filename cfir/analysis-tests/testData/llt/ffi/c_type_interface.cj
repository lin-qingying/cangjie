<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {}

interface I2 {}

@C
struct <!sema_cstruct_cannot_impl_interfaces!>R<!> <: I1 {
    public var raw: CPointer<Unit>
    public init(raw: CPointer<Unit>) {
        this.raw = raw
    }
}

extend <!sema_c_type_cannot_extend_interface!>R<!> <: I2 {}
