<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

<!sema_value_type_recursive!>s<!>truct A {
    <!sema_class_uninitialized_field!>p<!>ublic let x: A
}

<!sema_value_type_recursive!>s<!>truct B {
    <!sema_class_uninitialized_field!>p<!>ublic let x: C
    <!sema_class_uninitialized_field!>p<!>ublic let y: Int32
}

struct C {
    <!sema_class_uninitialized_field!>p<!>ublic let x: D
}

struct D {
    <!sema_class_uninitialized_field!>p<!>ublic let x: B
}
