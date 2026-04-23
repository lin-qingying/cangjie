// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class App {
    public init(config!: Option<Config> = Option<Config>.None) {}
}

class Config {
    public init(logLevel!: Option<<!UNRESOLVED_REFERENCE!>LogLevel<!>> = Option<<!UNRESOLVED_REFERENCE!>LogLevel<!>>.None) {}
}

// 注释去掉 不会core dump
// public enum LogLevel {
// }

public func SwanMain() {
    App(config: Option<Config>.Some(Config(<!NEED_NAMED_ARGUMENT!>Option<<!UNRESOLVED_REFERENCE!>LogLevel<!>>.Some(LogLevel.debug)<!>))).run()
}
