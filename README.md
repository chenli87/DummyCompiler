<h2> DummyCompiler: a course project compiler (http://www.michaelfranz.com/w14cs241.html) </h2>

<h3> EBNF for the language </h3>

<p> letter = “a” | “b” | ... | “z”. </p>
<p> digit = “0” | “1” | ... | “9”. </p>
<p> relOp = “==“|“!=“|“<“|“<=“|“>“|“>=“. </p>
<p> ident = letter {letter | digit}. </p>
<p> number = digit {digit}. </p>
<p> designator = ident{ "[" expression "]" }. </p>
<p> factor = designator | number | “(“ expression “)” | funcCall . </p>
<p> term = factor { (“*” | “/”) factor}. </p>
<p> expression = term {(“+” | “-”) term}. </p>
<p> relation = expression relOp expression . </p>
<p> assignment = “let” designator “<-” expression. </p>
<p> funcCall = “call” ident [ “(“ [expression { “,” expression } ] “)” ]. </p>
<p> ifStatement = “if” relation “then” statSequence [ “else” statSequence ] “fi”. </p>
<p> whileStatement = “while” relation “do” StatSequence “od”. </p>
<p> returnStatement = “return” [ expression ] . </p>
<p> statement = assignment | funcCall | ifStatement | whileStatement | returnStatement. </p>
<p> statSequence = statement { “;” statement }. </p>
<p> typeDecl = “var” | “array” “[“ number “]” { “[“ number “]” }. </p>
<p> varDecl = typeDecl indent { “,” ident } “;” . </p>
<p> funcDecl = (“function” | “procedure”) ident [formalParam] “;” funcBody “;” . </p>
<p> formalParam = “(“ [ident { “,” ident }] “)” . </p>
<p> funcBody = { varDecl } “{” [ statSequence ] “}”. </p>
<p> computation = “main” { varDecl } { funcDecl } “{” statSequence “}” “.” . </p>

<h3> Predefined Function </h3>
<p> InputNum() read a number from the standard input </p>

<h3> Predefined Procedure </h3>
<p> OutputNum(x) write a number to the standard output OutputNewLine() write a carriage return to the standard output </p>

