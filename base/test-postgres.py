"""Exercise lifecycle locking and stale-pid handling without a live database."""
import os
from pathlib import Path
import subprocess
import tempfile
import time
import unittest

SCRIPT = Path(__file__).with_name('vb-postgres').resolve()


class PostgresLifecycleTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        self.data = self.root / 'data'
        self.data.mkdir()
        (self.data / 'PG_VERSION').write_text('16\n')
        self.env = dict(os.environ, VB_PGBIN=str(self.bin), VB_PGDATA=str(self.data),
                        VB_PGLOG=str(self.root / 'server.log'), PG_TEST=str(self.root),
                        PATH=f'{self.bin}:{os.environ["PATH"]}')
        self.stub('id', 'echo 1000')
        self.stub('postgres', 'exit 0')
        self.stub('pg_isready', 'test -e "$PG_TEST/ready"')
        self.stub('pg_ctl', '''
case "${@: -1}" in
  start)
    test ! -e /proc/$$/fd/9 || { echo 'lifecycle lock leaked to server'; exit 1; }
    echo start >> "$PG_TEST/events"
    sleep 0.1
    touch "$PG_TEST/ready"
    ;;
  stop)
    echo stop >> "$PG_TEST/events"
    touch "$PG_TEST/stopping"
    sleep 0.2
    rm -f "$PG_TEST/ready"
    ;;
esac
''')

    def stub(self, name, body):
        p = self.bin / name
        p.write_text('#!/bin/bash\nset -eu\n' + body + '\n')
        p.chmod(0o755)

    def run_pg(self, command):
        return subprocess.run(['bash', str(SCRIPT), command], env=self.env,
                              text=True, capture_output=True, timeout=10)

    def test_missing_installation_prints_diagnostic(self):
        self.env.pop('VB_PGBIN')
        self.stub('ls', 'exit 2')
        result = self.run_pg('start')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('PostgreSQL is not installed', result.stderr)

    def test_live_unready_server_keeps_pid_file(self):
        pid = self.data / 'postmaster.pid'
        pid.write_text(f'{os.getpid()}\n')
        self.stub('sleep', 'exit 0')
        result = self.run_pg('start')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(pid.read_text(), f'{os.getpid()}\n')
        self.assertFalse((self.root / 'events').exists())

    def test_dead_server_pid_is_replaced(self):
        child = subprocess.Popen(['true'])
        child.wait()
        (self.data / 'postmaster.pid').write_text(f'{child.pid}\n')
        result = self.run_pg('start')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse((self.data / 'postmaster.pid').exists())
        self.assertEqual((self.root / 'events').read_text(), 'start\n')

    def test_concurrent_starts_only_start_once(self):
        processes = [subprocess.Popen(['bash', str(SCRIPT), 'start'], env=self.env,
                                     stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                     for _ in range(6)]
        for process in processes:
            _, err = process.communicate(timeout=10)
            self.assertEqual(process.returncode, 0, err.decode())
        self.assertEqual((self.root / 'events').read_text(), 'start\n')

    def test_start_waits_for_stop_then_restarts(self):
        (self.root / 'ready').touch()
        stop = subprocess.Popen(['bash', str(SCRIPT), 'stop'], env=self.env)
        deadline = time.monotonic() + 5
        while not (self.root / 'stopping').exists() and time.monotonic() < deadline:
            time.sleep(0.01)
        self.assertTrue((self.root / 'stopping').exists())
        result = self.run_pg('start')
        self.assertEqual(stop.wait(timeout=5), 0)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.root / 'events').read_text(), 'stop\nstart\n')
        self.assertTrue((self.root / 'ready').exists())


if __name__ == '__main__':
    unittest.main()
