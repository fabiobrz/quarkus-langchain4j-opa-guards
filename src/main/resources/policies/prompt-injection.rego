package promptinjection

import rego.v1

default injection_score := 0.0

allow := result if {
	result := injection_score <= 0.7
}

injection_score := score if {
	input.text
	patterns := [
		{"pattern": "ignore previous", "weight": 1.0},
		{"pattern": "reveal the secret", "weight": 0.95},
		{"pattern": "you are being hacked", "weight": 0.9},
		{"pattern": "cancel my booking", "weight": 0.0},
	]

	text_lower := lower(input.text)
	matches := [weight |
		pattern := patterns[_]
		contains(text_lower, pattern.pattern)
		weight := pattern.weight
	]

	score := max(array.concat(matches, [0.0]))
}
