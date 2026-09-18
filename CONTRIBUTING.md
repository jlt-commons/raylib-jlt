# Contributing

Thanks for taking an interest. This is a suite of [raylib](https://github.com/raysan5/raylib)
examples written in [jolt](https://github.com/jolt-lang) (native Clojure, no JVM),
calling the real `libraylib` over its C ABI through `jolt.ffi`.

New examples are very welcome; the suite is deliberately mechanical to grow.

The documentation is published at <https://jlt-commons.github.io/raylib-jlt/>. It's generated from
this repo's `docs/guide/`; edit the Markdown here, never the site.

## Setting up

You need two things:

```sh
jolt --version    # jolt 0.8.0 or newer (deps.edn declares :jolt/min-version)
bb lib:check      # is the native libraylib installed for this OS/arch?
```

If `bb lib:check` says no, `bb lib:install` will install it via your platform's
package manager (brew / pacman / apt / dnf / zypper / apk), or `bb lib:install
--dry-run` prints the command it would run so you can do it yourself.

If any task stops with `this project needs jolt 0.8.0 or newer`, your jolt is below
the floor `deps.edn` declares. jolt v0.8.0 was released on 2026-09-01, so installing
a current jolt is the fix. The one exception is a build from `main` made between the
`ffi/write` change and that tag: it reports something like `v0.7.29-25-gd4e92a43`,
whose numeric prefix sorts below the floor even though it carries the new behaviour.
Prefix the command with `JOLT_SKIP_MIN_VERSION=1` in that case only. The
[README](README.md#jolt) explains why the floor exists, and why it could not have
caught an older jolt in the first place.

CI and your machine can be on different jolts, and that is deliberate rather than
a fault. The workflow pins `JOLT_VERSION` (0.8.1 at the time of writing), while
local development here tracks jolt's `main`, which runs ahead of the pin: 45
commits ahead as of 2026-09-02. So a regression introduced upstream shows up
locally and stays invisible in CI, and an upstream fix does the opposite. If a
failure you see locally goes green in CI, compare `jolt --version` on both sides
before concluding the failure was yours. To test a different runtime without
moving the pin, run the workflow via `workflow_dispatch` with the `jolt-version`
input.

[babashka](https://babashka.org) is optional but makes everything friendlier:
every example has a `bb <name>` task. Without it, use `jolt -M:<alias>` directly.

## Before you open a PR

Run the gates. All four are fast and none needs a JVM at runtime:

```sh
bb check              # headless compile-check of every example (no window opens)
bb test               # unit suite: ffi/write puts the value where the offset says
bb lint:strict        # clj-kondo over src, non-zero exit on any finding
bb lsp:format-check   # clojure-lsp formatting, dry run
```

`bb test` earns its place by catching what `bb check` structurally cannot. An
`ffi/write` argument flip swaps two integers, so the wrong call compiles exactly
as cleanly as the right one, and a compile-only gate reports green while every
write lands at the wrong address.

`bb lsp:fix` applies formatting and ns cleanup in place if `lsp:format-check`
complains. **Formatting is owned by clojure-lsp, not cljfmt**; please don't run
cljfmt over `src`, the two disagree on some compact literal tables.

If you'd like these to run automatically, `bb hooks:install` sets up a local
pre-commit hook (~2s). It's never committed, so each clone opts in.

## Adding an example

One new example touches exactly five places. The full recipe (with the source
layout, the naming rules, and a diagram) lives in the example catalog:

**[docs/guide/example-catalog.md § Adding an example: the five touchpoints](docs/guide/example-catalog.md#adding-an-example-the-five-touchpoints)**

In short: the source namespace, a `deps.edn` alias, a `check.clj` require, a row
in `scripts/examples_registry.clj`, and a `bb.edn` task.

`bb check:registration` checks all five and CI runs it. It exists because
`bb check` cannot check the third one: `bb check` compiles what `check.clj`
requires, so an example missing from that list is never compiled and the run
still prints success. Measured, rather than assumed: a namespace carrying a
deliberate unresolved symbol fails `bb check` with its require present and
passes with it removed.

Two rules worth knowing before you write any code:

- **Definitions must precede their first use.** Since jolt 0.4.0 an unresolved
  symbol is a compile error, not a late-bound reference. This bites hardest in the
  shared `src/net/b12n/raylib_jlt/raylib.clj` binding layer, where one misordered
  symbol stops *every* example from loading and only the first offender is
  reported. Fix them one at a time; `bb check` is the quick confirmation.
- **Write against the shared API, not raw FFI.** `net.b12n.raylib-jlt.raylib`
  already exposes a keyword-argument drawing layer (`rl/text!`, `rl/rect!`,
  `rl/circle!`, …) plus the color palette. Add a new binding there only if the
  example genuinely needs a raylib call nothing else uses.

## Understanding the FFI

If you're touching the binding layer rather than adding an example, read
[`docs/guide/`](docs/guide/index.md) first. Every non-obvious decision in
`raylib.clj` traces back to how a particular C struct crosses the FFI boundary,
and the answer differs per struct: `Color` packs into a `:uint`,
`Camera2D`/`Camera3D` go by pointer, and `Vector2`/`Vector3` geometry has to fall
back to rlgl immediate mode. Those three pages explain why.

Note the **x86-64 caveat** documented in
[`struct-by-value-pointer-trick.md`](docs/guide/struct-by-value-pointer-trick.md):
the `Camera2D`/`Camera3D` pointer trick is AArch64-specific. If you're on x86-64
and `camera2d` crashes with an invalid memory reference, that's the known cause,
not a bug in your setup, and a portable rlgl-matrix replacement would be a
genuinely valuable contribution.

## Verifying a windowed example without watching it

Every example honors two environment variables so it can prove itself unattended:

```sh
RAYLIB_APP_AUTO_QUIT_MS=2000 jolt -M:<alias>   # close after 2s
RAYLIB_APP_SHOT=proof.png    jolt -M:<alias>   # dump one frame as a PNG
```

`bb run-all [secs]` reels through the whole suite this way. See
[`headless-smoke-testing.md`](docs/guide/headless-smoke-testing.md) for how the
batched-geometry flush makes the screenshot non-empty.

## Demo GIFs

You don't need to record anything. Every GIF under `docs/demos/` is committed, and
`docs/demos/README.md` is generated from `scripts/demo_manifest.edn`. The `bb
record` task drives an internal capture tool that isn't publicly released, so it's
maintainer-only; it will tell you so rather than failing obscurely. If your new
example would benefit from a specific input sequence in its demo, add an
`:overrides` entry for it in the manifest and mention it in your PR; a maintainer
will record it.

`bb check:demos` runs in CI and does not ask you for a GIF. What it asks is that
every surface agrees about which examples have one, so give your new example a
row in `docs/guide/example-catalog.md` reading `*not recorded yet*` in the
preview column and leave the full-size gallery alone. The gate names the exact
row it wants if you forget. A maintainer records the GIF and flips the row in
the same pass, and the stated counts follow from what is on disk rather than
from anyone remembering to update them.

Publishing the site is automatic. `.github/workflows/site.yml` builds it on every
pull request and deploys it when your change lands on `main`, so a docs change goes
live on merge without anyone running anything. You can preview it locally with
`bb site:serve` if you clone
[jlt-commons/docs-engine](https://github.com/jlt-commons/docs-engine) alongside this
repo, but the pull request build is the authority.

## Licensing

This project is released under the Eclipse Public License 2.0 (`EPL-2.0`), the
licence used across jlt-commons; it was zlib until 2026-09-05. By contributing, you
agree your contribution is licensed under those terms. If your example is a port of
an upstream raylib example, please name the original in the README table (e.g.
`shapes/shapes_bouncing_ball`): that is both the attribution and the plain marking
of an altered source that raylib's zlib licence requires, so it has to stay
traceable.
