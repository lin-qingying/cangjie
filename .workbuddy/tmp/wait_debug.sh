#!/bin/bash
export PATH="/usr/bin:/bin:$PATH"
cd /d/code/intellij/cangjie || exit 1
QJ="java -jar gradle-queue-cli/build/libs/gradle-queue-cli.jar --project-dir D:/code/intellij/cangjie"

ok=0
for i in $(seq 1 30); do
  $QJ :cfir:resolve:compileKotlin :cfir:checkers:compileKotlin --console=plain > /tmp/wait2.log 2>&1
  if grep -q "BUILD SUCCESSFUL" /tmp/wait2.log; then
    echo "[$(date +%H:%M:%S)] COMPILE OK (attempt $i)"
    ok=1
    break
  fi
  echo "[$(date +%H:%M:%S)] attempt $i: $(grep -m1 '^e: ' /tmp/wait2.log | cut -c1-150)"
  sleep 40
done

if [ "$ok" = "1" ]; then
  rm -f tmp/cst-debug.txt
  $QJ :cfir:analysis-tests:test --tests '*CfirAnalysisLLTTestGenerated$ConstraintCheck*' --console=plain > /tmp/opt_debug4.log 2>&1
  echo "[$(date +%H:%M:%S)] test done"
  echo "--- debug file lines: $(wc -l < tmp/cst-debug.txt 2>/dev/null || echo 0)"
  grep -i "option" -A3 tmp/cst-debug.txt 2>/dev/null | head -20
  echo "--- 所有 multi-lower 记录 ---"
  grep -n "lowers=\[" tmp/cst-debug.txt 2>/dev/null | head -20
else
  echo "[$(date +%H:%M:%S)] 放弃等待编译"
fi
