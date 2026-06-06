<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %f 2>&1 | compare %f

<!sema_type_implement_non_interface!>s<!>truct A <: A {} // Expected: regex  struct 'A' can only implement interface
