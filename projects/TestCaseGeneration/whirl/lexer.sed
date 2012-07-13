#!/bin/sed -f

# Lexer for Whirl

# Usage:  $ cat program.wrl | sed -f lexer.sed > program.k.wrl




# Remove everything except '0' and '1'
s/[^01]//g

# Put spaces after '0' and '1'
s/./& /g



# Append "end" to end
$aend 

# Delete blank lines
/^$/d
