"""Minimal Docker stats → Prometheus exporter (works with containerd image store)."""

from __future__ import annotations

import time

import docker
from prometheus_client import Gauge, start_http_server

CPU = Gauge(
    "docker_container_cpu_usage_percent",
    "Container CPU usage percent",
    ["name", "compose_service"],
)
MEM = Gauge(
    "docker_container_memory_usage_bytes",
    "Container memory usage in bytes",
    ["name", "compose_service"],
)
MEM_LIMIT = Gauge(
    "docker_container_memory_limit_bytes",
    "Container memory limit in bytes (0 if unlimited)",
    ["name", "compose_service"],
)
NET_RX = Gauge(
    "docker_container_network_rx_bytes",
    "Container network bytes received (cumulative from docker stats)",
    ["name", "compose_service"],
)
NET_TX = Gauge(
    "docker_container_network_tx_bytes",
    "Container network bytes sent (cumulative from docker stats)",
    ["name", "compose_service"],
)
RUNNING = Gauge(
    "docker_container_running",
    "1 if container is running",
    ["name", "compose_service"],
)


def _compose_service(labels: dict) -> str:
    return labels.get("com.docker.compose.service") or labels.get("name") or "unknown"


def _cpu_percent(stats: dict) -> float:
    cpu = stats.get("cpu_stats", {})
    precpu = stats.get("precpu_stats", {})
    cpu_delta = float(cpu.get("cpu_usage", {}).get("total_usage", 0)) - float(
        precpu.get("cpu_usage", {}).get("total_usage", 0)
    )
    system_delta = float(cpu.get("system_cpu_usage", 0)) - float(precpu.get("system_cpu_usage", 0))
    online = float(cpu.get("online_cpus") or len(cpu.get("cpu_usage", {}).get("percpu_usage") or []) or 1)
    if system_delta > 0 and cpu_delta >= 0:
        return (cpu_delta / system_delta) * online * 100.0
    return 0.0


def _net_bytes(stats: dict) -> tuple[float, float]:
    networks = stats.get("networks") or {}
    rx = tx = 0.0
    for n in networks.values():
        rx += float(n.get("rx_bytes", 0))
        tx += float(n.get("tx_bytes", 0))
    return rx, tx


def collect(client: docker.DockerClient) -> None:
    seen: set[tuple[str, str]] = set()
    for c in client.containers.list():
        name = (c.name or c.short_id).lstrip("/")
        labels = c.labels or {}
        service = _compose_service(labels)
        key = (name, service)
        seen.add(key)
        try:
            stats = c.stats(stream=False)
            mem = float((stats.get("memory_stats") or {}).get("usage", 0))
            limit = float((stats.get("memory_stats") or {}).get("limit", 0))
            rx, tx = _net_bytes(stats)
            CPU.labels(name=name, compose_service=service).set(_cpu_percent(stats))
            MEM.labels(name=name, compose_service=service).set(mem)
            MEM_LIMIT.labels(name=name, compose_service=service).set(limit)
            NET_RX.labels(name=name, compose_service=service).set(rx)
            NET_TX.labels(name=name, compose_service=service).set(tx)
            RUNNING.labels(name=name, compose_service=service).set(1)
        except Exception as exc:  # noqa: BLE001 — keep exporter alive
            print(f"stats failed for {name}: {exc}", flush=True)
            RUNNING.labels(name=name, compose_service=service).set(0)

    # Clear stale series for stopped containers (best-effort)
    for metric in (CPU, MEM, MEM_LIMIT, NET_RX, NET_TX, RUNNING):
        for labels_dict, _child in list(metric._metrics.items()):  # noqa: SLF001
            key = (labels_dict[0], labels_dict[1])
            if key not in seen:
                metric.remove(*labels_dict)


def main() -> None:
    start_http_server(9487)
    print("docker-exporter listening on :9487", flush=True)
    client = docker.from_env()
    while True:
        try:
            collect(client)
        except Exception as exc:  # noqa: BLE001
            print(f"collect error: {exc}", flush=True)
        time.sleep(10)


if __name__ == "__main__":
    main()
