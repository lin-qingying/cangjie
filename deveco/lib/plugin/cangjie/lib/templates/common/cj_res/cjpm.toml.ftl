[target.aarch64-linux-ohos]
  compile-option = "-B \"${DEVECO_CANGJIE_HOME}/compiler/third_party/llvm/bin\" -B \"${DEVECO_CANGJIE_HOME}/musl/usr/lib/aarch64-linux-ohos\" -L \"${DEVECO_CANGJIE_HOME}/musl/usr/lib/aarch64-linux-ohos\" -L \"${DEVECO_CANGJIE_HOME}/build/linux_ohos_aarch64_llvm/openssl\" --sysroot \"${DEVECO_CANGJIE_HOME}/musl\""

[target.x86_64-linux-ohos]
  compile-option = "-B \"${DEVECO_CANGJIE_HOME}/compiler/third_party/llvm/bin\" -B \"${DEVECO_CANGJIE_HOME}/musl/usr/lib/x86_64-linux-ohos\" -L \"${DEVECO_CANGJIE_HOME}/musl/usr/lib/x86_64-linux-ohos\" -L \"${DEVECO_CANGJIE_HOME}/build/linux_ohos_x86_64_llvm/openssl\" --sysroot \"${DEVECO_CANGJIE_HOME}/musl\""

[dependencies]

[package]
  cjc-version = "0.49.2"
  compile-option = ""
  description = "Cangjie Resource Module"
  link-option = ""
  name = "cj_res_${moduleName}"
  output-type = "dynamic"
  src-dir = "src"
  target-dir = ""
  version = "1.0.0"
  package-configuration = {}
  scripts = {}

[profile]
  build = {incremental = true, lto = ""}
  customized-option = {debug = "-g -Woff all", release = "--fast-math -O2 -s -Woff all"}
  test = {}
