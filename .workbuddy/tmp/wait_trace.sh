#!/bin/bash
export PATH="/usr/bin:/bin:$PATH"
cd /d/code/intellij/cangjie || exit 1
JAR=gradle-queue-cli/build/libs/gradle-queue-cli.jar
rm -f tmp/cst-debug.txt

for i in $(seq 1 15); do
  echo "=== attempt $i at $(date '+%H:%M:%S') ==="
  java -jar "$JAR" --project-dir 'D:/code/intellij/cangjie' \
    :cfir:analysis-tests:test --tests '*CfirAnalysisLLTTestGenerated$ConstraintCheck*' \
    --console=plain > /tmp/o1_trace.log 2>&1
  if grep -q "^e: " /tmp/o1_trace.log; then
    echo "--- compile broken, waiting ---"
    grep -m3 "^e: " /tmp/o1_trace.log
    sleep 90
    continue
  fi
  echo "--- run ok at $(date '+%H:%M:%S') ---"
  grep -E "tests completed" /tmp/o1_trace.log
  break
done
echo "=== trace lines: $(wc -l < tmp/cst-debug.txt 2>/dev/null) ==="
