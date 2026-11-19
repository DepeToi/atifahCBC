#!/usr/bin/env node
/**
 * PreToolUse(Bash) 守卫。
 * 破坏性命令直接拒绝；影响共享状态的命令转为人工确认。
 * 未命中规则时静默放行（退出码 0，无输出）。
 */
let raw = "";

process.stdin.setEncoding("utf8");
process.stdin.on("data", (chunk) => {
  raw += chunk;
});
process.stdin.on("end", () => {
  const verdict = judge(raw);
  if (!verdict) {
    return;
  }
  process.stdout.write(
    JSON.stringify({
      hookSpecificOutput: {
        hookEventName: "PreToolUse",
        permissionDecision: verdict.mode,
        permissionDecisionReason: verdict.reason,
      },
    })
  );
});

function judge(input) {
  let payload;
  try {
    payload = JSON.parse(input);
  } catch {
    return null;
  }
  const command = payload && payload.tool_input && payload.tool_input.command;
  if (typeof command !== "string" || command.trim() === "") {
    return null;
  }
  const cmd = command.toLowerCase();

  for (const rule of DENY) {
    if (rule.test.test(cmd)) {
      return { mode: "deny", reason: rule.reason };
    }
  }
  for (const rule of ASK) {
    if (rule.test.test(cmd) && !(rule.exempt && rule.exempt.test(cmd))) {
      return { mode: "ask", reason: rule.reason };
    }
  }
  return null;
}

const DENY = [
  {
    test: /docker[^|;&]*\b(down|rm)\b[^|;&]*-v\b/,
    reason: "该命令会删除 Docker 数据卷（MySQL/Redis/Nacos 数据）。需要清库时请先确认并手动执行。",
  },
  {
    test: /docker\s+volume\s+(rm|prune)\b/,
    reason: "删除数据卷不可恢复，已阻止。",
  },
  {
    test: /docker\s+system\s+prune\b/,
    reason: "系统级清理会删除未使用镜像/卷/网络，已阻止。",
  },
  {
    test: /git\s+clean\s+-[a-z]*[fdx]{2,}/,
    reason: "git clean -fdx 会删除全部未跟踪文件（含本地配置与产物），已阻止。",
  },
  {
    test: /\brm\s+-[a-z]*r[a-z]*f?\b[^|;&]*\s(\/|~)(\s|$)/,
    reason: "递归删除根目录或家目录，已阻止。",
  },
  {
    test: /\brm\s+-[a-z]*r[a-z]*f?\b[^|;&]*\.git(\s|$)/,
    reason: "删除 .git 会丢失全部版本历史，已阻止。",
  },
  {
    test: /\b(cat|head|tail|less|more|strings|base64)\b[^|;&]*\.env(\s|$|["')])/,
    reason: "禁止在命令中读取 .env（可能含密钥）。请让用户确认具体配置项。",
  },
  {
    test: /\b(cat|head|tail|less|more|strings)\b[^|;&]*(\.pem|\.key|id_rsa)/,
    reason: "禁止读取私钥/证书文件。",
  },
];

const ASK = [
  {
    test: /\bgit\s+push\b/,
    reason: "推送会改动远端共享分支，请确认目标分支与提交内容。",
    exempt: /--dry-run/,
  },
  {
    test: /\bgit\s+reset\s+--hard\b/,
    reason: "git reset --hard 会丢弃未提交改动，请确认。",
  },
  {
    test: /\bgit\s+(rebase|filter-branch)\b/,
    reason: "会重写历史，请确认。",
  },
  {
    test: /\bmvn\b[^|;&]*\bdeploy\b/,
    reason: "mvn deploy 会发布制品到远程仓库，请确认版本与仓库。",
  },
  {
    test: /\bdocker\s+push\b/,
    reason: "会推送镜像到镜像仓库，请确认 tag 与目标仓库。",
  },
  {
    test: /\bgh\s+(pr|release)\b/,
    reason: "会创建 PR / Release 等对外可见的操作，请确认。",
  },
];
