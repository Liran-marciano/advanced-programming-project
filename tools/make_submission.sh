#!/usr/bin/env bash
# Builds a "test"-packaged copy of the files required for a specific exercise's
# submission to the course's grading system. The exact set differs per exercise
# (the course did not say "submit the whole package every time"), so this
# script knows the per-exercise file list and only copies what is required.
#
# Usage: ./tools/make_submission.sh <exercise-number>
#   e.g. ./tools/make_submission.sh 1
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage: $0 <exercise-number>" >&2
    echo "  e.g. $0 1" >&2
    exit 1
fi

ex="$1"
case "$ex" in
    1) files=(
           "graph/Message.java"
           "graph/Topic.java"
           "graph/TopicManagerSingleton.java"
       ) ;;
    2) files=(
           "graph/Message.java"
           "graph/Topic.java"
           "graph/TopicManagerSingleton.java"
           "graph/ParallelAgent.java"
       ) ;;
    3) files=(
           "graph/Message.java"
           "graph/Topic.java"
           "graph/TopicManagerSingleton.java"
           "graph/ParallelAgent.java"
           "configs/BinOpAgent.java"
           "configs/Graph.java"
           "configs/Node.java"
       ) ;;
    4) files=(
           "graph/Message.java"
           "graph/Topic.java"
           "graph/TopicManagerSingleton.java"
           "graph/ParallelAgent.java"
           "configs/PlusAgent.java"
           "configs/IncAgent.java"
           "configs/GenericConfig.java"
       ) ;;
    5) files=(
           "server/MyHTTPServer.java"
           "server/RequestParser.java"
       ) ;;
    *)
       echo "Unknown exercise number '$ex' (valid: 1-5)" >&2
       exit 1
       ;;
esac

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
src_root="$repo_root/src/project_biu"
out_root="$repo_root/submission_ex${ex}"
out_dir="$out_root/test"

# Validate every source file exists before touching disk.
missing=()
for rel in "${files[@]}"; do
    [ -f "$src_root/$rel" ] || missing+=("$rel")
done
if [ ${#missing[@]} -gt 0 ]; then
    echo "Missing source files for exercise $ex:" >&2
    for m in "${missing[@]}"; do echo "  - $m" >&2; done
    exit 1
fi

rm -rf "$out_root"
mkdir -p "$out_dir"

for rel in "${files[@]}"; do
    name="$(basename "$rel")"
    sed -E \
        -e 's/^package[[:space:]]+project_biu\.[a-zA-Z0-9_.]+[[:space:]]*;/package test;/' \
        -e 's/^import[[:space:]]+project_biu\.[a-zA-Z0-9_]+\.([A-Za-z0-9_.]+)[[:space:]]*;/import test.\1;/' \
        "$src_root/$rel" > "$out_dir/$name"
done

# Files that sit alongside the submission for local compilation / IDE
# happiness but must NOT be uploaded -- the grader supplies them.
# Different exercises reference different supporting classes.
case "$ex" in
    1|2|3) reference_only=("graph/Agent.java") ;;
    4)     reference_only=("graph/Agent.java" "configs/Config.java") ;;
    5)     reference_only=("server/HTTPServer.java" "servlets/Servlet.java") ;;
    *)     reference_only=() ;;
esac
for rel in "${reference_only[@]}"; do
    [ -f "$src_root/$rel" ] || continue
    name="$(basename "$rel")"
    sed -E \
        -e 's/^package[[:space:]]+project_biu\.[a-zA-Z0-9_.]+[[:space:]]*;/package test;/' \
        -e 's/^import[[:space:]]+project_biu\.[a-zA-Z0-9_]+\.([A-Za-z0-9_.]+)[[:space:]]*;/import test.\1;/' \
        "$src_root/$rel" > "$out_dir/$name"
done

echo ""
echo "Exercise $ex submission written to: $out_dir"
echo ""
echo "Upload these files (zip them):"
for rel in "${files[@]}"; do echo "  $(basename "$rel")"; done
echo ""
echo "Reference-only (DO NOT upload — the grader supplies them):"
for rel in "${reference_only[@]}"; do echo "  $(basename "$rel")"; done
