// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class D {}

interface A <: B {}

interface C <: A {}

interface B <: C {}

<!sema_inheritance_cycle!>e<!>xtend D <: C {}

main() {
    0
}
