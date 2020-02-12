grammar XQuery;

ap
	: 'doc' '(' '"' fileName '"' ')' '/' rp           # apSlash
	| 'doc' '(' '"' fileName '"' ')' '//' rp          # apDoubleSlash
	;

fileName
	: NAME ('.' NAME)?
	;

rp
	: NAME                         # rpName
	| '*'                          # rpStar
	| '.'                          # rpDot
	| '..'                         # rpDoubleDot
	| 'text()'                     # rpText
	| '@' NAME                     # rpAttribute
	| '(' rp ')'                   # rpParentheses
	| rp '/' rp                    # rpSlash
	| rp '//' rp                   # rpDoubleSlash
	| rp '[' f ']'                 # rpFilter
	| rp ',' rp                    # rpComma
	;

f
	: rp                           # filterRp
	| rp '=' rp                    # filterEqual
	| rp 'eq' rp                   # filterEqual
	| rp '==' rp                   # filterIs
	| rp 'is' rp                   # filterIs
	| '(' f ')'                    # filterParentheses
	| f 'and' f                    # filterAnd
	| f 'or' f                     # filterOr
	| 'not' f                      # filterNot
	;
	
xq
	: Var							# xqVar
	| StringConstant				# xqStringConstant
	| ap							# xqAp
	| '(' xq ')'					# xqParentheses
	| xq ',' xq						# xqComma
	| xq '/' rp						# xqSlash
	| xq '//' rp					# xqDoubleSlash
	| '<' NAME '>' '{' xq '}' '<' '/' NAME '>'	# xqName
	| forClause letClause? whereClause? returnClause		# xqFLWR
	| letClause xq						# xqLet
	;

forClause
	: 'for' Var 'in' xq (',' Var 'in' xq)	# forXQ
	;
	
letClause
	: 'let' Var ':=' xq (',' Var ':=' xq)	# letXQ
	;
	
whereClause
	: 'where' cond					# whereCond
	;

returnClause
	: 'return' xq					# returnXQ
	;

cond
	: xq '=' xq						# condEqual
	| xq 'eq' xq					# condEqual
	| xq '==' xq					# condIs
	| xq 'is' xq					# condIs
	| 'empty' '(' xq ')'			# condEmpty
	| 'some' Var 'in' xq (',' Var 'in' xq)* 'satisfies' cond	# condSatisfy
	| '(' cond ')'					# condParentheses
	| cond 'and' cond				# condAnd
	| cond 'or' cond				# condOr
	| 'not' cond					# condNot
	;


NAME: [a-zA-Z0-9_-_:]+;
Var: '$' NAME;
StringConstant: '"'+[a-zA-Z0-9,.!?; ''""-]+'"';
WS : [ \t\r\n]+ -> skip;