<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck -c %cmp_opt %f 2>&1 | compare %f

foreign func foo(<!sema_cfunc_cannot_have_unit_args!>a: Unit<!>): Unit

foreign func bar(<!sema_cfunc_cannot_have_unit_args!>a: Unit<!>): Unit
