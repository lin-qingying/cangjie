#!/bin/bash
SDK="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe"
BASE="D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113"
export CANGJIE_HOME="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie"
export TMP="$BASE/tmp" TEMP="$BASE/tmp"
mkdir -p "$TMP" "$BASE/p6"
cd "$BASE/p6" || exit 1
run_pair() {
  local name="$1"
  echo "=================== CASE $name ==================="
  echo "--- common.cj ---"; cat common.cj
  rm -f *.cjo *.chir *.dll *.a 2>/dev/null
  echo ">>> common compile"
  "$SDK" common.cj --experimental --output-type=chir 2>&1 | head -25
  if [ -f cjmp_p.chir ] && [ -f cjmp_p.cjo ]; then
    echo ">>> specific compile"
    "$SDK" specific.cj cjmp_p.chir --experimental --common-part-cjo=cjmp_p.cjo -o out.dll --output-type=dylib 2>&1 | head -25
  else
    echo ">>> common compile failed, skip specific"
  fi
}
H='package cjmp_p

'
SP="${H}public specific func Platform(): String {
    \"Spec\"
}
"
printf '%s\npublic common interface I {\n    common func m()\n}\n' "$H" > common.cj
printf '%s' "$SP" > specific.cj
run_pair common_iface_member_marked

printf '%s\npublic common func NoRet()\n' "$H" > common.cj
run_pair toplevel_bodyless_confirm

printf '%s\npublic specific func Pf()\n' "$SP" > specific.cj
printf '%s\npublic common func Platform(): String {\n    \"Common\"\n}\n' "$H" > common.cj
run_pair specific_toplevel_bodyless

echo "=================== DONE ==================="
