package promptinjection

import rego.v1

default injection_score := 0.0

allow := result if {
	result := injection_score <= 0.7
}

injection_score := score if {
	input.text
	text_lower := lower(input.text)

	exact_patterns := [
		{"pattern": "ignore previous", "weight": 1.0},
		{"pattern": "disregard previous", "weight": 1.0},
		{"pattern": "forget your instructions", "weight": 1.0},
		{"pattern": "override your instructions", "weight": 1.0},
		{"pattern": "ignore all prior", "weight": 1.0},
		{"pattern": "new instructions", "weight": 0.95},
		{"pattern": "reveal the secret", "weight": 0.95},
		{"pattern": "system prompt", "weight": 0.95},
		{"pattern": "reveal your prompt", "weight": 0.95},
		{"pattern": "show your instructions", "weight": 0.95},
		{"pattern": "you are being hacked", "weight": 0.9},
		{"pattern": "do as i say", "weight": 0.9},
		{"pattern": "obey me", "weight": 0.9},
		{"pattern": "pretend you are", "weight": 0.85},
	]

	regex_patterns := [
		{"pattern": "friend\\s+of\\s+(the|da|tha|de)\\s+owner", "weight": 0.85},
		{"pattern": "listen\\s+to\\s+m(y|e)\\s+(instructions|commands|orders)", "weight": 0.9},
		{"pattern": "follow\\s+m(y|e)\\s+(instructions|commands|orders)", "weight": 0.9},
		{"pattern": "i\\s+(am|m)\\s+(an?\\s+)?admin", "weight": 0.85},
		{"pattern": "i\\s+have\\s+special\\s+access", "weight": 0.85},
		{"pattern": "act\\s+as\\s+(if|though)", "weight": 0.8},
		{"pattern": "trust\\s+me\\s+and", "weight": 0.8},
		{"pattern": "(delete|remove|drop|cancel)\\s+all\\s+(the\\s+)?(bookings|reservations|data|records)", "weight": 0.85},
	]

	exact_matches := [weight |
		p := exact_patterns[_]
		contains(text_lower, p.pattern)
		weight := p.weight
	]

	regex_matches := [weight |
		p := regex_patterns[_]
		regex.match(p.pattern, text_lower)
		weight := p.weight
	]

	all_matches := array.concat(exact_matches, regex_matches)
	score := max(array.concat(all_matches, [0.0]))
}
