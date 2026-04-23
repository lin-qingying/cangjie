[target.aarch64-linux-ohos]
  compile-option = "-B \"${DEVECO_CANGJIE_HOME}/compiler/third_party/llvm/bin\" -B \"${DEVECO_CANGJIE_HOME}/musl/usr/lib/aarch64-linux-ohos\" -L \"${DEVECO_CANGJIE_HOME}/musl/usr/lib/aarch64-linux-ohos\" -L \"${DEVECO_CANGJIE_HOME}/build/linux_ohos_aarch64_llvm/openssl\" --sysroot \"${DEVECO_CANGJIE_HOME}/musl\""

[target.x86_64-linux-ohos]
  compile-option = "-B \"${DEVECO_CANGJIE_HOME}/compiler/third_party/llvm/bin\" -B \"${DEVECO_CANGJIE_HOME}/musl/usr/lib/x86_64-linux-ohos\" -L \"${DEVECO_CANGJIE_HOME}/musl/usr/lib/x86_64-linux-ohos\" -L \"${DEVECO_CANGJIE_HOME}/build/linux_ohos_x86_64_llvm/openssl\" --sysroot \"${DEVECO_CANGJIE_HOME}/musl\""

[dependencies]
  cj_res_${moduleName} = {path = "./cj_res", version = "1.0.0"}

[package]
  cjc-version = "0.48.2"
  compile-option = ""
  description = "CangjieUI Application"
  link-option = ""
  name = "ohos_app_cangjie_${moduleName}"
  output-type = "dynamic"
  src-dir = "."
  target-dir = ""
  version = "1.0.0"
  package-configuration = {}
  scripts = {}

[target.aarch64-linux-ohos.bin-dependencies]
  path-option = ["<#noparse>${AARCH64_LIBS}</#noparse>", "<#noparse>${AARCH64_MACRO_LIBS}</#noparse>", "<#noparse>${AARCH64_TPC_LIBS}</#noparse>"]

[target.x86_64-linux-ohos.bin-dependencies]
  path-option = ["<#noparse>${X86_64_OHOS_LIBS}</#noparse>", "<#noparse>${X86_64_OHOS_MACRO_LIBS}</#noparse>", "<#noparse>${X86_TPC_LIBS}</#noparse>"]

<#if osType == "windows">
[target.x86_64-unknown-windows-gnu.bin-dependencies]
  path-option = ["<#noparse>${X86_64_LIBS}</#noparse>", "<#noparse>${X86_64_MACRO_LIBS}</#noparse>"]
</#if>

[target.x86_64-w64-mingw32.bin-dependencies]
  path-option = [ "<#noparse>${X86_64_WIN_TPC_MACRO_LIBS}</#noparse>" ]

[target.aarch64-apple-darwin.bin-dependencies]
  path-option = [ "<#noparse>${AARCH64_DARWIN_TPC_MACRO_LIBS}</#noparse>" ]

[target.x86_64-apple-darwin.bin-dependencies]
  path-option = [ "<#noparse>${X86_64_DARWIN_TPC_MACRO_LIBS}</#noparse>" ]

[profile]
  [profile.build]
    incremental = true
    lto = ""
    [profile.build.combined]
      ohos_app_cangjie_${moduleName} = "dynamic"
  [profile.customized-option]
    debug = "-g -Woff all"
    release = "--fast-math -O2 -s -Woff all"
  [profile.test]
