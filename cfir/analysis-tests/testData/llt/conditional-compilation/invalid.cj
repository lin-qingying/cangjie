// RUN_PIPELINE_TILL: FRONTEND
// WHEN_ENV: backend=cjnative,arch=x86_64,os=Windows,cjc_version=1.0.5,debug=false,test=false

@When[<!CONDITIONAL_COMPILATION_NOT_SUPPORT_BUILTIN_VALUE!>arch == "arm64"<!>]
func invalidArchitectureCondition() {}
