import { spawnSync } from 'node:child_process';

const composeFile = new URL('../../deploy/compose.e2e.yaml', import.meta.url).pathname;
const composeProject = 'docvault-e2e';

const processPatterns = [
  'spring-boot:run.*spring.profiles.active=e2e',
  'DocvaultApplication --spring.profiles.active=e2e',
  'ng serve --host 127.0.0.1 --port 4200',
];

for (const pattern of processPatterns) {
  spawnSync('pkill', ['-TERM', '-f', pattern], { stdio: 'ignore' });
}

const down = spawnSync(
  'docker',
  ['compose', '-p', composeProject, '-f', composeFile, 'down', '--remove-orphans'],
  { stdio: 'inherit' }
);

process.exit(down.status ?? 0);
