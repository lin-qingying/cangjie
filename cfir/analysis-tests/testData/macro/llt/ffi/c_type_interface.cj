<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {}

interface I2 {}

@C
struct <!CSTRUCT_CANNOT_IMPL_INTERFACES!>R<!> <: I1 {
    public var raw: CPointer<Unit>
    public init(raw: CPointer<Unit>) {
        this.raw = raw
    }
}

extend <!EXTEND_C_TYPE_NOT_ALLOWED!>R<!> <: I2 {}
