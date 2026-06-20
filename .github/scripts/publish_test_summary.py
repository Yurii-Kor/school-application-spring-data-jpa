#!/usr/bin/env python3

import argparse
import os
import sys
import xml.etree.ElementTree as element_tree
from pathlib import Path
from typing import Iterable


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Publish a summary from Maven Surefire XML reports."
    )
    parser.add_argument(
        "--reports-directory",
        default="target/surefire-reports",
        help="Directory containing TEST-*.xml Surefire reports.",
    )
    parser.add_argument(
        "--title",
        default="Test results",
        help="Heading used in the GitHub Actions job summary.",
    )
    return parser.parse_args()


def local_tag_name(tag: str) -> str:
    return tag.rsplit("}", maxsplit=1)[-1]


def find_test_suites(
        root: element_tree.Element,
) -> Iterable[element_tree.Element]:
    if local_tag_name(root.tag) == "testsuite":
        return [root]

    return [
        element
        for element in root.iter()
        if local_tag_name(element.tag) == "testsuite"
    ]


def read_integer_attribute(
        suite: element_tree.Element,
        attribute: str,
) -> int:
    value = suite.attrib.get(attribute, "0")

    try:
        return int(value)
    except ValueError as error:
        raise ValueError(
            f"Invalid {attribute!r} value {value!r} "
            f"in test suite {suite.attrib.get('name', '')!r}."
        ) from error


def read_float_attribute(
        suite: element_tree.Element,
        attribute: str,
) -> float:
    value = suite.attrib.get(attribute, "0")

    try:
        return float(value)
    except ValueError as error:
        raise ValueError(
            f"Invalid {attribute!r} value {value!r} "
            f"in test suite {suite.attrib.get('name', '')!r}."
        ) from error


def write_github_summary(summary: str) -> None:
    summary_path = os.getenv("GITHUB_STEP_SUMMARY")

    if not summary_path:
        return

    with Path(summary_path).open(
            mode="a",
            encoding="utf-8",
    ) as summary_file:
        summary_file.write(summary)
        summary_file.write("\n")


def main() -> int:
    arguments = parse_arguments()
    reports_directory = Path(arguments.reports_directory)
    reports = sorted(reports_directory.glob("TEST-*.xml"))

    totals = {
        "tests": 0,
        "failures": 0,
        "errors": 0,
        "skipped": 0,
    }

    test_suite_count = 0
    duration = 0.0

    try:
        for report in reports:
            root = element_tree.parse(report).getroot()
            suites = list(find_test_suites(root))

            if not suites:
                raise ValueError(
                    f"No testsuite element found in {report}."
                )

            test_suite_count += len(suites)

            for suite in suites:
                for attribute in totals:
                    totals[attribute] += read_integer_attribute(
                        suite,
                        attribute,
                    )

                duration += read_float_attribute(suite, "time")

    except (element_tree.ParseError, OSError, ValueError) as error:
        print(
            f"Failed to read Surefire reports: {error}",
            file=sys.stderr,
        )
        return 1

    passed = max(
        0,
        totals["tests"]
        - totals["failures"]
        - totals["errors"]
        - totals["skipped"],
    )

    summary = f"""## {arguments.title}

| Result | Count |
|---|---:|
| Report files | {len(reports)} |
| Test suites | {test_suite_count} |
| Total tests | {totals["tests"]} |
| Passed | {passed} |
| Failures | {totals["failures"]} |
| Errors | {totals["errors"]} |
| Skipped | {totals["skipped"]} |
| Reported duration | {duration:.3f} s |
"""

    print(summary)
    write_github_summary(summary)

    if not reports:
        print(
            f"No Surefire XML reports were found in "
            f"{reports_directory}.",
            file=sys.stderr,
        )
        return 1

    if totals["tests"] == 0:
        print(
            "Surefire reports contain zero executed tests.",
            file=sys.stderr,
        )
        return 1

    if totals["failures"] > 0 or totals["errors"] > 0:
        print(
            "One or more tests failed or ended with an error.",
            file=sys.stderr,
        )
        return 1

    print("All tests completed successfully.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
