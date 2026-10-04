#!/usr/bin/env bash
# Assertions this project's documentation build must satisfy.
#
# Run by the shared site workflow in jlt-commons/ci-builds against the freshly
# built _site, with BASE_PATH exported. Lifted verbatim out of this repo's own
# site.yml when the build scaffolding moved to the shared workflow, so every
# check here predates that move and still means what it did.
#
# Run it locally the same way:
#   bb site:build && BASE_PATH=/raylib-jlt bash docs/check-site.sh

set -euo pipefail
out=_site

test -f "$out/index.html"       || { echo "no homepage generated"; exit 1; }
test -f "$out/guide/index.html" || { echo "no guide page generated"; exit 1; }
test -f "$out/css/screen.css"   || { echo "static assets missing"; exit 1; }

! grep -rq '{{site-base}}' "$out"/index.html "$out"/guide/*.html \
  || { echo "unrendered template variable"; exit 1; }

grep -q 'pre class="mermaid"' "$out/guide/color-by-value.html" \
  || { echo "mermaid fences were not rewritten"; exit 1; }

# Since engine v0.2.0 the 3.4 MB bundle loads only where a diagram
# exists. Both directions matter: a page with a diagram that does not
# load it renders unstyled source text, and a page without one that
# does costs every reader 3.4 MB for nothing.
grep -q 'mermaid.min.js' "$out/guide/color-by-value.html" \
  || { echo "a page with a diagram is not loading mermaid"; exit 1; }
! grep -q 'mermaid.min.js' "$out/guide/kwarg-drawing-api.html" \
  || { echo "a page with no diagram is loading mermaid"; exit 1; }

# The failure mode this site's base path exists to prevent. Served at
# jlt-commons.github.io/raylib-jlt/, a root-absolute URL loads the
# ORGANIZATION site's asset instead of this project's. The page still
# renders, wearing the wrong clothes, so nothing but a check catches it.
if grep -ohE '(href|src)="/[^"]*"' "$out"/index.html "$out"/404.html "$out"/guide/*.html \
     | grep -vE "=\"$BASE_PATH/"; then
  echo "the URLs above escape $BASE_PATH and would resolve against the org site"
  exit 1
fi

echo "build looks correct: every URL under $BASE_PATH"
