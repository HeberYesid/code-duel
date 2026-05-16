"""
Code Duel — Sandboxed test runner.
Executed INSIDE the Docker container.
Reads test cases from /app/test_cases.json, runs /app/solution.py for each,
and outputs a JSON array of results to stdout.
"""
import subprocess
import json
import sys
import time
import os

SOLUTION_PATH = "/app/solution.py"
TEST_CASES_PATH = "/app/test_cases.json"


def run_test(test_case, timeout_seconds):
    """Run solution.py with the given input and capture output."""
    start = time.time()
    try:
        proc = subprocess.run(
            [sys.executable, SOLUTION_PATH],
            input=test_case["input"],
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
        elapsed_ms = int((time.time() - start) * 1000)
        return {
            "testOrder": test_case["testOrder"],
            "stdout": proc.stdout,
            "stderr": proc.stderr,
            "exitCode": proc.returncode,
            "executionTimeMs": elapsed_ms,
            "timedOut": False,
        }
    except subprocess.TimeoutExpired:
        elapsed_ms = int((time.time() - start) * 1000)
        return {
            "testOrder": test_case["testOrder"],
            "stdout": "",
            "stderr": "Time Limit Exceeded",
            "exitCode": -1,
            "executionTimeMs": elapsed_ms,
            "timedOut": True,
        }
    except Exception as e:
        elapsed_ms = int((time.time() - start) * 1000)
        return {
            "testOrder": test_case["testOrder"],
            "stdout": "",
            "stderr": str(e),
            "exitCode": -1,
            "executionTimeMs": elapsed_ms,
            "timedOut": False,
        }


def main():
    # Read timeout from environment variable (set by Java side)
    timeout_seconds = int(os.environ.get("TEST_TIMEOUT", "5"))

    with open(TEST_CASES_PATH, "r") as f:
        test_cases = json.load(f)

    results = []
    for tc in test_cases:
        result = run_test(tc, timeout_seconds)
        results.append(result)

    # Output as JSON to stdout — Java side will parse this
    print(json.dumps(results))


if __name__ == "__main__":
    main()
