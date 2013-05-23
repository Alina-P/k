package org.kframework.krun.api;

import edu.uci.ics.jung.graph.DirectedGraph;
import org.kframework.compile.utils.RuleCompilerSteps;
import org.kframework.kil.Term;
import org.kframework.kil.Rule;
import org.kframework.krun.KRunExecutionException;

import java.util.Set;

/**
The interface to the KRun api. Each backend implements this interface with as much or as little of
the functionality described below as it can.
*/
public interface KRun {
	/**
	Execute a term in normal execution mode until it cannot rewrite any further
	@param cfg The term to rewrite
	@return An object containing both metadata about krun's execution, and information about
	the exit state of the execution
	@exception KRunExecutionException Thrown if the backend fails to successfully execute the
	term
	@exception UnsupportedOperationException The backend implementing this interface does not
	support execution
	*/
	public KRunResult<KRunState> run(Term cfg) throws KRunExecutionException;

	/**
	Perform a breadth-first search of the transition system starting at a particular term.
	@param bound The maximum number of search results to return; null if unbounded
	@param depth The maximum number of transitions to make before terminating; null if
	unbounded  
	@param searchType Represents the types of result states to return
	@param pattern A kompiled rule without rewrites (i.e. a pattern and a side condition) to
	use to determine whether a particular state is a search result
	@param cfg The term to begin the search at
	@param compilationInfo the object used to kompile the search pattern, which contains
	metadata used to pretty-print results
	@exception KRunExecutionException Thrown if the backend fails to successfully perform the
	search
	@exception UnsupportedOperationException The backend implementing this interface does not
	support breadth-first search
	@return An object containing both metadata about krun's execution, and information about
	the results of the search
	*/
	public KRunResult<SearchResults> search(Integer bound, Integer depth, SearchType searchType, Rule pattern, Term cfg, RuleCompilerSteps compilationInfo) throws KRunExecutionException;

	/**
	Perform LTL model-checking of a term according to a particular LTL formula
	@param formula The K term expressing the LTL formula to check
	@param cfg The initial configuration whose transitions should be model-checked
	@exception KRunExecutionException Thrown if the backend fails to successfully model-check
	the term
	@exception UnsupportedOperationException The backend implementing this interface does not
	support LTL model checking
	@return An object containing both metadata about krun's execution, and a graph containing
	the LTL counterexample if model-checking failed (null if it succeeded)
	*/
	public KRunResult<DirectedGraph<KRunState, Transition>> modelCheck(Term formula, Term cfg) throws KRunExecutionException;

	/**
	Execute a term in normal-execution mode for a specified number of steps
	@param cfg The K term to rewrite
	@param steps The maximum number of transitions to execute for (zero if you want to rewrite
	only until the first transition)
	@exception KRunExecutionException Thrown if the backend fails to successfully execute the
	term
	@exception UnsupportedOperationException The backend implementing this interface does not
	support bounded stepping
	@return An object containing both metadata about krun's execution, and information about
	the resulting term after executing the specified number of steps (or fewer if no further
	rewrites are possible)
	*/
	public KRunResult<KRunState> step(Term cfg, int steps) throws KRunExecutionException;

	/**
	Initiate a new debugger session on a particular initial configuration. This function also,
	at the discretion of the implementation, may execute a particular set of initial commands,
	such as performing rewriting until the first transition.
	@param cfg The initial configuration to begin exploring the transition system at
	@exception KRunExecutionException Thrown if the backend fails to successfully initiate the
	debug session, or successfully perform any additional commands it chooses to execute. The
	implementation should not execute any commands which will throw this exception as a normal
	state of affairs.
	@exception UnsupportedOperationException The backend implementing this interface does not
	support debugging
	@return An object upon which debugger commands can be executed.
	*/
	public KRunDebugger debug(Term cfg) throws KRunExecutionException;

	/**
	Create a debugger session from an existing state space graph.
	@param searchResults the result of a search command, used to initialize the debugger with
	an already-executed graph traversal.
	@exception UnsupportedOperationException The backend implementing this interface does not
	support debugging
	@return An object upon which debugger commands can be executed. This object will contain
	the entire state space explored by the search command it follows.
	*/
	public KRunDebugger debug(SearchResults searchResults);
}
