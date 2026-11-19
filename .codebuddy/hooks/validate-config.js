#!/usr/bin/env node
/**
 * PostToolUse(Write|Edit) 守卫：写入后校验 JSON / YAML 基本语法。
 * 只做无依赖检查：JSON 用 JSON.parse，YAML 检查缩进是否混入 Tab。
 * 有错时退出码 2，把修复提示回传给 Agent（文件已写入，需要主动修正）。
 */
const fs = require("fs");
const path = require("path");

let raw = "";

process.stdin.setEncoding("utf8");
process.stdin.on("data", (chunk) => {
  raw += chunk;
});
process.stdin.on("end", () => {
  let payload;
  try {
    payload = JSON.parse(raw);
  } catch {
    return;
  }

  const input = payload.tool_input || {};
  const filePath = input.file_path || (payload.tool_response && payload.tool_response.filePath);
  if (!filePath) {
    return;
  }

  const normalized = String(filePath).replace(/\\/g, "/");
  if (/(^|\/)(node_modules|target|dist|logs|.idea)(\/|$)/.test(normalized)) {
    return;
  }
  if (/(package-lock\.json|tsconfig.*\.json|\.code-workspace)$/.test(normalized)) {
    return;
  }

  const resolved = path.isAbsolute(filePath)
    ? filePath
    : path.resolve(payload.cwd || process.cwd(), filePath);

  let content;
  try {
    content = fs.readFileSync(resolved, "utf8");
  } catch {
    return;
  }
  if (content.trim() === "") {
    return;
  }

  const ext = path.extname(resolved).toLowerCase();
  if (ext === ".json") {
    try {
      JSON.parse(content);
    } catch (error) {
      fail(resolved, "JSON 语法错误：" + error.message + "。请修正后重新写入。");
    }
    return;
  }

  if (ext === ".yml" || ext === ".yaml") {
    const lines = content.split(/\r?\n/);
    for (let i = 0; i < lines.length; i += 1) {
      if (/^ *\t/.test(lines[i])) {
        fail(resolved, "YAML 第 " + (i + 1) + " 行缩进使用了 Tab，Spring 配置会解析失败，请改为空格。");
        return;
      }
    }
  }
});

function fail(file, message) {
  process.stdout.write(message + "\n文件：" + file + "\n");
  process.exit(2);
}
