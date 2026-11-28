// 宏包及 Tokens 签名遵循官方文法；A/M 故意未声明，仅作 raw 重建，不期待语义诊断。
macro package lowlevel.parameterownerrebuild

import std.ast.*

public macro generate(@!A input: Tokens): Tokens { return @M(input) }
