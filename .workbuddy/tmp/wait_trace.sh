#!/bin/bash
# 只读重试：等 cfir 编译恢复后跑 ConstraintCheck LLT 切片，抓 option01 的推断分支。
export PATH="/usr/bin:/bin:$PATH"
cd /d/code/intellij/cangjie || exit 1

for i in $(seq 1 20); do
  echo "=== attempt $i at $(date '+%H:%M:%S') ==="
  rm -f cfir/analysis-tests/build/cst-debug.txt
  java -jar gradle-queue-cli/build/libs/gradle-queue-cli.jar --project-dir 'D:/code/intellij/cangjie' \
    :cfir:analysis-tests:test --tests '*CfirAnalysisLLTTestGenerated$ConstraintCheck*' \
    --console=plain > /tmp/o1_trace8.log 2>&1
  if grep -q "^e: " /tmp/o1_trace8.log; then
    echo "--- compile broken ---"
    grep -m2 "^e: " /tmp/o1_trace8.log | sed 's|file:///D:/code/intellij/cangjie/||'
    sleep 60
    continue
  fi
  echo "--- ran at $(date '+%H:%M:%S') ---"
  grep -E "tests completed" /tmp/o1_trace8.log
  echo "--- UNABLE_INFER_BRANCH ---"
  grep -o "UNABLE_INFER_BRANCH.*" cfir/analysis-tests/build/cst-debug.txt 2>/dev/null | head -8
  echo "--- NOT_ENOUGH ---"
  grep -o "NOT_ENOUGH.*" cfir/analysis-tests/build/cst-debug.txt 2>/dev/null | head -8
  echo "--- FAIL-* ---"
  grep -oE "FAIL-[ABC].*" cfir/analysis-tests/build/cst-debug.txt 2>/dev/null | head -8
  break
done
