# meeKeyboard — Master Plan

## Vision
Gboard-এর ছোট ভাই। Chatting-friendly। বাংলা+English একসাথে।
Privacy-first। Light (5 MB)। No tracking।

## Design Direction
Deep Current + Bengal Night hybrid:
- English mode: cyan accent (#00E5C7)
- Bangla mode: warm amber (#FFB547)
- Background: #0B1220
- Key: #151E32

## Version Roadmap
- v1.1 — Proper icons (shift, backspace, enter)       [25 min]
- v1.2 — Bottom row Gboard standard                    [20 min]
- v1.3 — Font size uniform                             [10 min]
- v1.4 — Tight spacing 2dp                             [5 min]
- v1.5 — Space swipe cursor                            [30 min]
- v1.6 — Suggestion pill                               [15 min]
- v1.7 — Emoji bottom row                              [15 min]
- v1.8 — Trackpad mode                                 [40 min]
- v1.9 — Smart autocorrect                             [1 hr]
- v2.0 — Complete + Play Store ready

## Rules
- ONE version per session
- Commit → push → build → verify
- Build fail = no next version
- NO big rewrites
- NO unverified patches
- Rest > Speed

## Current State (2026-09-21)
- 23 commits
- Working: QWERTY, বাংলা, emoji, clipboard, themes,
  background, suggestions, auto-correct, swipe prototype
- Broken: text-glyph icons (shift, backspace, enter)
- Next: v1.1 icons
