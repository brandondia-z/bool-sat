package solver.sat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DPLL {

  public static Result solveSAT(SATInstance instance, Map<Integer, Boolean> assignments) {
    return new Result(dpll(instance, assignments), assignments);
  }

  private static boolean dpll(SATInstance instance, Map<Integer, Boolean> assignments) {
    if (!doUnitPropagation(instance, assignments)) {
      return false;
    }

    if (instance.clauses.isEmpty()) {
      return true; // All clauses are satisfied
    }
    if (hasEmptyClause(instance)) {
      return false; // An empty clause was found, so the branch is unsatisfiable
    }

    // choose an unassigned variable
    Integer variable = chooseVariable(instance, assignments);
    if (variable == null) {
      return false; // No variables left to assign, but not all clauses are satisfied
    }

    // try assigning true to the variable
    assignments.put(variable, true);
    List<Set<Integer>> oldClauses = new ArrayList<>(instance.clauses);
    removeClausesWithAssignedVariable(instance, assignments);
    removeOppositeLiterals(instance, assignments);
    if (dpll(instance, assignments)) {
      return true;
    }
    assignments.remove(variable);
    instance.clauses = oldClauses;

    // try assigning false to the variable
    assignments.put(variable, false);
    oldClauses = new ArrayList<>(instance.clauses);
    removeClausesWithAssignedVariable(instance, assignments);
    removeOppositeLiterals(instance, assignments);
    if (dpll(instance, assignments)) {
      return true;
    }
    assignments.remove(variable);
    instance.clauses = oldClauses;
    return false;
  }

  private static Integer chooseVariable(SATInstance instance, Map<Integer, Boolean> assignments) {
     // count the number of times a variable occurs in the clauses
    int maxOccurances = 0;
    Integer mostCommonVariable = null;
    Map<Integer, Integer> variableToNumOccurrences = new HashMap<>();
    for (Set<Integer> clause : instance.clauses) {
      for (Integer literal : clause) {
        int var = Math.abs(literal);
        if (!assignments.containsKey(var)) {
          int numOccurrences = variableToNumOccurrences.getOrDefault(var, 0) + 1;
          variableToNumOccurrences.put(var, numOccurrences);
          if (numOccurrences > maxOccurances) {
            maxOccurances = numOccurrences;
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

  // remove all clauses that include the recently assigned variable
  private static void removeClausesWithAssignedVariable(SATInstance instance, Map<Integer, Boolean> assignments) {
    // Create a temporary list to hold the clauses to be removed
    List<Set<Integer>> clausesToRemove = new ArrayList<>();

    // First, iterate through the clauses to identify which ones should be removed
    for (Set<Integer> clause : instance.clauses) {
      for (Integer literal : clause) {
        int var = Math.abs(literal);
        if (assignments.containsKey(var) && assignments.get(var) == (literal > 0)) {
          // Add the clause to the temporary list and break out of the inner loop
          clausesToRemove.add(clause);
          break; // Break out of the inner loop once a removal candidate is found
        }
      }
    }

    // Remove the identified clauses from the instance's clause collection
    instance.clauses.removeAll(clausesToRemove);
  }

  // remove all literals that are opposite of the recently assigned variable
    private static void removeOppositeLiterals(SATInstance instance, Map<Integer, Boolean> assignments) {
        // First, iterate through the clauses to identify which ones should be removed
      for (Set<Integer> clause : instance.clauses) {
        for (Integer literal : clause) {
          int var = Math.abs(literal);
          if (assignments.containsKey(var) && assignments.get(var) != (literal > 0)) {
          // Add the clause to the temporary list and break out of the inner loop
          clause.remove(literal);
          break; // Break out of the inner loop once a removal candidate is found
          }
        }
      }
    }

  // check for empty clauses
    private static boolean hasEmptyClause(SATInstance instance) {
      for (Set<Integer> clause : instance.clauses) {
        if (clause.isEmpty()) {
          return true;
        }
      }
      return false;
    }

  private DPLL(){}
}