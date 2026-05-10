import { spawn, spawnSync } from 'node:child_process';
import http from 'node:http';
import net from 'node:net';
import { setTimeout as delay } from 'node:timers/promises';

const root = new URL('../..', import.meta.url).pathname;
const frontendDir = new URL('..', import.meta.url).pathname;
const backendDir = new URL('../../backend', import.meta.url).pathname;
const composeFile = new URL('../../deploy/compose.e2e.yaml', import.meta.url).pathname;
const composeProject = 'docvault-e2e';

const children = [];
let cleaningUp = false;

function run(command, args, options = {}) {
  const child = spawn(command, args, {
    cwd: options.cwd ?? root,
    stdio: ['ignore', 'pipe', 'pipe'],
    detached: true,
    env: { ...process.env, ...(options.env ?? {}) },
  });
  children.push(child);
  child.stdout.on('data', data => process.stdout.write(`[${options.name ?? command}] ${data}`));
  child.stderr.on('data', data => process.stderr.write(`[${options.name ?? command}] ${data}`));
  child.on('exit', code => {
    if (!cleaningUp && code !== 0) {
      console.error(`[${options.name ?? command}] exited with code ${code}`);
      cleanup(code ?? 1);
    }
  });
  return child;
}

function runOnce(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd ?? root,
    stdio: 'inherit',
    env: process.env,
  });
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

async function waitForHttp(url, timeoutMs = 180_000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await canFetch(url)) return;
    await delay(500);
  }
  throw new Error(`Timed out waiting for ${url}`);
}

function canFetch(url) {
  return new Promise(resolve => {
    const req = http.get(url, res => {
      res.resume();
      resolve(res.statusCode >= 200 && res.statusCode < 500);
    });
    req.on('error', () => resolve(false));
    req.setTimeout(1000, () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function waitForTcp(host, port, timeoutMs = 120_000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await canConnect(host, port)) return;
    await delay(500);
  }
  throw new Error(`Timed out waiting for ${host}:${port}`);
}

function canConnect(host, port) {
  return new Promise(resolve => {
    const socket = net.createConnection({ host, port }, () => {
      socket.end();
      resolve(true);
    });
    socket.on('error', () => resolve(false));
    socket.setTimeout(1000, () => {
      socket.destroy();
      resolve(false);
    });
  });
}

async function cleanup(code = 0) {
  if (cleaningUp) return;
  cleaningUp = true;

  for (const child of children.reverse()) {
    try {
      process.kill(-child.pid, 'SIGTERM');
    } catch {
      // Process already exited.
    }
  }

  runOnce('docker', ['compose', '-p', composeProject, '-f', composeFile, 'down', '--remove-orphans']);
  process.exit(code);
}

process.on('SIGINT', () => cleanup(130));
process.on('SIGTERM', () => cleanup(143));
process.on('exit', () => {
  for (const child of children.reverse()) {
    try {
      process.kill(-child.pid, 'SIGTERM');
    } catch {
      // Process already exited.
    }
  }
});

runOnce('docker', ['compose', '-p', composeProject, '-f', composeFile, 'up', '-d', '--remove-orphans']);
await waitForTcp('127.0.0.1', 55432);
await waitForHttp('http://127.0.0.1:59200');

run('./mvnw', [
  '-q',
  '-DskipTests',
  'spring-boot:run',
  '-Dspring-boot.run.arguments=--spring.profiles.active=e2e',
], { cwd: backendDir, name: 'backend' });
await waitForHttp('http://127.0.0.1:8080/api/actuator/health', 240_000);

run('npm', ['start', '--', '--host', '127.0.0.1', '--port', '4200'], {
  cwd: frontendDir,
  name: 'frontend',
});
await waitForHttp('http://127.0.0.1:4200', 180_000);

console.log('[e2e] Real DocVault stack is ready');
setInterval(() => {}, 60_000);
