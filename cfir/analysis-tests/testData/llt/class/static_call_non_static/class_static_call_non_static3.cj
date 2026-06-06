<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt  %compile_lib_opt %f 2>&1 | compare %f

class Base {
    public func f(): Int32 {
        return 1
    }
    public static func get(): Int32 {
        var a = <!sema_static_function_cannot_access_non_static_member!>f<!>
        return a()
    }
}
