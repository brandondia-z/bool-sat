package solver.sat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple class to represent a SAT instance.
 */
public class SATInstance {
  private int numVars;
  private int numClauses;
  private Set<Integer> vars = new HashSet<Integer>();
  private List<Clause> clauses = new ArrayList<>();
  private final Map<Integer, List<Clause>> variableToClauses = new HashMap<>();


  public SATInstance(int numVars, int numClauses) {
		this.numVars = numVars;
		this.numClauses = numClauses;
  }

  // Deep copy constructor
  public SATInstance(SATInstance other) {
    this.numVars = other.numVars;
    this.numClauses = other.numClauses;
    this.vars = new HashSet<>(other.vars);
    for (Clause clause : other.clauses) {
      addClause(new Clause(clause)); // Use Clause's deep copy constructor
    }
  }

  int getNumVars() {
    return numVars;
  }

  int getNumClauses() {
      return numClauses;
  }

  Set<Integer> getVars() {
      return vars;
  }

  List<Clause> getClauses() {
      return clauses;
  }

  public void addClause(Clause clause) {
    clauses.add(clause);
    for (Integer literal : clause.getLiterals()) {
      int variable = Math.abs(literal);
      variableToClauses.putIfAbsent(variable, new ArrayList<>());
      variableToClauses.get(variable).add(clause);
    }
  }

  public void removeClause(Clause clause) {
    clauses.remove(clause);
    for (Integer literal : clause.getLiterals()) {
      int variable = Math.abs(literal);
      removeFromVariableToClauses(variable, clause);
    }
  }

  public void removeFromVariableToClauses(int variable, Clause clause) {
    List<Clause> clausesForVariable = variableToClauses.get(variable);
    if (clausesForVariable != null) {
      clausesForVariable.remove(clause);
      if (clausesForVariable.isEmpty()) {
        variableToClauses.remove(variable);
      }
    }
  }


  public List<Clause> getClausesContaining(int variable) {
    return variableToClauses.getOrDefault(variable, new ArrayList<>());
  }

  void addVariable(Integer literal) {
    vars.add( (literal < 0) ? -1 * literal : literal);
  }

  
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Number of variables: " + numVars + "\n");
		buf.append("Number of clauses: " + numClauses + "\n");
    buf.append("Variables: " + vars.toString() + "\n");
    for(int c = 0; c < clauses.size(); c++)
			buf.append("Clause " + c + ": " + clauses.get(c).toString() + "\n");
    return buf.toString();
  }
}
