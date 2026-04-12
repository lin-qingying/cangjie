// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %diag_json  %f  -o %output 2>&1 | compare %f

class Aoo<T> where T <: Bool & Object {
    public var a: T
    public init(x: T) {
        a = x
    }
}

main() {
    var a = Aoo<Bool>(false)
    if (!a.a) {
        return 1
    }
    return 0
}

// ASSERT: scan-1 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-1 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 2
