# TODO / Roadmap

Status: Stable; full-screen viewer active; per-tile Aspect Fit/Fill + watchdog implemented.

## Completed

- [x] Full-screen viewing: hide system/status bars; immersive mode.
- [x] Menu access: single tap anywhere opens camera selector.
- [x] Per-tile Aspect Fit/Fill: double tap toggles; overlay shows “Aspect Fit/Fill”.
- [x] Persistence: keep per-tile aspect mode applied across restarts/reloads; re-apply on VLC events and watchdog.
- [x] Remove global Fit/Fill control from selector; per-tile only.
- [x] Stream watchdog: restart tile after 60s of no progress; brief reconnect overlay.
- [x] Fix VLC media options compile issue.

## Next Up

- [ ] Gesture choice confirmation: keep single-tap = menu, double-tap = fit/fill? (Default now.)
- [ ] Watchdog tuning: per-tile backoff, max retries, timeout configurable in settings; light logging/metrics.

## Event-Driven Fullscreen (Requires MQTT/WS)

- [ ] Research Frigate event transports (MQTT topics and any WebSocket endpoints); document payloads.
- [ ] EventSource abstraction; choose initial backend (MQTT likely first).
- [ ] Settings: enable/disable, event types, dwell time, cooldown, priority.
- [ ] Fullscreen transition/queueing without recreating players.

## Brightness Automation (Low Priority)

- [ ] Choose approach: in-app brightness vs system brightness (permission) vs overlay dimmer.
- [ ] MQTT control surface: topic/payload schema; optional HA discovery.
- [ ] Smoothing, clamping, and manual override handling.

## Housekeeping

- [ ] Optionally add GitHub issue templates and labels; keep using this TODO for quick capture.
