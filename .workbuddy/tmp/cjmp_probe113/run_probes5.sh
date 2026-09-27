#!/bin/bash
SDK="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe"
BASE="D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113"
export CANGJIE_HOME="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie"
export TMP="$BASE/tmp" TEMP="$BASE/tmp"
mkdir -p "$TMP" "$BASE/p5"
cd "$BASE/p5" || exit 1

run_pair() {
  local name="$1"
  echo "=================== CASE $name ==================="
  echo "--- common.cj ---"; cat common.cj
  echo "--- specific.cj ---"; cat specific.cj
  rm -f *.cjo *.chir *.dll *.a 2>/dev/null
  echo ">>> common compile"
  "$SDK" common.cj --experimental --output-type=chir 2>&1 | head -25
  if [ -f cjmp_p.chir ] && [ -f cjmp_p.cjo ]; then
    echo ">>> specific compile"
    "$SDK" specific.cj cjmp_p.chir --experimental --common-part-cjo=cjmp_p.cjo -o out.dll --output-type=dylib 2>&1 | head -25
    echo ">>> dll: $(ls *.dll 2>/dev/null)"
  else
    echo ">>> common compile failed, skip specific"
  fi
}

H='package cjmp_p

'
BC="${H}public common func Platform(): String {
    \"Common\"
}
"
SP="${H}public specific func Platform(): String {
    \"Spec\"
}
"

printf '%s\npublic interface Iface {}\n\npublic common extend Iface <: Dummy {}\n\npublic interface Dummy {}\n' "$BC" > common.cj
printf '%s' "$SP" > specific.cj
run_pair extend_common_single

printf '%s\npublic interface Iface2 {}\n\npublic interface Dummy2 {}\n\npublic common public extend Iface2 <: Dummy2 {}\n' "$BC" > common.cj
run_pair extend_common_with_public

printf '%s\npublic common prop gp: Int64 {\n    get() {\n        1\n    }\n}\n' "$BC" > common.cj
run_pair toplevel_prop_common

printf '%s\npublic common type TA = Int64\n' "$BC" > common.cj
run_pair common_typealias

printf '%s\npublic func ff(common x: Int64): Int64 {\n    x\n}\n' "$BC" > common.cj
run_pair common_on_value_parameter

echo "=================== DONE ==================="
