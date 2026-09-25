// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: SPECIFIC
// RUN_PIPELINE_TILL: FRONTEND
// CJMP 解析族（specific 侧，官方 ParseCJMPDecl.cpp:278-290）：
// cjc 1.1.3 实测——specific 接口成员必须有实现（锚成员声明）。
<!NOT_MATCHED!>specific interface I2 {
    <!PARSE_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION!>func m(): Unit<!>
}<!>
