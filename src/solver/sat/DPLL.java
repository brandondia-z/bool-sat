package solver.sat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DPLL {

  public static Result solveSAT(SATInstance instance, Map<Integer, Boolean> assignments) {
    // initialize assignments map with no assignments
    if (!doUnitPropagation(instance, assignments)) {
      return new Result(false, assignments);
    }
    return new Result(dpll(instance, assignments), assignments);
  }

  private static boolean dpll(SATInstance instance, Map<Integer, Boolean> assignments) {
    // base case: if all clauses are satisfied, return true
    if (allClausesSatisfied(instance, assignments)) {
      return true;
    }

    // base case: if some clause is not satisfied, return false
    if (someClauseUnsatisfiedWithCurrentAssignments(instance, assignments)) {
      return false;
    }

    if (!doUnitPropagation(instance, assignments)) {
      return false;
    }

    // choose an unassigned variable
    Integer variable = chooseVariable(instance, assignments);
    if (variable == null) {
      return false; // No variables left to assign, but not all clauses are satisfied
    }

    // try assigning true to the variable
    assignments.put(variable, true);
    if (dpll(instance, assignments)) {
      return true;
    }
    assignments.remove(variable);

    // try assigning false to the variable
    assignments.put(variable, false);
    if (dpll(instance, assignments)) {
      return true;
    }
    assignments.remove(variable);
    return false;
  }

  private static boolean allClausesSatisfied(SATInstance instance, Map<Integer, Boolean> assignments) {
    for (Set<Integer> clause : instance.clauses) {
      boolean clauseSatisfied = false;
      for (Integer literal : clause) {
        int var = Math.abs(literal);
        boolean val = literal > 0;
        if (assignments.containsKey(var) && assignments.get(var) == val) {
          clauseSatisfied = true;
          break;
        }
      }
      if (!clauseSatisfied) {
        return false;
      }
    }
    return true;
  }

  private static boolean someClauseUnsatisfiedWithCurrentAssignments(SATInstance instance, Map<Integer, Boolean> assignments) {
    for (Set<Integer> clause : instance.clauses) {
      boolean clauseUnsatisfied = true;
      for (Integer literal : clause) {
        int var = Math.abs(literal);
        boolean val = literal > 0;
        if (!assignments.containsKey(var) || assignments.get(var) == val) {
          clauseUnsatisfied = false;
          break;
        }
      }
      if (clauseUnsatisfied) {
        return true;
      }
    }
    return false;
  }

  private static Integer chooseVariable(SATInstance instance, Map<Integer, Boolean> assignments) {
     // count the number of times a variable occurs in the clauses
    int maxOccurances = 0;
    Integer mostCommonVariable = null;
    Map<Integer, Integer> variableToNumOccurances = new HashMap<>();
    for (Set<Integer> clause : instance.clauses) {
      for (Integer literal : clause) {
        int var = Math.abs(literal);
        if (!assignments.containsKey(var)) {
          int numOccurances = variableToNumOccurances.getOrDefault(var, 0) + 1;
          variableToNumOccurances.put(var, numOccurances);
          if (numOccurances > maxOccurances) {
            maxOccurances = numOccurances;
            mostCommonVariable = var;
          }
        }
      }
    }
    return mostCommonVariable;
  }

  public static boolean doUnitPropagation(SATInstance instance, Map<Integer, Boolean> assignments) {
    boolean progress;
    do {
      List<Set<Integer>> unitClauses = instance.clauses.stream()
          .filter(clause -> clause.size() == 1 && !assignments.containsKey(Math.abs(clause.iterator().next())))
          .toList();
      for (Set<Integer> clause : unitClauses) {
        Integer literal = clause.iterator().next();
        int var = Math.abs(literal);
        boolean val = literal > 0;
        if (assignments.containsKey(var)) {
          if (assignments.get(var) != val) {
            return false;
          }
        } else {
          assignments.put(var, val);
          instance.clauses.remove(clause);
        }
      }
      unitClauses = instance.clauses.stream()
          .filter(clause -> clause.size() == 1 && !assignments.containsKey(Math.abs(clause.iterator().next())))
          .toList();
      progress = !unitClauses.isEmpty();
    } while (progress);
    return true;
  }

  private DPLL(){}
}