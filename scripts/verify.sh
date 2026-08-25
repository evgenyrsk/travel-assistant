#!/usr/bin/env sh

set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
profile=${1:-core}

check_untracked_whitespace() {
  untracked_files=$(git -C "$repository_root" ls-files --others --exclude-standard)
  [ -n "$untracked_files" ] || return

  saved_ifs=$IFS
  IFS='
'
  untracked_whitespace_failed=0
  for relative_path in $untracked_files; do
    absolute_path="$repository_root/$relative_path"
    if [ -f "$absolute_path" ] && grep -Iq . "$absolute_path"; then
      if ! awk -v path="$relative_path" '
        /[[:blank:]]+$/ {
          printf "%s:%d: trailing whitespace\n", path, NR
          found = 1
        }
        END { exit found }
      ' "$absolute_path"; then
        untracked_whitespace_failed=1
      fi
    fi
  done
  IFS=$saved_ifs

  [ "$untracked_whitespace_failed" -eq 0 ]
}

run_docs() {
  git -C "$repository_root" diff --check
  git -C "$repository_root" diff --cached --check
  check_untracked_whitespace
}

run_core() {
  run_docs

  (
    cd "$repository_root/services/backend"
    if [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
      unset JAVA_HOME
      if [ -x /usr/libexec/java_home ]; then
        if detected_java_home=$(/usr/libexec/java_home -v 17 2>/dev/null); then
          export JAVA_HOME="$detected_java_home"
        fi
      fi
    fi
    ./gradlew test
  )

  (
    cd "$repository_root"
    node --test scripts/local-demo.test.mjs
  )

  (
    cd "$repository_root/app"
    npm run lint
    npm test
    npm run build
  )

  (
    cd "$repository_root/tools/openapi-conformance"
    npm test
    npm run check
  )

  (
    cd "$repository_root/tools/semantic-evaluation"
    npm test
  )
}

run_optional_tools() {
  if [ -f "$repository_root/tools/tbank-mcp-local/package.json" ]; then
    (
      cd "$repository_root/tools/tbank-mcp-local"
      npm run verify
    )
    return
  fi

  if [ -f "$repository_root/tools/tbank-hotels-mcp/package.json" ]; then
    (
      cd "$repository_root/tools/tbank-hotels-mcp"
      npm test
    )
  fi

  if [ -f "$repository_root/tools/tbank-banking-mcp/pyproject.toml" ]; then
    banking_python=python3
    if [ -x "$repository_root/tools/tbank-banking-mcp/.venv/bin/python" ]; then
      banking_python="$repository_root/tools/tbank-banking-mcp/.venv/bin/python"
    fi
    (
      cd "$repository_root/tools/tbank-banking-mcp"
      "$banking_python" -m unittest discover -s test
    )
  fi
}

case "$profile" in
  docs)
    run_docs
    ;;
  core)
    run_core
    ;;
  all)
    run_core
    run_optional_tools
    ;;
  *)
    printf 'Usage: %s [docs|core|all]\n' "$0" >&2
    exit 2
    ;;
esac
