#!/usr/bin/env bash
# Copyright 2026 IgnitronDev
# SPDX-License-Identifier: Apache-2.0
#
# Installs the simulator build on the newest iPhone and iPad simulators, launches it, takes light
# and dark screenshots and fails if the app does not stay running.
# Usage: ios-simulator-smoke.sh <path to FocusFlow.app> <screenshot folder>
set -euo pipefail

APP="$1"
OUT="$2"
BUNDLE_ID=com.ignitrondev.focusflow
mkdir -p "$OUT"

# The newest available simulator whose name starts with the given prefix.
pick() {
  xcrun simctl list devices available -j | python3 -c '
import json, sys
prefix = sys.argv[1]
runtimes = json.load(sys.stdin)["devices"]
def version(key):
    return [int(p) for p in key.rsplit("iOS-", 1)[-1].split("-") if p.isdigit()]
for key in sorted((k for k in runtimes if "iOS" in k), key=version, reverse=True):
    for device in runtimes[key]:
        if device["name"].startswith(prefix):
            print(device["udid"], key.rsplit(".", 1)[-1], device["name"].replace(" ", "_"), sep="|")
            sys.exit(0)
sys.exit("no " + prefix + " simulator")
' "$1"
}

run_on() {
  IFS='|' read -r udid runtime name <<<"$(pick "$1")"
  echo "== $name ($runtime, $udid)"
  xcrun simctl boot "$udid" 2>/dev/null || true
  xcrun simctl bootstatus "$udid" -b >/dev/null
  xcrun simctl install "$udid" "$APP"
  for appearance in light dark; do
    xcrun simctl ui "$udid" appearance "$appearance"
    xcrun simctl terminate "$udid" "$BUNDLE_ID" 2>/dev/null || true
    pid=$(xcrun simctl launch "$udid" "$BUNDLE_ID" | awk '{print $NF}')
    sleep 15
    xcrun simctl io "$udid" screenshot "$OUT/${name}_${runtime}_${appearance}.png" >/dev/null
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "::error::FocusFlow stopped running on $name ($appearance)"
      xcrun simctl spawn "$udid" log show --last 2m --style compact --predicate 'process == "FocusFlow"' | tail -n 80 || true
      exit 1
    fi
    echo "running after 15 s ($appearance), pid $pid"
  done
  xcrun simctl shutdown "$udid"
}

run_on "iPhone"
run_on "iPad"
ls -l "$OUT"
