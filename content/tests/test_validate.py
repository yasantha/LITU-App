"""Unit tests for the automatic checks in validate.py (spec section 10). Run: python -m unittest discover content/tests"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "pipeline"))

from validate import check_question, near_duplicates  # noqa: E402

SECTIONS = {"CH3-TUD": None}


def q(**over):
    base = {
        "id": "Q-CH3-TUD-001", "section_id": "CH3-TUD", "type": "single",
        "stem": "Which king broke away from the Church of Rome?",
        "options": [{"label": l, "correct": l == "Henry VIII"} for l in ("Henry VIII", "Edward I", "Charles II", "James I")],
        "explanation": "Henry VIII set up the Church of England when the Pope would not let him end his first marriage.",
        "handbook_ref": "Chapter 3 · The Tudors and Stuarts", "difficulty": 2,
    }
    base.update(over)
    return base


class CheckQuestionTest(unittest.TestCase):
    def errors(self, **over):
        return check_question(q(**over), SECTIONS)[0]

    def test_valid_question_passes(self):
        self.assertEqual([], self.errors())

    def test_id_format_and_section_must_match(self):
        self.assertTrue(self.errors(id="CH3-TUD-1"))
        self.assertTrue(self.errors(id="Q-CH3-MID-001"))

    def test_answer_counts_follow_type(self):
        two_right = [{"label": l, "correct": i < 2} for i, l in enumerate("ABCD")]
        self.assertTrue(self.errors(options=two_right))
        self.assertEqual([], self.errors(type="multi", stem="Which TWO are right?", options=two_right))
        self.assertTrue(self.errors(type="truefalse", options=[{"label": "False", "correct": True}, {"label": "True", "correct": False}]))

    def test_lengths_and_banned_options(self):
        self.assertTrue(self.errors(stem="x" * 161))
        self.assertTrue(self.errors(explanation="Too short."))
        opts = q()["options"][:3] + [{"label": "All of the above", "correct": False}]
        self.assertTrue(self.errors(options=opts))

    def test_near_duplicate_stems_are_flagged(self):
        a = q()
        b = q(id="Q-CH3-TUD-002", stem="Which king broke away from the Church of Rome")
        self.assertEqual(1, len(near_duplicates([a, b])))


if __name__ == "__main__":
    unittest.main()
