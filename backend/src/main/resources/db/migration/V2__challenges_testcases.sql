CREATE TABLE challenges (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    language VARCHAR(20) NOT NULL,
    room_code VARCHAR(50)
);

CREATE TABLE test_cases (
    id UUID PRIMARY KEY,
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    test_order INT NOT NULL,
    challenge_id UUID NOT NULL REFERENCES challenges(id) ON DELETE CASCADE
);

CREATE INDEX idx_challenges_difficulty ON challenges(difficulty);
CREATE INDEX idx_test_cases_challenge_id ON test_cases(challenge_id);

-- Seed: sample challenges
INSERT INTO challenges (id, title, description, difficulty, language) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Two Sum',
     'Given a list of integers and a target number, return the indices of the two numbers that add up to the target.\n\nInput: First line contains the list of integers separated by spaces. Second line contains the target integer.\nOutput: Print the two indices separated by a space (0-indexed).\n\nExample:\nInput:\n2 7 11 15\n9\nOutput:\n0 1',
     'EASY', 'PYTHON'),

    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Reverse String',
     'Given a string, return it reversed.\n\nInput: A single line containing a string.\nOutput: The reversed string.\n\nExample:\nInput:\nhello\nOutput:\nolleh',
     'EASY', 'PYTHON'),

    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'FizzBuzz',
     'Print numbers from 1 to N. For multiples of 3 print "Fizz", for multiples of 5 print "Buzz", for multiples of both print "FizzBuzz".\n\nInput: A single integer N.\nOutput: Each result on a new line.\n\nExample:\nInput:\n5\nOutput:\n1\n2\nFizz\n4\nBuzz',
     'EASY', 'PYTHON'),

    ('d4e5f6a7-b8c9-0123-defa-234567890123', 'Longest Common Subsequence',
     'Given two strings, find the length of their longest common subsequence.\n\nInput: Two lines, each containing a string.\nOutput: An integer representing the length of the LCS.\n\nExample:\nInput:\nabcde\nace\nOutput:\n3',
     'MEDIUM', 'PYTHON'),

    ('e5f6a7b8-c9d0-1234-efab-345678901234', 'Matrix Spiral Order',
     'Given an MxN matrix, return all elements in spiral order.\n\nInput: First line contains M and N. Next M lines contain N space-separated integers.\nOutput: Elements in spiral order, space-separated.\n\nExample:\nInput:\n3 3\n1 2 3\n4 5 6\n7 8 9\nOutput:\n1 2 3 6 9 8 7 4 5',
     'MEDIUM', 'PYTHON'),

    ('f6a7b8c9-d0e1-2345-fabc-456789012345', 'N-Queens',
     'Solve the N-Queens problem: place N queens on an NxN chessboard so that no two queens threaten each other. Return the number of distinct solutions.\n\nInput: A single integer N.\nOutput: The number of distinct solutions.\n\nExample:\nInput:\n4\nOutput:\n2',
     'HARD', 'PYTHON');

-- Seed: test cases for each challenge
-- Two Sum
INSERT INTO test_cases (id, input, expected_output, test_order, challenge_id) VALUES
    (gen_random_uuid(), '2 7 11 15\n9', '0 1', 1, 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'),
    (gen_random_uuid(), '3 2 4\n6', '1 2', 2, 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'),
    (gen_random_uuid(), '3 3\n6', '0 1', 3, 'a1b2c3d4-e5f6-7890-abcd-ef1234567890');

-- Reverse String
INSERT INTO test_cases (id, input, expected_output, test_order, challenge_id) VALUES
    (gen_random_uuid(), 'hello', 'olleh', 1, 'b2c3d4e5-f6a7-8901-bcde-f12345678901'),
    (gen_random_uuid(), 'Python', 'nohtyP', 2, 'b2c3d4e5-f6a7-8901-bcde-f12345678901'),
    (gen_random_uuid(), 'a', 'a', 3, 'b2c3d4e5-f6a7-8901-bcde-f12345678901');

-- FizzBuzz
INSERT INTO test_cases (id, input, expected_output, test_order, challenge_id) VALUES
    (gen_random_uuid(), '5', '1\n2\nFizz\n4\nBuzz', 1, 'c3d4e5f6-a7b8-9012-cdef-123456789012'),
    (gen_random_uuid(), '15', '1\n2\nFizz\n4\nBuzz\nFizz\n7\n8\nFizz\nBuzz\n11\nFizz\n13\n14\nFizzBuzz', 2, 'c3d4e5f6-a7b8-9012-cdef-123456789012');

-- LCS
INSERT INTO test_cases (id, input, expected_output, test_order, challenge_id) VALUES
    (gen_random_uuid(), 'abcde\nace', '3', 1, 'd4e5f6a7-b8c9-0123-defa-234567890123'),
    (gen_random_uuid(), 'abc\nabc', '3', 2, 'd4e5f6a7-b8c9-0123-defa-234567890123'),
    (gen_random_uuid(), 'abc\ndef', '0', 3, 'd4e5f6a7-b8c9-0123-defa-234567890123');

-- Matrix Spiral
INSERT INTO test_cases (id, input, expected_output, test_order, challenge_id) VALUES
    (gen_random_uuid(), '3 3\n1 2 3\n4 5 6\n7 8 9', '1 2 3 6 9 8 7 4 5', 1, 'e5f6a7b8-c9d0-1234-efab-345678901234'),
    (gen_random_uuid(), '2 3\n1 2 3\n4 5 6', '1 2 3 6 5 4', 2, 'e5f6a7b8-c9d0-1234-efab-345678901234');

-- N-Queens
INSERT INTO test_cases (id, input, expected_output, test_order, challenge_id) VALUES
    (gen_random_uuid(), '4', '2', 1, 'f6a7b8c9-d0e1-2345-fabc-456789012345'),
    (gen_random_uuid(), '1', '1', 2, 'f6a7b8c9-d0e1-2345-fabc-456789012345'),
    (gen_random_uuid(), '8', '92', 3, 'f6a7b8c9-d0e1-2345-fabc-456789012345');
