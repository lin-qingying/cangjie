// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class App {
    public init(config!: Option<Config> = Option<Config>.None) {}
}

class Config {
    public init(logLevel!: Option<LogLevel> = Option<LogLevel>.None) {}
}

// 注释去掉 不会core dump
// public enum LogLevel {
// }

public func SwanMain() {
    App(config: Option<Config>.Some(Config(Option<LogLevel>.Some(LogLevel.debug)))).run()
}

/* SCAN
error: undeclared type name 'LogLevel'
  ==> enum6_test.cj:14:35:
   | 
14 |     public init(logLevel!: Option<LogLevel> = Option<LogLevel>.None) {}
   |                                   ^ 
   | 

error: undeclared type name 'LogLevel'
  ==> enum6_test.cj:14:54:
   | 
14 |     public init(logLevel!: Option<LogLevel> = Option<LogLevel>.None) {}
   |                                                      ^ 
   | 

error: undeclared type name 'LogLevel'
  ==> enum6_test.cj:22:51:
   | 
22 |     App(config: Option<Config>.Some(Config(Option<LogLevel>.Some(LogLevel.debug)))).run()
   |                                                   ^ 
   | 

error: missing argument prefix 'logLevel:' for named parameter
  ==> enum6_test.cj:22:44:
   | 
22 |     App(config: Option<Config>.Some(Config(Option<LogLevel>.Some(LogLevel.debug)))).run()
   |                                            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
