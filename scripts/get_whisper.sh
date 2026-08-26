#!/usr/bin/env bash
set -e
mkdir -p app/src/main/cpp
if [ ! -d app/src/main/cpp/whisper.cpp ]; then
  git clone --depth 1 https://github.com/ggml-org/whisper.cpp.git app/src/main/cpp/whisper.cpp
fi
echo "whisper.cpp baixado."
