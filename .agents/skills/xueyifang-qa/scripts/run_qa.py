#!/usr/bin/env python3
"""
学艺坊分层 QA 统一执行器

用法:
    python .agents/skills/xueyifang-qa/scripts/run_qa.py --check
    python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer unit
    python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer api
    python .agents/skills/xueyifang-qa/scripts/run_qa.py --suite trade   # trade | admin | gaps
"""
import argparse
import os
import shutil
import socket
import subprocess
import sys
from pathlib import Path
from urllib.parse import urlparse

SKILL_DIR = Path(__file__).resolve().parent.parent
SCRIPTS_DIR = Path(__file__).resolve().parent
REPO_ROOT = SKILL_DIR.parent.parent.parent
BACKEND_DIR = REPO_ROOT / "xueyifang-backend"
FRONTEND_DIR = REPO_ROOT / "xueyifang-frontend"

API_SUITES = {
    "trade": ("买卖交易/退款/库存全流程集成测试", SCRIPTS_DIR / "trade_integration_test.py"),
    "admin": ("管理后台全量接口测试", SCRIPTS_DIR / "test_admin.py"),
    "gaps": ("认证白名单与边界覆盖测试", SCRIPTS_DIR / "test_coverage_gaps.py"),
}


def check_port(host: str, port: int, timeout: float = 1.0) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(timeout)
        return sock.connect_ex((host, port)) == 0


def get_backend_host_port() -> tuple[str, int]:
    base = os.environ.get("XUEYIFANG_API_BASE", "http://127.0.0.1:8080/api")
    parsed = urlparse(base)
    return (parsed.hostname or "127.0.0.1", parsed.port or 8080)


def print_preflight() -> dict[str, bool]:
    be_host, be_port = get_backend_host_port()
    services = {
        "MySQL (3306)": ("127.0.0.1", int(os.environ.get("DB_PORT", "3306"))),
        "Redis (6379)": ("127.0.0.1", int(os.environ.get("REDIS_PORT", "6379"))),
        f"Backend ({be_port})": (be_host, be_port),
        "Frontend (3000)": ("127.0.0.1", 3000),
    }
    status: dict[str, bool] = {}
    print("=== 环境端口检查 ===")
    for label, (host, port) in services.items():
        ok = check_port(host, port)
        status[label] = ok
        print(f"  [{'UP' if ok else 'DOWN'}] {label}")
    return status


def run_cmd(cmd: list[str], cwd: Path, title: str) -> int:
    print(f"\n=== {title} ===")
    print(f"$ {' '.join(cmd)}  (cwd={cwd})")
    # Windows 下 npm/mvn 可能是 .cmd，需要通过 shell 或 shutil.which 解析完整路径
    exe = shutil.which(cmd[0])
    resolved = [exe, *cmd[1:]] if exe else cmd
    proc = subprocess.run(resolved, cwd=str(cwd))
    return proc.returncode


def run_unit_layer() -> int:
    rc1 = run_cmd(["mvn", "-q", "test"], BACKEND_DIR, "后端单元测试 (mvn test)")
    if rc1 != 0:
        return rc1
    rc2 = run_cmd(["npm", "run", "build"], FRONTEND_DIR, "前端类型检查与构建 (npm run build)")
    return rc2


def run_api_suites(keys: list[str]) -> int:
    status = print_preflight()
    _, be_port = get_backend_host_port()
    if not status[f"Backend ({be_port})"]:
        print(f"\n[BLOCKED] 后端未启动 (127.0.0.1:{be_port} 不可达)。")
        print("请先启动 MySQL/Redis 后运行: cd xueyifang-backend && mvn spring-boot:run")
        return 2

    failed_suites: list[str] = []
    for key in keys:
        title, script = API_SUITES[key]
        rc = run_cmd([sys.executable, str(script)], SCRIPTS_DIR, title)
        if rc != 0:
            failed_suites.append(key)

    print("\n=== API 集成测试汇总 ===")
    if failed_suites:
        print(f"失败套件: {', '.join(failed_suites)}")
        return 1
    print(f"全部 {len(keys)} 个套件通过")
    return 0


def main() -> int:
    if sys.platform == "win32":
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(description="学艺坊分层 QA 执行器")
    parser.add_argument("--check", action="store_true", help="仅检查本地端口状态")
    parser.add_argument("--layer", choices=["unit", "api", "all"], help="按测试层级运行")
    parser.add_argument("--suite", choices=list(API_SUITES.keys()), help="运行单个 API 集成测试套件")
    args = parser.parse_args()

    if args.check:
        print_preflight()
        return 0
    if args.suite:
        return run_api_suites([args.suite])
    if args.layer == "unit":
        return run_unit_layer()
    if args.layer == "api":
        return run_api_suites(list(API_SUITES.keys()))
    if args.layer == "all":
        rc = run_unit_layer()
        if rc != 0:
            return rc
        return run_api_suites(list(API_SUITES.keys()))

    parser.print_help()
    return 0


if __name__ == "__main__":
    sys.exit(main())
