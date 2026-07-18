

class App {
    public init(config!: Option<Config> = Option<Config>.None) {}
}

class Config {
    public init(logLevel!: Option<<!UNDECLARED_TYPE_NAME!>LogLevel<!>> = Option<<!UNDECLARED_TYPE_NAME!>LogLevel<!>>.None) {}
}

// 注释去掉 不会core dump
// public enum LogLevel {
// }

public func SwanMain() {
    App(config: Option<Config>.Some(Config(<!NEED_NAMED_ARGUMENT!>Option<<!UNDECLARED_TYPE_NAME!>LogLevel<!>>.Some(LogLevel.debug)<!>))).run()
}
