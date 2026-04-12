// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT:regex : expected a type name after '.' in qualified name, found '<EOF>'
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace


class C1 {
  public interface I1 {}
  public class C2 {
    public interface I2 {}
 }
}
class C3 <: C1.
