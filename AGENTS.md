# Instructions for benchmark runs

Before launching any Claude Code benchmark run on this server, load the saved
OAuth token and force subscription authentication in the launching process:

```bash
export CLAUDE_CODE_OAUTH_TOKEN="$(cat "$HOME/.config/vaadinbench/claude-oauth-token")"
export CLAUDE_FORCE_OAUTH=1
```

Check that the token file is readable and nonempty before launching. If it is
missing or empty, report that setup is required; do not silently fall back to an
API key. Keep the token private: never print it, commit it, or put it in command
arguments. Do not enable shell tracing while loading or using the token.

Pass these variables through the launcher's environment, including for runs
started from scripts or services. Do not rely on `.bashrc` being sourced. Do not
pass either variable through Harbor's `--ae` option: its secret redaction can
corrupt JSON result files.
