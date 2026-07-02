package promptinjection

import rego.v1

default pattern_score := 0.0

pattern_score := score if {
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
		{"pattern": "do anything now", "weight": 0.95},
		{"pattern": "from now on you are", "weight": 0.9},
		{"pattern": "you are now", "weight": 0.85},
		{"pattern": "developer mode", "weight": 0.9},
		{"pattern": "debug mode", "weight": 0.85},
		{"pattern": "maintenance mode", "weight": 0.85},
		{"pattern": "roleplay as", "weight": 0.85},
		{"pattern": "let's play a game", "weight": 0.8},
		{"pattern": "let's pretend", "weight": 0.8},
		{"pattern": "bypass", "weight": 0.8},
		{"pattern": "jailbreak", "weight": 0.95},
		{"pattern": "ignore safety", "weight": 0.95},
		{"pattern": "ignore restrictions", "weight": 0.95},
		{"pattern": "no restrictions", "weight": 0.9},
		{"pattern": "without limitations", "weight": 0.85},
		{"pattern": "uncensored", "weight": 0.9},
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
		{"pattern": "ignore\\s+(all\\s+)?(your|the|these)\\s+(rules|guidelines|policies)", "weight": 0.95},
		{"pattern": "you\\s+(must|should|have\\s+to)\\s+obey", "weight": 0.9},
		{"pattern": "speak\\s+(only\\s+)?in\\s+[a-z]+\\s+(from\\s+now|going\\s+forward)", "weight": 0.8},
		{"pattern": "(secret|hidden|special)\\s+(code|password|key|token)", "weight": 0.85},
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

	suspicious_words := [
		{"word": "password", "weight": 0.4},
		{"word": "admin", "weight": 0.4},
		{"word": "secret", "weight": 0.4},
		{"word": "credentials", "weight": 0.4},
		{"word": "token", "weight": 0.4},
		{"word": "api key", "weight": 0.4},
		{"word": "database", "weight": 0.3},
		{"word": "hack", "weight": 0.5},
		{"word": "exploit", "weight": 0.5},
		{"word": "vulnerability", "weight": 0.4},
		{"word": "root access", "weight": 0.5},
		{"word": "sudo", "weight": 0.4},
		{"word": "ssh", "weight": 0.3},
		{"word": "confidential", "weight": 0.3},
		{"word": "classified", "weight": 0.3},
		{"word": "all customer", "weight": 0.4},
		{"word": "all user", "weight": 0.4},
		{"word": "show me all", "weight": 0.3},
		{"word": "give me all", "weight": 0.3},
		{"word": "dump", "weight": 0.4},
		{"word": "exfiltrate", "weight": 0.5},
		{"word": "all instructions above", "weight": 0.5},
		{"word": "different assistant", "weight": 0.5},
		{"word": "tell me the", "weight": 0.3},
	]

	suspicious_matches := [weight |
		w := suspicious_words[_]
		contains(text_lower, w.word)
		weight := w.weight
	]

	all_matches := array.concat(array.concat(exact_matches, regex_matches), suspicious_matches)
	score := max(array.concat(all_matches, [0.0]))
}

injection_score := pattern_score if {
	pattern_score > 0.7
} else := llm_score(input.text) if {
	pattern_score > 0.0
	pattern_score <= 0.7
} else := 0.0

allow := result if {
	result := injection_score <= 0.7
}
